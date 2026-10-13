# URI templates for outbound RestTemplate calls to bound http.client.requests cardinality

The Security and Portfolio service clients baked dynamic values (id, ticker, name, name_like) straight into the URL string handed to `RestTemplate`, so Micrometer's `http.client.requests` meter tagged every distinct value as its own `uri` and hit the per-meter `max-uri-tags` cap. The fix rewrites all six dynamic GET calls to pass a `{placeholder}` template string plus the real value as a trailing `uriVariable`, letting `RestTemplate` do the expansion so Micrometer records the templated path. Two of the Portfolio search methods previously used `UriComponentsBuilder.toUriString()` (the pre-expansion trap) and were converted to template strings with the now-unused import removed. The existing client unit tests were updated to the 3-arg varargs `getForEntity` overload; no new tests, and the `max-uri-tags` cap was not raised.

Watch for: nothing blocking. One behavioral nuance worth noting — query values are now URL-encoded by `RestTemplate` template expansion rather than by `UriComponentsBuilder`/manual concatenation (confirmed), which is the correct and intended outcome.

**Verdict**: APPROVED

## High-level view

Every outbound call that carried a dynamic path or query value now follows the same shape: a static template string (`.../{id}` or `?name={name}`) as the first argument and the dynamic value as the trailing varargs argument. There is no remaining concatenation of dynamic values into a URL and no `UriComponentsBuilder` pre-expansion, so `RestTemplate` is the component that expands the template and the Micrometer tag collapses to one `uri` per endpoint.

The task named four files; only two needed changes. `SecurityServiceClient` and `PortfolioServiceClient` held all six dynamic calls and were converted. `ExecutionServiceClient`'s single outbound call is a POST to the static path `/api/v1/executions/batch` with no dynamic URL component, and `ExecutionServiceImpl` injects a `RestTemplate` but makes no outbound call itself — both correctly left untouched. A sweep of `src/main/java` found no other outbound caller with dynamic values: the only other `RestTemplate` user, `security/SecurityServiceImpl`, calls two static paths.

Behavior is preserved across endpoints, query parameter names, header handling, error/retry branches, and logging. The one observable difference is that encoding of query values moves into `RestTemplate` expansion, which is the point of the change. Build evidence is present and credible: `compileJava` succeeds, the two updated client test classes pass in isolation (27 tests), and the documented full-suite compile failure traces to a pre-existing DTO mismatch in `ExecutionServiceClientTest` that this commit does not touch.

<details>
<summary>Issues (0)</summary>

No blocking or non-blocking action items. All concerns investigated resolved to confirmed-correct.

</details>

<details>
<summary>Details</summary>

### The six converted calls all pass template + variable

Each of the three `SecurityServiceClient` methods and three `PortfolioServiceClient` methods now builds a URL of the form `baseUrl + "/path/{var}"` or `baseUrl + "/path?param={var}"` and passes the trimmed dynamic value as the trailing `Object...` argument to `getForEntity` (confirmed by reading both files in full):

```
/api/v2/securities?ticker={ticker}        , ticker.trim()
/api/v1/security/{securityId}             , securityId.trim()
/api/v2/securities?ticker_like={tickerLike}, tickerPattern.trim()
/api/v2/portfolios?name={name}            , name.trim()
/api/v1/portfolio/{portfolioId}           , portfolioId.trim()
/api/v2/portfolios?name_like={nameLike}   , namePattern.trim()
```

No call string concatenates a dynamic value, and the two Portfolio search methods that previously ended in `UriComponentsBuilder...toUriString()` — which would hand a fully-expanded URI to `RestTemplate` and defeat the fix — are gone, with the `UriComponentsBuilder` import removed. Query parameters keep static names in the template (`ticker`, `ticker_like`, `name`, `name_like`) with only the value templated. This is exactly the shape that keeps the Micrometer `uri` tag low-cardinality.

### Encoding moves into RestTemplate expansion

The old Security search methods concatenated the raw trimmed value directly (`?ticker=" + ticker.trim()`), and the Portfolio search methods relied on `UriComponentsBuilder` to encode. Both paths now defer to `RestTemplate`'s template expansion, which URL-encodes variable values. For the id/ticker lookups the practical result is identical; for the name searches the encoding is now performed by the expansion step rather than the builder. Same endpoint, same parameter, same value reaching the remote service — the only shift is which component does the encoding, which is intended.

### Files named but not changed are correct to leave alone

`ExecutionServiceClient.executeSubmitBatch` posts to `executionServiceBaseUrl + "/api/v1/executions/batch"`, a fully static path with the dynamic data living in the request body, not the URL — no `uri`-tag cardinality to fix (confirmed against both the working tree and the baseline). `ExecutionServiceImpl` holds `restTemplate` and `executionServiceBaseUrl` fields but contains no call site that uses them for an outbound request; it delegates batch submission elsewhere. Neither file had anything to convert, so their absence from the diff is correct rather than a miss.

A full `src/main/java` sweep for `RestClient`/`WebClient`/`Feign`/`RestTemplate` call sites turned up only one other outbound user, `security/SecurityServiceImpl`, whose two calls target static paths (`/api/v1/securityTypes`, `/api/v1/securities`) with no dynamic component. Nothing else was missed.

### max-uri-tags cap untouched

`max-uri-tags` appears only in the task plan as a constraint note; no occurrence exists in `application.properties` or any config/source file (confirmed). The fix addresses cardinality at the source rather than by raising the cap, which is what the original request asked for.

### Tests updated to the varargs overload, no new tests

Both client test classes switch their stubs and verifications from the 2-arg `getForEntity(String, Class)` to the 3-arg `getForEntity(String, Class, Object...)` overload, adding `any(Object[].class)` to the `when(...)` stubs and asserting on the templated URL (`contains("name={name}")`, `contains("/api/v1/portfolio/{portfolioId}")`, etc.) plus the expected variable (`eq("Growth Fund")`). This keeps coverage aligned with the new call signature without adding gratuitous tests, and the URL assertions now verify the template rather than a pre-expanded string — which is the behavior that matters for the cap.

Not tested: there is no assertion that Micrometer actually records a single templated `uri` tag (the metrics-level behavior). That is reasonable to omit here — it would require wiring an observed `RestTemplate` and inspecting the meter registry, which is heavier than the fix warrants, and the templated-URL assertions are a sound proxy for the call shape that drives the tag.

### Build and test evidence

The commit message records that `./gradlew compileJava` succeeds and that the two updated client test classes were compiled and run in isolation with all 27 passing. It also documents two problems that block the full suite — an `ExecutionServiceClientTest`/`BulkExecutionSubmissionServiceTest` reference to `BatchExecutionRequestDTO` where `ExecutionServiceBatchRequestDTO` is expected, and a Java 25 / Byte Buddy inline-mock-maker incompatibility — and claims both predate this change. The DTO claim checks out: `BatchExecutionRequestDTO` is referenced throughout `ExecutionServiceClientTest` on `origin/main`, and this commit does not touch that file. The evidence is sufficient; re-running the full suite would add no information.

</details>

<details>
<summary>File map</summary>

- `client/SecurityServiceClient.java` — three GET calls converted to template + uriVariable (`ticker`, `securityId`, `ticker_like`).
- `client/PortfolioServiceClient.java` — three GET calls converted; two `UriComponentsBuilder` pre-expansions replaced with templates; unused import removed.
- `test/.../service/SecurityServiceClientTest.java` — stubs/verifications moved to 3-arg varargs overload and templated-URL assertions.
- `test/.../service/PortfolioServiceClientTest.java` — same test adjustment.

Full diff: `git show 65c1af5`.

</details>
