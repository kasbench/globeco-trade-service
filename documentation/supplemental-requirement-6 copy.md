# Supplemental Requirement 6

This requirement will support the user interface for trading.

The purpose of this requirement is to:
- Add paging, filtering, and sorting to GET api/v1/tradeOrders
    - Add offset and limit
    - Add filters and sorting on
        - id 
        - orderId
        - orderType
        - Portfolio Name (lookup from Portfolio Service)
        - Security Ticker (lookup from Security Ticker)
        - quantity
        - quantitySent
        - Blotter Abbreviation
        - Submitted
- Add paging, filtering, and sorting to GET api/v1/executions
    - Add offset and limit
    - Add filters and sorting on 
        - id 
        - Execution Status Abbreviation
        - Blotter Abbreviation
        - Trade Type Abbreviation
        - TradeOrderId
        - Destination Abbreviation
        - Quantity Ordered
        - Quantity Placed
        - Quantity Filled
- Paging, filtering, and sorting should be generally consistent with [api-paging-example.md](api-paging-example.md) 
- Add batch mode to POST api/v1/tradeOrders/{id}/submit 
    - POST api/v1/tradeOrders/batch/submit
    - List of Ids in request
    - Use [api-bulk-conversion-example.md](api-bulk-conversion-example.md) as a guide.  Make this conversion consistent in terms of general design and error handling.
- Modify response objects for these APIs to include both securityId and security ticker wherever securityId is returned.
- Modify response objects for these APIs to include both portfolioId and portfolio name where portfolioId is returned.

## Integrations

| Service | Host | Port | OpenAPI Spec |
| --- | --- | --- | --- |
| Security Service | globeco-security-service | 8000 | [globeco-security-service-openapi.yaml](globeco-security-service-openapi.yaml)
| Portfolio Service | globeco-portfolio-service | 8001 | [globeco-portfolio-service-openapi.yaml](globeco-portfolio-service-openapi.yaml)

---

- Do not show users security or portfolio ids.  Map them to security tickers and portfolio names, respectively.  Do this both on display and input.
- The APIs for this lookup are documented in [PORTFOLIO_SERVICE_SEARCH_REQUIREMENTS.md](PORTFOLIO_SERVICE_SEARCH_REQUIREMENTS.md) and [SECURITY_SERVICE_SEARCH_REQUIREMENTS-updated.md](SECURITY_SERVICE_SEARCH_REQUIREMENTS-updated.md).
- Securities and portfolios can be cached in memory for performance with a 5 minute TTL.  Use Caffeine for in memory caching.


