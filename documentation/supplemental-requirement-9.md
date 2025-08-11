# Supplemental Requirement 9: Add order_id as a query parameter to GET /api/v1/tradeOrders

## Overview
Add `order_id` as an optional query parameter to the existing `GET /api/v1/tradeOrders` endpoint to enable filtering by order ID.

## Functional Requirements

### API Enhancement
- **Endpoint**: `GET /api/v1/tradeOrders`
- **New Parameter**: `order_id` (optional, integer)
- **Behavior**: 
  - When `order_id` is provided: Return only trade orders matching the specified order ID
  - When `order_id` is omitted: Return all trade orders (existing behavior)
- **Backward Compatibility**: This is a non-breaking change - existing clients continue to work unchanged

### Parameter Specification
- **Name**: `order_id`
- **Type**: Integer
- **Required**: No (optional)
- **Description**: Filter trade orders by order ID
- **Example**: `GET /api/v1/tradeOrders?order_id=123456`

### Data Model Context
- The `order_id` field corresponds to the `order_id` column in the `trade_order` database table
- This field has a unique index (`trade_order_order_id_ndx`) ensuring each order_id is unique
- The field is exposed in the API as `orderId` in the `TradeOrderResponseDTO`

## Technical Requirements

### Implementation Tasks
1. **Controller Enhancement**
   - Add `@RequestParam(required = false) Integer orderId` parameter to the GET endpoint
   - Pass the parameter to the service layer for filtering

2. **Service Layer Enhancement**
   - Modify the service method to accept the optional `orderId` parameter
   - Implement conditional filtering logic in the repository/data access layer

3. **Repository/Data Access Enhancement**
   - Add conditional WHERE clause when `order_id` is provided
   - Maintain existing query behavior when parameter is null

4. **OpenAPI Documentation Update**
   - Add the new query parameter to the OpenAPI specification (`openapi.yaml`)
   - Include parameter description, type, and example usage

### Error Handling
- **Maintain Existing Behavior**: All current error responses and status codes remain unchanged
- **Invalid order_id**: If a non-existent order_id is provided, return empty array (consistent with current filtering behavior)
- **Invalid Parameter Type**: Return 400 Bad Request for non-integer values (handled by Spring Boot validation)

### Response Format
- **Success Response**: Same `TradeOrderResponseDTO[]` format as current implementation
- **Empty Results**: Return empty array `[]` when no matches found
- **HTTP Status Codes**: Maintain existing 200 OK for successful requests

## Testing Requirements

### Test Cases
1. **Existing Functionality**: Verify all existing tests pass (no regression)
2. **With order_id Parameter**: 
   - Valid order_id returns matching trade order
   - Non-existent order_id returns empty array
   - Invalid order_id type returns 400 error
3. **Without order_id Parameter**: Verify existing behavior unchanged
4. **Integration Tests**: Test end-to-end API behavior with various scenarios

## Documentation Updates Required

### Files to Update
1. **OpenAPI Specification** (`openapi.yaml`)
   - Add `order_id` query parameter to `/tradeOrders` GET endpoint
   - Include parameter documentation and examples

2. **API Documentation** (if separate from OpenAPI)
   - Update any additional API documentation with the new parameter

### Example API Documentation
```yaml
parameters:
  - name: order_id
    in: query
    required: false
    schema:
      type: integer
    description: Filter trade orders by order ID
    example: 123456
```

## Acceptance Criteria
- [ ] `GET /api/v1/tradeOrders?order_id=123` returns only trade orders with order_id=123
- [ ] `GET /api/v1/tradeOrders` continues to return all trade orders (unchanged behavior)
- [ ] `GET /api/v1/tradeOrders?order_id=999999` returns empty array for non-existent order_id
- [ ] `GET /api/v1/tradeOrders?order_id=invalid` returns 400 Bad Request
- [ ] All existing tests continue to pass
- [ ] OpenAPI documentation includes the new parameter
- [ ] Response format and error handling remain consistent with existing implementation
