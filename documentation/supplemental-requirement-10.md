# Supplemental Requirement 10: Add order_id as a query parameter to GET /api/v2/tradeOrders

## Overview
Add `order_id` as an optional query parameter to the existing `GET /api/v2/tradeOrders` endpoint to enable filtering by order ID.

## Functional Requirements

### API Enhancement
- **Endpoint**: `GET /api/v2/tradeOrders`
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
- **Example**: `GET /api/v2/tradeOrders?order_id=123456`

### Data Model Context
- The `order_id` field corresponds to the `order_id` column in the `trade_order` database table
- This field has a unique index (`trade_order_order_id_ndx`) ensuring each order_id is unique
- The field is exposed in the API as `orderId` in the `TradeOrderResponseDTO`

## Technical Requirements

### Implementation Tasks
1. **Controller Enhancement**
   - Add `@RequestParam(required = false) Integer orderId` parameter to the GET /api/v2/tradeOrders endpoint
   - Pass the parameter to the service layer for filtering

2. **Service Layer Enhancement**
   - Modify the service method to accept the optional `orderId` parameter for v2 endpoint
   - Implement conditional filtering logic in the repository/data access layer

3. **Repository/Data Access Enhancement**
   - Add conditional WHERE clause when `order_id` is provided
   - Maintain existing query behavior when parameter is null

4. **OpenAPI Documentation Update**
   - Add the new query parameter to the OpenAPI specification (`openapi.yaml`) for the v2 endpoint
   - Include parameter description, type, and example usage

### Error Handling
- **Maintain Existing Behavior**: All current error responses and status codes remain unchanged
- **Invalid order_id**: If a non-existent order_id is provided, return empty array (consistent with current filtering behavior)
- **Invalid Parameter Type**: Return 400 Bad Request for non-integer values (handled by Spring Boot validation)

### Response Format
- **Success Response**: Same `TradeOrderResponseDTO[]` format as current v2 implementation
- **Empty Results**: Return empty array `[]` when no matches found
- **HTTP Status Codes**: Maintain existing 200 OK for successful requests

## Testing Requirements

### Test Cases
1. **Existing Functionality**: Verify all existing v2 endpoint tests pass (no regression)
2. **With order_id Parameter**: 
   - Valid order_id returns matching trade order
   - Non-existent order_id returns empty array
   - Invalid order_id type returns 400 error
3. **Without order_id Parameter**: Verify existing v2 endpoint behavior unchanged
4. **Integration Tests**: Test end-to-end API behavior with various scenarios for v2 endpoint

## Documentation Updates Required

### Files to Update
1. **OpenAPI Specification** (`openapi.yaml`)
   - Add `order_id` query parameter to `/api/v2/tradeOrders` GET endpoint
   - Include parameter documentation and examples

2. **API Documentation** (if separate from OpenAPI)
   - Update any additional API documentation with the new parameter for v2 endpoint

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
- [ ] `GET /api/v2/tradeOrders?order_id=123` returns only trade orders with order_id=123
- [ ] `GET /api/v2/tradeOrders` continues to return all trade orders (unchanged behavior)
- [ ] `GET /api/v2/tradeOrders?order_id=999999` returns empty array for non-existent order_id
- [ ] `GET /api/v2/tradeOrders?order_id=invalid` returns 400 Bad Request
- [ ] All existing v2 endpoint tests continue to pass
- [ ] OpenAPI documentation includes the new parameter for v2 endpoint
- [ ] Response format and error handling remain consistent with existing v2 implementation

## Relationship to Other Requirements
This requirement is functionally identical to [Supplemental Requirement 9](supplemental-requirement-9.md), which implements the same enhancement for the `/api/v1/tradeOrders` endpoint. The implementation approach and acceptance criteria are the same, but applied to the v2 API endpoint.