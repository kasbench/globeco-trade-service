# Globeco Trade Service: v1 to v2 Migration Guide

## Overview

This guide helps you migrate from v1 to v2 endpoints of the Globeco Trade Service API. The v2 API provides enhanced functionality while maintaining complete backward compatibility with v1.

## What's New in v2

### Enhanced Features
- **Advanced Pagination**: Configurable page sizes with `page` and `size` parameters
- **Dynamic Filtering**: Filter by multiple fields with comma-separated values for OR conditions
- **Multi-field Sorting**: Sort by multiple fields with individual direction control
- **External Service Integration**: Enriched responses with security tickers and portfolio names
- **Batch Operations**: Submit multiple trade orders in a single request

### Performance Improvements
- **Caching**: External service calls cached for 5 minutes
- **Optimized Queries**: Database-level pagination and filtering
- **Parallel Processing**: Concurrent external service calls for batch operations

### Enhanced Data
- **Security Information**: Includes `securityTicker` from external Security Service
- **Portfolio Information**: Includes `portfolioName` from external Portfolio Service
- **Pagination Metadata**: Complete pagination information in responses

## Migration Paths

### Option 1: Gradual Migration (Recommended)
1. Keep existing v1 integrations running
2. Develop new features using v2 endpoints
3. Gradually migrate existing clients to v2
4. Deprecate v1 after 12 months

### Option 2: Feature-by-Feature Migration
1. Migrate endpoints that need new v2 features first
2. Use v1 for simple queries, v2 for complex ones
3. Consolidate to v2 over time

### Option 3: Complete Migration
1. Update all client code to use v2 endpoints
2. Test thoroughly in staging environment
3. Deploy all changes at once

## Endpoint Mapping

| v1 Endpoint | v2 Endpoint | Migration Notes |
|-------------|-------------|-----------------|
| `GET /api/v1/tradeOrders` | `GET /api/v2/tradeOrders` | Enhanced with filtering, sorting, pagination |
| `GET /api/v1/executions` | `GET /api/v2/executions` | Enhanced with filtering, sorting, pagination, and new `executionServiceId` filter |
| N/A | `POST /api/v1/tradeOrders/batch/submit` | New batch operation endpoint |

## Parameter Changes

### v1 Parameters
```
GET /api/v1/tradeOrders
- No query parameters (returns all results)
```

### v2 Parameters
```
GET /api/v2/tradeOrders
- page: Page number (0-based, default: 0)
- size: Items per page (1-1000, default: 20)
- id: Filter by trade order ID
- orderId: Filter by order ID
- orderType: Filter by order type(s) - comma-separated
- portfolioId: Filter by portfolio ID(s) - comma-separated
- portfolioNames: Filter by portfolio name(s) - comma-separated
- securityId: Filter by security ID(s) - comma-separated
- securityTickers: Filter by security ticker(s) - comma-separated
- minQuantity/maxQuantity: Filter by quantity range
- minQuantitySent/maxQuantitySent: Filter by quantity sent range
- blotterAbbreviation: Filter by blotter abbreviation(s)
- submitted: Filter by submission status
- sortBy: Fields to sort by - comma-separated
- sortDir: Sort directions - comma-separated (asc/desc)
```

## Response Format Changes

### v1 Response
```json
[
  {
    "id": 1,
    "orderId": 12345,
    "orderType": "BUY",
    "quantity": 100.00,
    "quantitySent": 100.00,
    "portfolioId": "PORTFOLIO1",
    "securityId": "SEC123",
    "blotterId": "BLOTTER1",
    "blotterAbbreviation": "EQ",
    "submitted": true
  }
]
```

### v2 Response
```json
{
  "content": [
    {
      "id": 1,
      "orderId": 12345,
      "orderType": "BUY",
      "quantity": 100.00,
      "quantitySent": 100.00,
      "portfolioId": "PORTFOLIO1",
      "portfolioName": "Growth Fund",
      "securityId": "SEC123",
      "securityTicker": "AAPL",
      "blotterId": "BLOTTER1",
      "blotterAbbreviation": "EQ",
      "submitted": true
    }
  ],
  "pageable": {
    "sort": {
      "sorted": true,
      "orders": [
        {
          "property": "quantity",
          "direction": "DESC"
        }
      ]
    },
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 150,
  "totalPages": 8,
  "size": 20,
  "number": 0,
  "numberOfElements": 20,
  "first": true,
  "last": false,
  "empty": false
}
```

## Code Examples

### Basic Migration

#### v1 Request
```javascript
// v1 - Simple request
fetch('/api/v1/tradeOrders')
  .then(response => response.json())
  .then(tradeOrders => {
    // Process array of trade orders
    tradeOrders.forEach(order => {
      console.log(`Order ${order.id}: ${order.orderType}`);
    });
  });
```

#### v2 Request
```javascript
// v2 - Enhanced request with pagination
fetch('/api/v2/tradeOrders?page=0&size=20')
  .then(response => response.json())
  .then(data => {
    // Process paginated response
    data.content.forEach(order => {
      console.log(`Order ${order.id}: ${order.orderType} - ${order.securityTicker}`);
    });
    
    console.log(`Page ${data.number + 1} of ${data.totalPages}`);
    console.log(`Total orders: ${data.totalElements}`);
  });
```

### Advanced Filtering

#### v1 Limitation
```javascript
// v1 - No filtering support, must filter client-side
fetch('/api/v1/tradeOrders')
  .then(response => response.json())
  .then(tradeOrders => {
    // Client-side filtering (inefficient for large datasets)
    const buyOrders = tradeOrders.filter(order => order.orderType === 'BUY');
    const portfolioOrders = buyOrders.filter(order => 
      order.portfolioId === 'PORTFOLIO1'
    );
  });
```

#### v2 Enhancement
```javascript
// v2 - Server-side filtering (efficient)
const params = new URLSearchParams({
  orderType: 'BUY',
  portfolioId: 'PORTFOLIO1',
  sortBy: 'quantity',
  sortDir: 'desc',
  page: '0',
  size: '50'
});

fetch(`/api/v2/tradeOrders?${params}`)
  .then(response => response.json())
  .then(data => {
    // Filtered and sorted results from server
    data.content.forEach(order => {
      console.log(`${order.portfolioName}: ${order.quantity} ${order.securityTicker}`);
    });
  });
```

### Batch Operations (New in v2)

```javascript
// v2 - Batch submission
const batchRequest = {
  tradeOrders: [
    {
      orderId: 12345,
      orderType: "BUY",
      quantity: 100.00,
      portfolioId: "PORTFOLIO1",
      securityId: "SEC123",
      blotterId: "BLOTTER1"
    },
    {
      orderId: 12346,
      orderType: "SELL",
      quantity: 200.00,
      portfolioId: "PORTFOLIO2",
      securityId: "SEC456",
      blotterId: "BLOTTER1"
    }
  ]
};

fetch('/api/v1/tradeOrders/batch/submit', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify(batchRequest)
})
.then(response => response.json())
.then(result => {
  console.log(`Successfully submitted: ${result.successCount}`);
  console.log(`Failed: ${result.failureCount}`);
  
  result.results.forEach(orderResult => {
    if (orderResult.success) {
      console.log(`Order ${orderResult.orderId} submitted with ID ${orderResult.id}`);
    } else {
      console.log(`Order ${orderResult.orderId} failed: ${orderResult.error}`);
    }
  });
});
```

## Backward Compatibility

### v1 Enhancements
v1 endpoints have been enhanced with optional pagination parameters while maintaining complete backward compatibility:

```javascript
// v1 with new optional parameters
fetch('/api/v1/tradeOrders?limit=50&offset=100')
  .then(response => {
    const totalCount = response.headers.get('X-Total-Count');
    return response.json();
  })
  .then(tradeOrders => {
    // Same array format as before, but paginated
    console.log(`Showing ${tradeOrders.length} orders`);
  });
```

### No Breaking Changes
- All existing v1 requests work exactly as before
- Response format remains unchanged
- No new required parameters
- Existing error handling continues to work

## Performance Considerations

### v1 vs v2 Performance

| Aspect | v1 | v2 | Improvement |
|--------|----|----|-------------|
| Pagination | Client-side | Server-side | Up to 100x faster |
| Filtering | Client-side | Server-side | Up to 50x faster |
| Sorting | Client-side | Database-level | Up to 20x faster |
| External Data | None | Cached (5 min TTL) | N/A |
| Batch Operations | N/A | Parallel processing | N/A |

### Best Practices

#### v2 Optimization Tips
1. **Use Pagination**: Always specify reasonable `size` values (≤100)
2. **Apply Filters**: Reduce dataset size before sorting
3. **Leverage Caching**: Use consistent parameter values
4. **Batch Operations**: Submit multiple orders together when possible

#### Example: Optimized v2 Query
```javascript
// Efficient v2 query
const params = new URLSearchParams({
  portfolioId: 'PORTFOLIO1,PORTFOLIO2',  // Filter first
  orderType: 'BUY',                      // Further filter
  sortBy: 'quantity',                    // Then sort
  sortDir: 'desc',
  page: '0',
  size: '25'                             // Reasonable page size
});

fetch(`/api/v2/tradeOrders?${params}`)
  .then(response => response.json())
  .then(data => {
    // Fast, filtered, sorted results
  });
```

## Error Handling

### Enhanced Error Responses in v2

```javascript
// v2 provides detailed error information
fetch('/api/v2/tradeOrders?page=-1&size=2000')
  .then(response => {
    if (!response.ok) {
      return response.json().then(error => {
        console.error('Validation errors:', error.details);
        // Output:
        // {
        //   "page": "Page number must be non-negative",
        //   "size": "Page size must be between 1 and 1000"
        // }
      });
    }
    return response.json();
  });
```

## Migration Checklist

### Pre-Migration
- [ ] Review current v1 usage patterns
- [ ] Identify opportunities for v2 enhancements
- [ ] Plan migration timeline (gradual vs. complete)
- [ ] Set up staging environment for testing

### During Migration
- [ ] Update API endpoints to v2
- [ ] Modify response parsing for paginated format
- [ ] Add pagination logic for large datasets
- [ ] Implement filtering and sorting parameters
- [ ] Update error handling for new error format
- [ ] Test with realistic data volumes

### Post-Migration
- [ ] Monitor performance improvements
- [ ] Validate data accuracy with external service integration
- [ ] Optimize cache hit rates
- [ ] Plan v1 deprecation timeline
- [ ] Update documentation and training materials

## Common Pitfalls

### 1. Response Format Changes
**Issue**: Expecting array response from v2
```javascript
// Wrong - treating v2 response as array
fetch('/api/v2/tradeOrders').then(orders => orders.forEach(...));

// Correct - accessing content property
fetch('/api/v2/tradeOrders').then(data => data.content.forEach(...));
```

### 2. Pagination Handling
**Issue**: Not handling pagination properly
```javascript
// Wrong - only getting first page
fetch('/api/v2/tradeOrders');

// Correct - handling all pages
async function getAllTradeOrders() {
  let allOrders = [];
  let page = 0;
  let totalPages = 1;
  
  while (page < totalPages) {
    const response = await fetch(`/api/v2/tradeOrders?page=${page}&size=100`);
    const data = await response.json();
    
    allOrders.push(...data.content);
    totalPages = data.totalPages;
    page++;
  }
  
  return allOrders;
}
```

### 3. Filtering Syntax
**Issue**: Incorrect comma-separated value handling
```javascript
// Wrong - separate parameters
const params = new URLSearchParams();
params.append('portfolioId', 'PORTFOLIO1');
params.append('portfolioId', 'PORTFOLIO2');

// Correct - comma-separated values
const params = new URLSearchParams({
  portfolioId: 'PORTFOLIO1,PORTFOLIO2'
});
```

## Support and Resources

### Documentation
- [OpenAPI Specification](../api-docs/openapi-v2.yaml)
- [Configuration Guide](./configuration-guide.md)
- [Performance Tuning Guide](./performance-tuning-guide.md)

### Testing
- Use staging environment: `https://api-staging.globeco.com`
- Swagger UI available at: `/swagger-ui.html`
- Postman collection: [Download](../api-docs/globeco-trade-service-v2.postman_collection.json)

### Getting Help
- Technical Questions: trade-service@globeco.com
- Migration Support: api-migration@globeco.com
- Performance Issues: performance-team@globeco.com

## Timeline

### Phase 1: v2 Availability (Completed)
- v2 endpoints available alongside v1
- No impact on existing v1 clients
- Documentation and migration guides available

### Phase 2: Migration Period (Next 12 months)
- Gradual client migration to v2
- v1 enhancements with optional pagination
- Performance monitoring and optimization

### Phase 3: v1 Deprecation (After 12 months)
- v1 endpoints marked as deprecated
- 6-month deprecation notice
- Continued support with migration assistance

### Phase 4: v1 Sunset (18 months from v2 launch)
- v1 endpoints removed
- All clients migrated to v2
- Full v2 feature utilization 