# Globeco Trade Service API Guide for Claude

## Document Purpose
This guide provides comprehensive information about the Globeco Trade Service API for Large Language Model (LLM) consumption. It includes complete API specifications, implementation patterns, and contextual information to enable effective interaction with the service.

## Service Overview

### Core Purpose
The Globeco Trade Service is a Spring Boot microservice that manages trade orders and executions for a financial trading platform. It provides both v1 (legacy) and v2 (enhanced) REST APIs with backward compatibility guarantees.

### Key Capabilities
- Trade order management (CRUD operations)
- Execution tracking and reporting
- Advanced filtering, sorting, and pagination (v2)
- External service integration for data enrichment (v2)
- Batch operations for high-volume processing (v2)
- High-performance caching with Caffeine
- Comprehensive monitoring and observability

### Architecture Context
```
Client Applications → Load Balancer → Trade Service → PostgreSQL Database
                                         ↓
                               External Services (Security, Portfolio)
                                         ↓
                                  Caffeine Cache Layer
```

## API Versions

### v1 API (Legacy with Enhancements)
- **Base Path**: `/api/v1`
- **Status**: Maintained for backward compatibility
- **Enhancements**: Optional pagination parameters added
- **Response Format**: Simple arrays
- **Use Case**: Existing integrations, simple queries

### v2 API (Enhanced)
- **Base Path**: `/api/v2`
- **Status**: Current version with full feature set
- **Response Format**: Paginated objects with metadata
- **Use Case**: New integrations, complex queries, high-performance applications

## Data Models

### Core Entities

#### TradeOrder Entity
```java
// Primary entity representing a trade order
{
  "id": Integer,                    // Primary key
  "orderId": Integer,               // Business order identifier
  "orderType": String,              // BUY, SELL, SHORT
  "quantity": BigDecimal,           // Total order quantity
  "quantitySent": BigDecimal,       // Quantity submitted for execution
  "portfolioId": String,            // Portfolio identifier
  "securityId": String,             // Security identifier
  "blotterId": String,              // Blotter reference
  "limitPrice": BigDecimal,         // Price limit (optional)
  "tradeTimestamp": OffsetDateTime, // Order timestamp
  "submitted": Boolean,             // True when fully submitted
  "version": Integer                // Optimistic locking version
}
```

#### Execution Entity
```java
// Represents execution of a trade order
{
  "id": Integer,           // Primary key
  "orderId": Integer,      // Reference to trade order
  "quantity": BigDecimal,  // Executed quantity
  "price": BigDecimal,     // Execution price
  "timestamp": OffsetDateTime, // Execution time
  "version": Integer       // Optimistic locking version
}
```

#### Enhanced Response Objects (v2 only)
```java
// Enhanced TradeOrder with external service data
{
  "id": Integer,
  "orderId": Integer,
  "orderType": String,
  "quantity": BigDecimal,
  "quantitySent": BigDecimal,
  "portfolioId": String,
  "portfolioName": String,      // From Portfolio Service
  "securityId": String,
  "securityTicker": String,     // From Security Service
  "blotterId": String,
  "blotterAbbreviation": String,
  "limitPrice": BigDecimal,
  "tradeTimestamp": OffsetDateTime,
  "submitted": Boolean,
  "version": Integer
}
```

## v1 API Reference

### Trade Orders v1

#### GET /api/v1/tradeOrders
**Purpose**: Retrieve all trade orders with optional pagination
**Method**: GET
**Response**: Array of TradeOrderResponseDTO

**Parameters**:
- `limit` (optional): Maximum number of results (1-1000)
- `offset` (optional): Number of records to skip

**Headers** (when pagination used):
- `X-Total-Count`: Total number of records

**Example Request**:
```http
GET /api/v1/tradeOrders?limit=50&offset=100
```

**Example Response**:
```json
[
  {
    "id": 1,
    "orderId": 12345,
    "orderType": "BUY",
    "quantity": 100.00,
    "quantitySent": 50.00,
    "portfolioId": "PORTFOLIO1",
    "securityId": "SEC123",
    "blotterId": "BLOTTER1",
    "blotterAbbreviation": "EQ",
    "limitPrice": 150.25,
    "tradeTimestamp": "2024-01-15T10:30:00Z",
    "submitted": false,
    "version": 1
  }
]
```

#### POST /api/v1/tradeOrders
**Purpose**: Create a new trade order
**Method**: POST
**Content-Type**: application/json

**Request Body**:
```json
{
  "orderId": 12345,
  "orderType": "BUY",
  "quantity": 100.00,
  "quantitySent": 0.00,
  "portfolioId": "PORTFOLIO1",
  "securityId": "SEC123",
  "blotterId": "BLOTTER1",
  "limitPrice": 150.25,
  "tradeTimestamp": "2024-01-15T10:30:00Z"
}
```

#### GET /api/v1/tradeOrders/{id}
**Purpose**: Retrieve specific trade order by ID
**Method**: GET
**Path Parameter**: `id` (Integer) - Trade order ID

#### PUT /api/v1/tradeOrders/{id}
**Purpose**: Update existing trade order
**Method**: PUT
**Path Parameter**: `id` (Integer) - Trade order ID
**Note**: Requires version field for optimistic locking

#### DELETE /api/v1/tradeOrders/{id}
**Purpose**: Delete trade order
**Method**: DELETE
**Query Parameter**: `version` (Integer) - Required for optimistic locking

### Executions v1

#### GET /api/v1/executions
**Purpose**: Retrieve all executions with optional pagination
**Method**: GET
**Parameters**: Same pagination options as trade orders

#### POST /api/v1/executions
**Purpose**: Create new execution
**Method**: POST

**Request Body**:
```json
{
  "orderId": 12345,
  "quantity": 25.00,
  "price": 150.30,
  "timestamp": "2024-01-15T10:35:00Z"
}
```

#### GET /api/v1/executions/{id}
**Purpose**: Retrieve specific execution by ID

#### PUT /api/v1/executions/{id}
**Purpose**: Update existing execution

#### DELETE /api/v1/executions/{id}
**Purpose**: Delete execution

## v2 API Reference

### Trade Orders v2

#### GET /api/v2/tradeOrders
**Purpose**: Advanced trade order retrieval with filtering, sorting, and pagination
**Method**: GET

**Pagination Parameters**:
- `limit` (Integer, default: 50, max: 1000): Maximum number of results to return
- `offset` (Integer, default: 0): Number of results to skip for pagination

**Filtering Parameters**:
- `id` (Integer): Filter by trade order ID
- `orderId` (Integer): Filter by order ID
- `orderType` (String): Comma-separated order types (BUY,SELL,SHORT)
- `portfolio.name` (String): Comma-separated portfolio names
- `security.ticker` (String): Comma-separated ticker symbols
- `quantity.min` (BigDecimal): Minimum quantity filter
- `quantity.max` (BigDecimal): Maximum quantity filter
- `quantitySent.min` (BigDecimal): Minimum quantity sent filter
- `quantitySent.max` (BigDecimal): Maximum quantity sent filter
- `blotter.abbreviation` (String): Comma-separated blotter abbreviations
- `submitted` (Boolean): Filter by submission status

**Sorting Parameters**:
- `sort` (String): Comma-separated sort fields with optional '-' prefix for descending order

**Available Sort Fields**:
- `id`, `orderId`, `orderType`, `quantity`, `quantitySent`
- `portfolioId`, `securityId`, `submitted`
- `security.ticker` (external data)
- `portfolio.name` (external data)
- `blotter.abbreviation`

**Example Request**:
```http
GET /api/v2/tradeOrders?portfolio.name=Growth Fund,Income Fund&orderType=BUY&sort=-quantity,security.ticker&limit=25&offset=0
```

**Example Response**:
```json
{
  "content": [
    {
      "id": 1,
      "orderId": 12345,
      "orderType": "BUY",
      "quantity": 1000.00,
      "quantitySent": 500.00,
      "portfolioId": "PORTFOLIO1",
      "portfolioName": "Growth Fund",
      "securityId": "SEC123",
      "securityTicker": "AAPL",
      "blotterId": "BLOTTER1",
      "blotterAbbreviation": "EQ",
      "limitPrice": 150.25,
      "tradeTimestamp": "2024-01-15T10:30:00Z",
      "submitted": false,
      "version": 1
    }
  ],
  "pageable": {
    "sort": {
      "sorted": true,
      "orders": [
        {"property": "quantity", "direction": "DESC"},
        {"property": "security.ticker", "direction": "ASC"}
      ]
    },
    "pageNumber": 0,
    "pageSize": 25
  },
  "totalElements": 150,
  "totalPages": 6,
  "size": 25,
  "number": 0,
  "numberOfElements": 25,
  "first": true,
  "last": false,
  "empty": false
}
```

### Executions v2

#### GET /api/v2/executions
**Purpose**: Advanced execution retrieval with filtering, sorting, and pagination
**Method**: GET

**Filtering Parameters**:
- `id` (Integer): Filter by execution ID
- `orderId` (Integer): Filter by order ID
- `quantity` (BigDecimal): Filter by execution quantity
- `price` (BigDecimal): Filter by execution price

**Sorting Fields**:
- `id`, `orderId`, `quantity`, `price`

### Batch Operations

#### POST /api/v1/tradeOrders/batch/submit
**Purpose**: Submit multiple trade orders in a single request
**Method**: POST
**Content-Type**: application/json
**Max Batch Size**: 100 orders

**Request Body**:
```json
{
  "tradeOrders": [
    {
      "orderId": 12345,
      "orderType": "BUY",
      "quantity": 100.00,
      "portfolioId": "PORTFOLIO1",
      "securityId": "SEC123",
      "blotterId": "BLOTTER1"
    },
    {
      "orderId": 12346,
      "orderType": "SELL",
      "quantity": 200.00,
      "portfolioId": "PORTFOLIO2",
      "securityId": "SEC456",
      "blotterId": "BLOTTER1"
    }
  ]
}
```

**Response**:
```json
{
  "successCount": 2,
  "failureCount": 0,
  "results": [
    {
      "orderId": 12345,
      "success": true,
      "id": 101,
      "message": "Trade order created successfully"
    },
    {
      "orderId": 12346,
      "success": true,
      "id": 102,
      "message": "Trade order created successfully"
    }
  ]
}
```

## Query Patterns and Examples

### Basic Queries

#### Simple Retrieval (v1)
```http
GET /api/v1/tradeOrders
```
Returns all trade orders as array.

#### Paginated Retrieval (v1)
```http
GET /api/v1/tradeOrders?limit=20&offset=40
```
Returns 20 orders starting from position 40.

#### Basic Pagination (v2)
```http
GET /api/v2/tradeOrders?limit=20&offset=40
```
Returns 20 items starting from position 40 (records 40-59).

### Advanced Filtering (v2 Only)

#### Single Field Filters
```http
GET /api/v2/tradeOrders?orderType=BUY
GET /api/v2/tradeOrders?submitted=false
GET /api/v2/tradeOrders?quantity.min=100
```

#### Multiple Value Filters (OR Logic)
```http
GET /api/v2/tradeOrders?orderType=BUY,SELL
GET /api/v2/tradeOrders?portfolio.name=Growth Fund,Income Fund
GET /api/v2/tradeOrders?security.ticker=AAPL,MSFT,GOOGL
```

#### Range Filters
```http
GET /api/v2/tradeOrders?quantity.min=100&quantity.max=1000
GET /api/v2/tradeOrders?quantitySent.min=50&quantitySent.max=500
```

#### Combined Filters
```http
GET /api/v2/tradeOrders?portfolio.name=Growth Fund&orderType=BUY&submitted=false&quantity.min=100
```

### Sorting Patterns (v2 Only)

#### Single Field Sorting
```http
GET /api/v2/tradeOrders?sort=-quantity
GET /api/v2/tradeOrders?sort=security.ticker
```

#### Multi-Field Sorting
```http
GET /api/v2/tradeOrders?sort=portfolio.name,-quantity
GET /api/v2/tradeOrders?sort=orderType,security.ticker,-quantity
```

### Complex Queries (v2 Only)

#### High-Value BUY Orders in Specific Portfolios
```http
GET /api/v2/tradeOrders?portfolio.name=Growth Fund,Aggressive Growth&orderType=BUY&quantity.min=1000&sort=-quantity&limit=50&offset=0
```

#### Recent Unsubmitted Orders
```http
GET /api/v2/tradeOrders?submitted=false&sort=-tradeTimestamp&limit=25&offset=0
```

#### Technology Stock Orders
```http
GET /api/v2/tradeOrders?security.ticker=AAPL,MSFT,GOOGL,AMZN,TSLA&sort=security.ticker,-quantity
```

## External Service Integration (v2 Only)

### Security Service Integration
- **Purpose**: Enriches responses with security ticker symbols
- **Endpoint**: `http://globeco-security-service:8000/api/v2/securities`
- **Caching**: 5-minute TTL, 1000 entry capacity
- **Fallback**: Returns security ID if service unavailable

### Portfolio Service Integration
- **Purpose**: Enriches responses with portfolio names
- **Endpoint**: `http://globeco-portfolio-service:8000/api/v1/portfolios`
- **Caching**: 5-minute TTL, 1000 entry capacity
- **Fallback**: Returns portfolio ID if service unavailable

### Performance Characteristics
- **Cache Hit Rate**: Typically 80%+ for external service data
- **Response Time Impact**: ~100ms additional latency for cache misses
- **Circuit Breaker**: Activates after 5 consecutive failures

## Error Handling

### HTTP Status Codes
- `200 OK`: Successful retrieval
- `201 Created`: Successful creation
- `400 Bad Request`: Invalid parameters or validation errors
- `404 Not Found`: Resource not found
- `409 Conflict`: Optimistic locking conflict
- `500 Internal Server Error`: Server errors

### Error Response Format
```json
{
  "error": "Bad Request",
  "message": "Invalid query parameters",
  "details": {
    "page": "Page number must be non-negative",
    "size": "Page size must be between 1 and 1000",
    "sortBy": "Invalid sort field: invalidField"
  }
}
```

### Common Validation Errors

#### v1 API
```json
{
  "error": "Bad Request",
  "message": "Validation failed",
  "details": {
    "orderId": "Order ID is required",
    "quantity": "Quantity must be positive"
  }
}
```

#### v2 API
```json
{
  "error": "Bad Request",
  "message": "Invalid query parameters",
  "details": {
    "sortBy": "Invalid sort field: invalidField. Valid fields: id, orderId, orderType, quantity, security.ticker, portfolio.name",
    "size": "Page size must be between 1 and 1000"
  }
}
```

## Performance Considerations

### Query Optimization Tips

#### Use Appropriate Limit Sizes
```http
# Good: Reasonable limit size
GET /api/v2/tradeOrders?limit=50

# Avoid: Very large limit sizes
GET /api/v2/tradeOrders?limit=1000
```

#### Apply Filters to Reduce Dataset
```http
# Good: Filter then sort
GET /api/v2/tradeOrders?portfolio.name=Growth Fund&orderType=BUY&sort=-quantity

# Less efficient: Large unfiltered dataset
GET /api/v2/tradeOrders?sort=-quantity&limit=1000
```

#### Leverage Caching for External Data
```http
# Cached data: portfolio.name and security.ticker fields
GET /api/v2/tradeOrders?sort=portfolio.name,security.ticker
```

### Performance Benchmarks
- **v1 Simple Query**: ~200ms average response time
- **v2 Filtered Query**: ~300ms average response time
- **v2 Complex Query**: ~500ms average response time
- **Batch Operations**: ~2000ms for 100 orders

## Authentication and Authorization

### Current Implementation
- Service-to-service authentication via API keys
- Role-based access control at application level
- Database-level security through connection credentials

### Headers Required
```http
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

## Rate Limiting and Quotas

### Current Limits
- **API Requests**: 1000 requests/minute per client
- **Batch Operations**: 10 batches/minute per client
- **Concurrent Connections**: 100 per client

### Monitoring Endpoints
- **Health Check**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Application Info**: `/actuator/info`

## Integration Patterns

### Polling Pattern (v1)
```javascript
// Simple polling for new orders
setInterval(async () => {
  const response = await fetch('/api/v1/tradeOrders?limit=10');
  const orders = await response.json();
  processNewOrders(orders);
}, 30000); // Every 30 seconds
```

### Efficient Pagination (v2)
```javascript
// Process all pages efficiently
let offset = 0;
const limit = 100;
let hasMore = true;

while (hasMore) {
  const response = await fetch(`/api/v2/tradeOrders?limit=${limit}&offset=${offset}`);
  const data = await response.json();
  
  processOrders(data.tradeOrders);
  
  hasMore = data.tradeOrders.length === limit;
  offset += limit;
}
```

### Filtered Processing (v2)
```javascript
// Process only relevant orders
const response = await fetch('/api/v2/tradeOrders?submitted=false&orderType=BUY&quantity.min=1000&sort=-quantity');
const data = await response.json();

data.tradeOrders.forEach(order => {
  console.log(`Large BUY order: ${order.quantity} ${order.securityTicker} for ${order.portfolioName}`);
});
```

### Batch Creation Pattern
```javascript
// Efficient batch creation
const batchRequest = {
  tradeOrders: orders.map(order => ({
    orderId: order.id,
    orderType: order.type,
    quantity: order.qty,
    portfolioId: order.portfolio,
    securityId: order.security,
    blotterId: order.blotter
  }))
};

const response = await fetch('/api/v1/tradeOrders/batch/submit', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(batchRequest)
});

const result = await response.json();
console.log(`Created ${result.successCount} orders, ${result.failureCount} failures`);
```

## Migration Guidance

### v1 to v2 Migration Strategy

#### Backward Compatibility
- All v1 endpoints remain functional
- Response formats unchanged in v1
- New optional parameters in v1 (limit, offset)

#### Migration Steps
1. **Phase 1**: Use v1 with new pagination parameters
2. **Phase 2**: Migrate read operations to v2 for enhanced features
3. **Phase 3**: Migrate write operations to v2 for consistency
4. **Phase 4**: Deprecate v1 usage (timeline: 12+ months)

#### Response Format Changes
```javascript
// v1 Response (Array)
[{id: 1, orderId: 123, ...}]

// v2 Response (Paginated Object)
{
  tradeOrders: [{id: 1, orderId: 123, ...}],
  pagination: {
    totalElements: 150,
    currentPage: 0,
    pageSize: 50,
    // ... pagination metadata
  }
}
```

## Troubleshooting Guide

### Common Issues

#### Empty Results
**Symptom**: Query returns no results unexpectedly
**Causes**:
- Overly restrictive filters
- Incorrect parameter names
- Date/time format issues

**Solutions**:
```http
# Verify filters step by step
GET /api/v2/tradeOrders?portfolio.name=Growth Fund
GET /api/v2/tradeOrders?portfolio.name=Growth Fund&orderType=BUY
```

#### Slow Response Times
**Symptom**: Queries taking > 1000ms
**Causes**:
- Large page sizes
- Complex sorting without filtering
- External service timeouts

**Solutions**:
```http
# Use smaller limit sizes
GET /api/v2/tradeOrders?limit=25

# Filter before sorting
GET /api/v2/tradeOrders?orderType=BUY&sort=quantity
```

#### Validation Errors
**Symptom**: 400 Bad Request responses
**Common Causes**:
- Invalid sort fields
- Limit size too large (> 1000)
- Negative offset values

### Error Recovery Patterns

#### Retry with Exponential Backoff
```javascript
async function retryRequest(url, maxRetries = 3) {
  for (let i = 0; i < maxRetries; i++) {
    try {
      const response = await fetch(url);
      if (response.ok) return response;
    } catch (error) {
      if (i === maxRetries - 1) throw error;
      await new Promise(resolve => setTimeout(resolve, Math.pow(2, i) * 1000));
    }
  }
}
```

#### Graceful Degradation
```javascript
// Fall back to v1 if v2 fails
async function getTradeOrders() {
  try {
    const response = await fetch('/api/v2/tradeOrders?limit=50&offset=0');
    return await response.json();
  } catch (error) {
    console.warn('v2 API failed, falling back to v1');
    const response = await fetch('/api/v1/tradeOrders?limit=50');
    return { tradeOrders: await response.json() };
  }
}
```

## Best Practices for LLM Integration

### Query Construction
1. **Start Simple**: Begin with basic queries and add complexity incrementally
2. **Use Filtering**: Apply filters before sorting for better performance
3. **Paginate Results**: Always use pagination for large datasets
4. **Cache-Friendly**: Use consistent parameter values to leverage caching

### Error Handling
1. **Check Status Codes**: Always validate HTTP response status
2. **Parse Error Details**: Use the `details` field for specific error information
3. **Implement Retries**: Handle transient failures with exponential backoff
4. **Fallback Strategies**: Have fallback plans for service unavailability

### Performance Optimization
1. **Batch Operations**: Use batch endpoints for multiple operations
2. **Efficient Pagination**: Use v2 pagination over v1 limit/offset
3. **Smart Filtering**: Combine filters to reduce dataset size
4. **Monitor Usage**: Track API usage and performance metrics

This completes the comprehensive API guide optimized for Claude consumption. The guide provides complete context, examples, and patterns needed to effectively interact with the Globeco Trade Service API. 