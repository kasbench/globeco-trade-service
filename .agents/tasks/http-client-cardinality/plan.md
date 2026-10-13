# Implementation Plan: Fix high-cardinality `http.client.requests` at the source

## Problem

Micrometer logs: "Reached the maximum number of URI tags for 'http.client.requests'. Are you
using 'uriVariables'?" This happens because outbound `RestTemplate` calls bake dynamic values
(ids, tickers, portfolio names) directly into the URL string handed to `RestTemplate`. Micrometer
derives the `uri` tag from that string, so every distinct value becomes a new tag value and blows
past the per-meter URI tag cap.

The fix: pass a URI **template** containing `{placeholder}` markers to `RestTemplate` and supply the
real values as the trailing `Object... uriVariables` (or a `Map`). `RestTemplate` then performs the
expansion, Micrometer records the templated path, and cardinality is bounded by the number of
endpoints rather than the number of distinct values.

## Design decisions (made here, grounded in the code)

- **Pass template String + uriVariables directly to `RestTemplate`** rather than keeping
  `UriComponentsBuilder` + `.toUriString()`. Reason: the two search methods in each client already
  call `UriComponentsBuilder...toUriString()`, which pre-expands the value into the string. That is
  precisely the pre-expansion trap: `RestTemplate` would see an already-expanded URI and cardinality
  would NOT drop. Passing a template plus `uriVariables` is the smallest change that both fixes the
  metric and keeps URL-encoding correct (RestTemplate encodes the variable values). `UriComponentsBuilder`
  is removed from the two search methods and the unused import dropped.
- **Keep the `logger.debug("Calling ... Service: {}", url)` lines**. They may continue to log a
  human-readable expanded URL for debugging; only the value PASSED TO `RestTemplate` must be the
  template. To keep behavior identical and avoid building two strings, each site logs the template
  string (the task explicitly allows this: the debug line is not what Micrometer reads).
- **Query parameters**: keep the parameter NAME static in the template and move the VALUE into a
  placeholder, e.g. `"/api/v2/securities?ticker={ticker}"` with the value passed as a uriVariable.
- **Static-URL callers are left unchanged**: `SecurityServiceImpl.getSecurityTypes()` /
  `getSecurities()` and `ExecutionServiceClient` / `ExecutionServiceImpl` batch POSTs have no dynamic
  value in the URL, so they already produce one low-cardinality `uri` tag each. See item 5.

## Build & test commands (discovered)

- Build: `./gradlew compileJava`
- Targeted tests: `./gradlew test --tests "<FQCN>"`
- The project uses JUnit 5 + Mockito (`spring-boot-starter-test`). Toolchain: Java 25, Spring Boot 3.4.5.

## Complete inventory of outbound HTTP caller sites (from grep of `src/main/java`)

| # | File | Method | Dynamic value in URL? | Action |
|---|------|--------|----------------------|--------|
| 1 | `client/SecurityServiceClient.java` | `findSecurityByTicker` | yes (`ticker` query) | fix |
| 1 | `client/SecurityServiceClient.java` | `findSecurityById` | yes (path id) | fix |
| 1 | `client/SecurityServiceClient.java` | `findSecuritiesByTickerLike` | yes (`ticker_like` query) | fix |
| 2 | `client/PortfolioServiceClient.java` | `findPortfolioByName` | yes (`name` query, via UriComponentsBuilder) | fix |
| 2 | `client/PortfolioServiceClient.java` | `findPortfolioById` | yes (path id) | fix |
| 2 | `client/PortfolioServiceClient.java` | `findPortfoliosByNameLike` | yes (`name_like` query, via UriComponentsBuilder) | fix |
| 5 | `client/ExecutionServiceClient.java` | `executeSubmitBatch` (`postForEntity`) | NO — `/api/v1/executions/batch` is static | no change |
| 5 | `security/SecurityServiceImpl.java` | `getSecurityTypes`, `getSecurities` (`getForObject`) | NO — static paths | no change |
| 5 | `service/ExecutionServiceImpl.java` | holds a `RestTemplate` field but makes no outbound call (routes through `BulkExecutionSubmissionService`) | n/a | no change |

No `WebClient`/`RestClient` usage exists in `src/main/java` (grep returned none for outbound HTTP beyond the above).

---

## Plan items

- [ ] 1. Rewrite the three dynamic calls in `SecurityServiceClient` to use URI templates + uriVariables.
      - `findSecurityByTicker`: change
        `String url = securityServiceBaseUrl + "/api/v2/securities?ticker=" + ticker.trim();`
        then `restTemplate.getForEntity(url, SecuritySearchResponse.class)`
        **to**
        `String url = securityServiceBaseUrl + "/api/v2/securities?ticker={ticker}";`
        and `restTemplate.getForEntity(url, SecuritySearchResponse.class, ticker.trim())`.
      - `findSecurityById`: change
        `String url = securityServiceBaseUrl + "/api/v1/security/" + securityId.trim();`
        then `restTemplate.getForEntity(url, SecurityDTO.class)`
        **to**
        `String url = securityServiceBaseUrl + "/api/v1/security/{securityId}";`
        and `restTemplate.getForEntity(url, SecurityDTO.class, securityId.trim())`.
      - `findSecuritiesByTickerLike`: change
        `String url = securityServiceBaseUrl + "/api/v2/securities?ticker_like=" + tickerPattern.trim();`
        then `restTemplate.getForEntity(url, SecuritySearchResponse.class)`
        **to**
        `String url = securityServiceBaseUrl + "/api/v2/securities?ticker_like={tickerLike}";`
        and `restTemplate.getForEntity(url, SecuritySearchResponse.class, tickerPattern.trim())`.
      Keep every `logger.debug`, status check, and catch block exactly as-is (the debug line logs the
      template string now, which is fine).
      Files: `src/main/java/org/kasbench/globeco_trade_service/client/SecurityServiceClient.java`
      Verify: `./gradlew compileJava` succeeds. (Tests covered in item 3.)

- [ ] 2. Rewrite the three dynamic calls in `PortfolioServiceClient`; remove the `UriComponentsBuilder` pre-expansion.
      - `findPortfolioByName`: replace the `UriComponentsBuilder.fromUriString(...).queryParam("name", name.trim()).toUriString()`
        block (the **pre-expansion trap** — it bakes the value into the string) with
        `String url = portfolioServiceBaseUrl + "/api/v2/portfolios?name={name}";`
        and `restTemplate.getForEntity(url, PortfolioSearchResponse.class, name.trim())`.
      - `findPortfoliosByNameLike`: replace the analogous `UriComponentsBuilder...queryParam("name_like", namePattern.trim()).toUriString()`
        with
        `String url = portfolioServiceBaseUrl + "/api/v2/portfolios?name_like={nameLike}";`
        and `restTemplate.getForEntity(url, PortfolioSearchResponse.class, namePattern.trim())`.
      - `findPortfolioById`: change
        `String url = portfolioServiceBaseUrl + "/api/v1/portfolio/" + portfolioId.trim();`
        then `restTemplate.getForEntity(url, PortfolioDTO.class)`
        **to**
        `String url = portfolioServiceBaseUrl + "/api/v1/portfolio/{portfolioId}";`
        and `restTemplate.getForEntity(url, PortfolioDTO.class, portfolioId.trim())`.
      Remove the now-unused `import org.springframework.web.util.UriComponentsBuilder;`. Keep all
      logging, status checks, and catch blocks identical.
      Files: `src/main/java/org/kasbench/globeco_trade_service/client/PortfolioServiceClient.java`
      Verify: `./gradlew compileJava` succeeds. (Tests covered in item 3.)

- [ ] 3. Update the existing client unit tests so their stubs and verifications match the new
      `getForEntity(String, Class, Object...)` signatures. This is required because the current tests
      stub the 2-arg overload (`getForEntity(anyString(), eq(X.class))`) and assert on the expanded URL
      via `contains(...)`; after the fix the code calls the 3-arg varargs overload and the URL string is
      a template, so the old stubs/matchers no longer match. Do NOT add new test cases — only adjust the
      existing ones to the new call shape, preserving each test's intent.
      - In `SecurityServiceClientTest`:
        * Stubs: `when(restTemplate.getForEntity(anyString(), eq(...class), any(Object[].class)))` (add the
          varargs matcher) for the search/by-ticker/by-id tests.
        * `testFindSecurityByTicker_Success`: change the verify from `contains("ticker=AAPL")` to assert
          the template + value, e.g. `verify(restTemplate).getForEntity(contains("ticker={ticker}"), eq(...class), eq("AAPL"))`.
        * `testFindSecurityById_Success`: change `contains("/api/v1/security/684F...")` to
          `verify(restTemplate).getForEntity(contains("/api/v1/security/{securityId}"), eq(SecurityDTO.class), eq("684F122BEEA39200E562918C"))`.
      - In `PortfolioServiceClientTest`:
        * Add the varargs matcher to the search/by-id stubs as above.
        * `testFindPortfolioByName_Success`: change `contains("name=Growth%20Fund")` to
          `verify(restTemplate).getForEntity(contains("name={name}"), eq(...class), eq("Growth Fund"))`.
        * `testNameWithSpaces_UrlEncoding`: this test asserted that the builder URL-encoded spaces to
          `%20`. Encoding now happens inside RestTemplate's expansion, not in the string the client builds,
          so change the assertion to verify the template + raw value is passed:
          `verify(restTemplate).getForEntity(contains("name={name}"), eq(...class), eq("Growth Technology Fund"))`.
          (The method name may stay; the behavior under test — correct handling of names with spaces — is
          preserved, now delegated to RestTemplate's encoding.)
        * `testFindPortfolioById_Success`: change `contains("/api/v1/portfolio/684f...")` to
          `contains("/api/v1/portfolio/{portfolioId}")` plus `eq("684f1250c19ad4fb89bbfed7")`.
      - Note on Mockito varargs: when stubbing/verifying the varargs overload use `any(Object[].class)`
        for "any variables" and `eq(value)` for a specific single value; verify stubs still compile against
        `getForEntity(String, Class<T>, Object...)`.
      Files:
      `src/test/java/org/kasbench/globeco_trade_service/service/SecurityServiceClientTest.java`,
      `src/test/java/org/kasbench/globeco_trade_service/service/PortfolioServiceClientTest.java`
      Verify:
      `./gradlew test --tests "org.kasbench.globeco_trade_service.service.SecurityServiceClientTest" --tests "org.kasbench.globeco_trade_service.service.PortfolioServiceClientTest"`
      — all tests pass.

- [ ] 4. Run the full test suite to confirm nothing else regressed (the clients are used by cache
      services and enrichment services that may have integration-style tests exercising these paths).
      Files: none (verification only).
      Verify: `./gradlew test` — build succeeds and all tests pass. If any other test stubbed these
      client calls with the 2-arg `getForEntity` overload, update that stub to the varargs overload the
      same way as item 3 (behavior-identical change only; no new tests).

- [ ] 5. Confirm the no-change caller sites and the constraint, and record the rationale. No edits —
      this item is a verification/sign-off step.
      - `ExecutionServiceClient.executeSubmitBatch` posts to the static `/api/v1/executions/batch`; one
        low-cardinality `uri` tag, no dynamic value — leave as-is.
      - `security/SecurityServiceImpl.getSecurityTypes()` and `getSecurities()` call static paths
        (`/api/v1/securityTypes`, `/api/v1/securities`) — leave as-is.
      - `service/ExecutionServiceImpl` injects a `RestTemplate` but makes no outbound call itself (it
        delegates to `BulkExecutionSubmissionService`); nothing to change there.
      - Constraint check: do NOT add or change any Micrometer `max-uri-tags` / `uri` tag cap setting.
        Confirm `src/main/resources/application.properties` contains no `max-uri-tags` key (grep already
        showed none) and that none is added by this task.
      Files: none.
      Verify: `grep -rn "max-uri-tags" src/main/resources` returns nothing, and
      `./gradlew compileJava` still succeeds. (This grep is a constraint check, not feature verification;
      feature verification is items 3-4.)

## Expected outcome

After items 1-4, each outbound client method hands `RestTemplate` a URL that contains `{...}`
placeholders with the real values passed as `uriVariables`. Micrometer records templated `uri` tags
(`/api/v2/securities?ticker={ticker}`, `/api/v1/security/{securityId}`,
`/api/v2/portfolios?name={name}`, `/api/v1/portfolio/{portfolioId}`, etc.), so the number of distinct
`uri` tag values for `http.client.requests` is bounded by the number of endpoints, and the
"maximum number of URI tags" warning stops. Behavior (endpoints, query params, values, headers, error
handling, logging) is otherwise unchanged.
