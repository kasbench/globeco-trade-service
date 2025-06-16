# Supplemental Requirement 6: Enhanced Trade Service UI Support

## Overview

This requirement enhances the GlobeCo Trade Service to support advanced user interface functionality for trading operations. The enhancement introduces comprehensive pagination, filtering, sorting, and batch operations while maintaining full backward compatibility with existing v1 APIs.

## Business Objectives

- **Enhanced User Experience**: Provide efficient data browsing with pagination, filtering, and sorting
- **Performance Optimization**: Reduce data transfer and improve response times for large datasets
- **Batch Operations**: Enable efficient bulk trade order submissions
- **User-Friendly Data**: Replace internal IDs with human-readable names (security tickers, portfolio names)
- **Backward Compatibility**: Ensure existing integrations continue to work without modification

## API Design Strategy

### Versioning Approach
- **v1 APIs**: Remain unchanged for backward compatibility
- **v2 APIs**: New endpoints with enhanced functionality for breaking changes
- **v1 Enhancements**: Add optional parameters to existing v1 endpoints where possible

### Data Enhancement Strategy
- **Security Integration**: Replace `securityId` with `security: {securityId, ticker}` objects
- **Portfolio Integration**: Replace `portfolioId` with `portfolio: {portfolioId, name}` objects
- **Caching Strategy**: Use Caffeine with 5-minute TTL for external service data
- **Graceful Degradation**: Fall back to IDs if external services are unavailable

## Enhanced API Specifications

### 1. Enhanced Trade Orders API

#### 1.1 GET /api/v2/tradeOrders (New Endpoint)

**Breaking Changes from v1:**
- Enhanced response structure with security and portfolio objects
- Pagination metadata in response wrapper
- Advanced filtering and sorting capabilities

**Query Parameters:**

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `limit` | integer | No | Maximum results (1-1000, default: 50) | `25` |
| `offset` | integer | No | Results to skip (default: 0) | `100` |
| `sort` | string | No | Comma-separated sort fields with optional `-` prefix for descending | `security.ticker,-quantity` |
| `id` | integer | No | Filter by trade order ID | `123` |
| `orderId` | integer | No | Filter by order ID | `456` |
| `orderType` | string | No | Filter by order type (comma-separated for OR) | `BUY,SELL` |
| `portfolio.name` | string | No | Filter by portfolio name (comma-separated for OR) | `Growth Fund,Tech Portfolio` |
| `security.ticker` | string | No | Filter by security ticker (comma-separated for OR) | `AAPL,MSFT` |
| `quantity.min` | number | No | Minimum quantity filter | `100.00` |
| `quantity.max` | number | No | Maximum quantity filter | `1000.00` |
| `quantitySent.min` | number | No | Minimum quantity sent filter | `50.00` |
| `quantitySent.max` | number | No | Maximum quantity sent filter | `500.00` |
| `blotter.abbreviation` | string | No | Filter by blotter abbreviation (comma-separated for OR) | `EQ,FI` |
| `submitted` | boolean | No | Filter by submission status | `true` |

**Sortable Fields:**
- `id`, `orderId`, `orderType`, `portfolio.name`, `security.ticker`, `quantity`, `quantitySent`, `blotter.abbreviation`, `submitted`, `tradeTimestamp`

**Response Structure:**
```json
{
  "tradeOrders": [
    {
      "id": 1,
      "orderId": 12345,
      "portfolio": {
        "portfolioId": "PORTFOLIO1",
        "name": "Growth Technology Fund"
      },
      "orderType": "BUY",
      "security": {
        "securityId": "SEC123",
        "ticker": "AAPL"
      },
      "quantity": 100.00,
      "quantitySent": 50.00,
      "limitPrice": 150.25,
      "tradeTimestamp": "2024-01-15T10:30:00Z",
      "blotter": {
        "id": 1,
        "abbreviation": "EQ",
        "name": "Equity",
        "version": 1
      },
      "submitted": false,
      "version": 1
    }
  ],
  "pagination": {
    "totalElements": 250,
    "totalPages": 10,
    "currentPage": 2,
    "pageSize": 25,
    "hasNext": true,
    "hasPrevious": true
  }
}
```

#### 1.2 GET /api/v1/tradeOrders (Enhanced)

**Backward Compatible Enhancements:**
- Add optional `limit` and `offset` parameters
- Maintain existing array response format
- Add `X-Total-Count` header for pagination metadata

**New Optional Parameters:**
- `limit` (integer, 1-1000, default: unlimited for backward compatibility)
- `offset` (integer, default: 0)

**Response:** Maintains existing array format with optional pagination

### 2. Enhanced Executions API

#### 2.1 GET /api/v2/executions (New Endpoint)

**Query Parameters:**

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `limit` | integer | No | Maximum results (1-1000, default: 50) | `25` |
| `offset` | integer | No | Results to skip (default: 0) | `100` |
| `sort` | string | No | Comma-separated sort fields with optional `-` prefix | `executionStatus.abbreviation,-quantityFilled` |
| `id` | integer | No | Filter by execution ID | `123` |
| `executionStatus.abbreviation` | string | No | Filter by execution status (comma-separated for OR) | `NEW,SENT,PART` |
| `blotter.abbreviation` | string | No | Filter by blotter abbreviation (comma-separated for OR) | `EQ,FI` |
| `tradeType.abbreviation` | string | No | Filter by trade type abbreviation (comma-separated for OR) | `BUY,SELL` |
| `tradeOrderId` | integer | No | Filter by trade order ID | `456` |
| `destination.abbreviation` | string | No | Filter by destination abbreviation (comma-separated for OR) | `NYSE,NASDAQ` |
| `quantityOrdered.min` | number | No | Minimum quantity ordered filter | `100.00` |
| `quantityOrdered.max` | number | No | Maximum quantity ordered filter | `1000.00` |
| `quantityPlaced.min` | number | No | Minimum quantity placed filter | `50.00` |
| `quantityPlaced.max` | number | No | Maximum quantity placed filter | `500.00` |
| `quantityFilled.min` | number | No | Minimum quantity filled filter | `25.00` |
| `quantityFilled.max` | number | No | Maximum quantity filled filter | `250.00` |

**Sortable Fields:**
- `id`, `executionStatus.abbreviation`, `blotter.abbreviation`, `tradeType.abbreviation`, `tradeOrderId`, `destination.abbreviation`, `quantityOrdered`, `quantityPlaced`, `quantityFilled`, `executionTimestamp`

**Response Structure:**
```json
{
  "executions": [
    {
      "id": 1,
      "executionTimestamp": "2024-01-15T10:30:00Z",
      "executionStatus": {
        "id": 1,
        "abbreviation": "PART",
        "description": "Partially Filled",
        "version": 1
      },
      "blotter": {
        "id": 1,
        "abbreviation": "EQ",
        "name": "Equity",
        "version": 1
      },
      "tradeType": {
        "id": 1,
        "abbreviation": "BUY",
        "description": "Buy Order",
        "version": 1
      },
      "tradeOrder": {
        "id": 1,
        "orderId": 12345,
        "portfolio": {
          "portfolioId": "PORTFOLIO1",
          "name": "Growth Technology Fund"
        },
        "security": {
          "securityId": "SEC123",
          "ticker": "AAPL"
        }
      },
      "destination": {
        "id": 1,
        "abbreviation": "NYSE",
        "description": "New York Stock Exchange",
        "version": 1
      },
      "quantityOrdered": 100.00,
      "quantityPlaced": 100.00,
      "quantityFilled": 50.00,
      "limitPrice": 150.25,
      "executionServiceId": 12345,
      "version": 1
    }
  ],
  "pagination": {
    "totalElements": 150,
    "totalPages": 6,
    "currentPage": 1,
    "pageSize": 25,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

#### 2.2 GET /api/v1/executions (Enhanced)

**Backward Compatible Enhancements:**
- Add optional `limit` and `offset` parameters
- Maintain existing array response format
- Add `X-Total-Count` header for pagination metadata

### 3. Batch Trade Order Submission API

#### 3.1 POST /api/v1/tradeOrders/batch/submit (New Endpoint)

**Request Body:**
```json
{
  "tradeOrderIds": [1, 2, 3, 4, 5],
  "submissions": [
    {
      "tradeOrderId": 1,
      "quantity": 50.00,
      "destinationId": 1
    },
    {
      "tradeOrderId": 2,
      "quantity": 75.00,
      "destinationId": 2
    }
  ]
}
```

**Response Structure:**
```json
{
  "status": "PARTIAL",
  "message": "4 of 5 trade orders submitted successfully",
  "totalRequested": 5,
  "successful": 4,
  "failed": 1,
  "results": [
    {
      "tradeOrderId": 1,
      "status": "SUCCESS",
      "message": "Trade order submitted successfully",
      "execution": {
        "id": 101,
        "quantityOrdered": 50.00,
        "destination": {
          "id": 1,
          "abbreviation": "NYSE"
        }
      },
      "requestIndex": 0
    },
    {
      "tradeOrderId": 5,
      "status": "FAILURE",
      "message": "Trade order not found or not in valid state",
      "requestIndex": 4
    }
  ]
}
```

**HTTP Status Codes:**
- **200 OK**: All submissions successful
- **207 Multi-Status**: Partial success
- **400 Bad Request**: Invalid request format or batch size exceeded
- **413 Payload Too Large**: Batch size exceeds 100 items

## External Service Integration

### Security Service Integration

**Service Details:**
- **Host**: `globeco-security-service`
- **Port**: `8000`
- **Endpoint**: `GET /api/v2/securities?ticker={ticker}`

**Integration Pattern:**
```java
// Resolve ticker to security details
SecurityDTO resolveSecurityByTicker(String ticker) {
    // Check cache first
    SecurityDTO cached = securityCache.get(ticker);
    if (cached != null) return cached;
    
    // Call external service
    String url = securityServiceBaseUrl + "/api/v2/securities?ticker=" + ticker;
    SecurityResponse response = restTemplate.getForObject(url, SecurityResponse.class);
    
    if (response != null && !response.getSecurities().isEmpty()) {
        SecurityDTO security = response.getSecurities().get(0);
        securityCache.put(ticker, security); // 5-minute TTL
        return security;
    }
    
    // Fallback to ID-only object
    return new SecurityDTO(ticker, ticker);
}
```

### Portfolio Service Integration

**Service Details:**
- **Host**: `globeco-portfolio-service`
- **Port**: `8000` (mapped to 8001 in Docker)
- **Endpoint**: `GET /api/v1/portfolios?name={name}`

**Integration Pattern:**
```java
// Resolve portfolio name to portfolio details
PortfolioDTO resolvePortfolioByName(String name) {
    // Check cache first
    PortfolioDTO cached = portfolioCache.get(name);
    if (cached != null) return cached;
    
    // Call external service
    String url = portfolioServiceBaseUrl + "/api/v1/portfolios?name=" + name;
    PortfolioResponse response = restTemplate.getForObject(url, PortfolioResponse.class);
    
    if (response != null && !response.getPortfolios().isEmpty()) {
        PortfolioDTO portfolio = response.getPortfolios().get(0);
        portfolioCache.put(name, portfolio); // 5-minute TTL
        return portfolio;
    }
    
    // Fallback to ID-only object
    return new PortfolioDTO(name, name);
}
```

## Performance Considerations

### Caching Strategy
- **Technology**: Caffeine Cache
- **TTL**: 5 minutes for external service data
- **Cache Size**: 1000 entries per service (configurable)
- **Eviction Policy**: LRU (Least Recently Used)
- **Metrics**: Cache hit/miss rates via Micrometer

### Database Optimization
- **Indexing**: Add composite indexes for common filter combinations
- **Query Optimization**: Use JPA Specifications for dynamic filtering
- **Connection Pooling**: Optimize HikariCP settings for concurrent requests
- **Pagination**: Use database-level LIMIT/OFFSET for efficient paging

### API Performance
- **Response Compression**: Enable GZIP compression for large responses
- **Parallel Processing**: Use CompletableFuture for external service calls
- **Circuit Breaker**: Implement Hystrix/Resilience4j for external service failures
- **Rate Limiting**: Implement request rate limiting to prevent abuse

## Error Handling

### Validation Errors (HTTP 400)
```json
{
  "error": "Bad Request",
  "message": "Invalid query parameters",
  "details": {
    "limit": "Must be between 1 and 1000",
    "sort": "Invalid sort field: invalidField"
  }
}
```

### External Service Failures
- **Graceful Degradation**: Return ID-only objects when external services fail
- **Timeout Handling**: 5-second timeout for external service calls
- **Retry Logic**: Exponential backoff for transient failures
- **Circuit Breaker**: Open circuit after 5 consecutive failures

## Security Considerations

### Input Validation
- **SQL Injection Prevention**: Use parameterized queries and JPA Specifications
- **XSS Prevention**: Sanitize all string inputs
- **Parameter Validation**: Strict validation of all query parameters
- **Rate Limiting**: Prevent abuse of filtering and sorting endpoints

### Data Access Control
- **Authentication**: Maintain existing authentication requirements
- **Authorization**: Ensure users can only access authorized data
- **Audit Logging**: Log all filtering and sorting operations
- **Data Masking**: Mask sensitive data in logs

## Backward Compatibility

### v1 API Guarantees
- **Response Format**: Existing v1 endpoints maintain exact response structure
- **Parameter Handling**: New optional parameters don't affect existing behavior
- **Error Codes**: Maintain existing HTTP status code patterns
- **Performance**: No degradation in v1 endpoint performance

### Migration Strategy
- **Phased Rollout**: Deploy v2 endpoints alongside v1
- **Client Migration**: Provide migration guide for v1 to v2 transition
- **Deprecation Timeline**: v1 endpoints remain supported for 12 months minimum
- **Feature Parity**: Ensure v2 provides all v1 functionality plus enhancements

---

## Execution Plan

### Phase 1: Foundation and External Service Integration ✅ COMPLETE
- [x] **1.1** External Service Client Implementation
  - [x] Create `SecurityServiceClient` with v2 API integration
  - [x] Create `PortfolioServiceClient` with search capabilities
  - [x] Implement timeout and retry logic for external calls
  - [x] Add comprehensive error handling and logging
- [x] **1.2** Caching Infrastructure
  - [x] Add Caffeine dependency to build.gradle
  - [x] Configure Caffeine cache manager with 5-minute TTL
  - [x] Create `SecurityCacheService` with metrics
  - [x] Create `PortfolioCacheService` with metrics
  - [x] Add cache configuration properties
- [x] **1.3** Enhanced DTO Structure
  - [x] Create `SecurityDTO` with `securityId` and `ticker` fields
  - [x] Create `PortfolioDTO` with `portfolioId` and `name` fields
  - [x] Create pagination wrapper DTOs (`TradeOrderPageResponseDTO`, `ExecutionPageResponseDTO`)
  - [x] Create batch operation DTOs (`BatchSubmitRequestDTO`, `BatchSubmitResponseDTO`)

### Phase 2: Database and Repository Enhancements ✅ COMPLETE
- [x] **2.1** Database Optimization
  - [x] Add composite indexes for common filter combinations
  - [x] Analyze and optimize existing queries
  - [x] Add database performance monitoring
- [x] **2.2** Repository Layer Enhancements
  - [x] Create `TradeOrderSpecification` for dynamic filtering
  - [x] Create `ExecutionSpecification` for dynamic filtering
  - [x] Implement sorting utilities with field validation
  - [x] Add pagination support to repository methods
- [x] **2.3** Service Layer Integration
  - [x] Update `TradeOrderService` with external service integration
  - [x] Update `ExecutionService` with external service integration
  - [x] Implement batch submission logic in `TradeOrderService`
  - [x] Add comprehensive error handling and fallback logic

### Phase 3: v2 API Implementation ✅ COMPLETE
- [x] **3.1** TradeOrder v2 Controller
  - [x] Create `TradeOrderV2Controller` with enhanced endpoints
  - [x] Implement pagination, filtering, and sorting
  - [x] Add comprehensive parameter validation
  - [x] Implement response transformation with external data
- [x] **3.2** Execution v2 Controller
  - [x] Create `ExecutionV2Controller` with enhanced endpoints
  - [x] Implement pagination, filtering, and sorting
  - [x] Add comprehensive parameter validation
  - [x] Implement response transformation with external data
- [x] **3.3** Batch Operations Controller
  - [x] Implement `POST /api/v1/tradeOrders/batch/submit` endpoint
  - [x] Add batch size validation (max 100)
  - [x] Implement parallel processing for batch submissions
  - [x] Add comprehensive error handling and status reporting

### Phase 4: v1 API Backward Compatible Enhancements ✅ COMPLETE
- [x] **4.1** TradeOrder v1 Enhancements
  - [x] Add optional `limit` and `offset` parameters to existing endpoint
  - [x] Maintain existing response format
  - [x] Add `X-Total-Count` header for pagination metadata
  - [x] Ensure no breaking changes to existing behavior
- [x] **4.2** Execution v1 Enhancements
  - [x] Add optional `limit` and `offset` parameters to existing endpoint
  - [x] Maintain existing response format
  - [x] Add `X-Total-Count` header for pagination metadata
  - [x] Ensure no breaking changes to existing behavior

### Phase 5: Testing and Validation
- [ ] **5.1** Unit Testing
  - [ ] Test external service client implementations
  - [ ] Test caching behavior and TTL functionality
  - [ ] Test filtering and sorting specifications
  - [ ] Test batch operation logic and error handling
- [ ] **5.2** Integration Testing
  - [ ] Test v2 API endpoints end-to-end
  - [ ] Test external service integration with mock services
  - [ ] Test pagination, filtering, and sorting combinations
  - [ ] Test batch submission scenarios (success, partial, failure)
- [ ] **5.3** Backward Compatibility Testing
  - [ ] Verify v1 endpoints maintain exact existing behavior
  - [ ] Test v1 enhancements don't break existing clients
  - [ ] Validate response format consistency
  - [ ] Performance regression testing
- [ ] **5.4** Performance Testing
  - [ ] Load testing for pagination and filtering
  - [ ] Cache performance and hit rate validation
  - [ ] External service timeout and circuit breaker testing
  - [ ] Batch operation performance testing

### Phase 6: Documentation and Configuration
- [ ] **6.1** API Documentation
  - [ ] Update OpenAPI specification with v2 endpoints
  - [ ] Document all query parameters and response formats
  - [ ] Add comprehensive examples for complex queries
  - [ ] Create migration guide from v1 to v2
- [ ] **6.2** Configuration and Monitoring
  - [ ] Add external service configuration properties
  - [ ] Configure cache metrics and monitoring
  - [ ] Add API performance metrics
  - [ ] Configure alerting for external service failures
- [ ] **6.3** Deployment Documentation
  - [ ] Update README with new features
  - [ ] Document configuration requirements
  - [ ] Add troubleshooting guide
  - [ ] Create operational runbook

### Phase 7: Production Deployment and Monitoring
- [ ] **7.1** Staging Deployment
  - [ ] Deploy to staging environment
  - [ ] Run comprehensive smoke tests
  - [ ] Validate external service integration
  - [ ] Performance testing in staging
- [ ] **7.2** Production Rollout
  - [ ] Deploy with feature flags for gradual rollout
  - [ ] Monitor API performance and error rates
  - [ ] Monitor cache hit rates and external service calls
  - [ ] Validate backward compatibility in production
- [ ] **7.3** Post-Deployment Validation
  - [ ] Monitor system performance and stability
  - [ ] Collect user feedback on new features
  - [ ] Optimize based on usage patterns
  - [ ] Plan for v1 deprecation timeline

### Phase 8: Optimization and Future Enhancements
- [ ] **8.1** Performance Optimization
  - [ ] Optimize database queries based on usage patterns
  - [ ] Fine-tune cache settings and TTL values
  - [ ] Implement additional performance improvements
  - [ ] Add advanced monitoring and alerting
- [ ] **8.2** Feature Enhancements
  - [ ] Add advanced filtering capabilities based on user feedback
  - [ ] Implement saved search functionality
  - [ ] Add export capabilities for filtered data
  - [ ] Plan for additional batch operations

---

## Success Criteria

### Functional Requirements
- [ ] All v2 endpoints provide enhanced functionality with pagination, filtering, and sorting
- [ ] Batch submission supports up to 100 trade orders with proper error handling
- [ ] External service integration provides human-readable names for securities and portfolios
- [ ] All v1 endpoints maintain exact backward compatibility

### Performance Requirements
- [ ] v2 endpoints respond within 500ms for typical queries
- [ ] Cache hit rate exceeds 80% for external service data
- [ ] Batch submissions process within 2 seconds for 100 items
- [ ] No performance degradation in v1 endpoints

### Quality Requirements
- [ ] 95% test coverage for new functionality
- [ ] Zero breaking changes to existing v1 APIs
- [ ] Comprehensive error handling and graceful degradation
- [ ] Production monitoring and alerting in place

### Documentation Requirements
- [ ] Complete OpenAPI specification for all endpoints
- [ ] Migration guide for v1 to v2 transition
- [ ] Operational documentation and troubleshooting guide
- [ ] Performance tuning and configuration guide


