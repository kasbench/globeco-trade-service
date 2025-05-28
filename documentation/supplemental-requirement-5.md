# Supplemental Requirement 5

## Overview
Add a new API endpoint to update execution fill information, allowing external systems to report partial or complete fills on executions.

## API Specification

### Endpoint
- **PUT** `/api/v1/executions/{id}/fill`

### Request DTO
Create a new DTO called `ExecutionPutFillDTO`:
```json
{
  "executionStatus": "PART",
  "quantityFilled": 50.00,
  "version": 1
}
```

### Response DTO
- **ExecutionResponseDTO** (existing DTO)

## Business Logic

### Update Rules
1. **Quantity Filled**: Update the `quantity_filled` column with the provided value
2. **Execution Status**: Map the `executionStatus` string to the corresponding `execution_status_id` and update the column
3. **Unchanged Fields**: Leave all other execution fields unchanged
4. **Optimistic Concurrency**: Apply standard optimistic locking using the `version` field

### Validation Rules
1. **Execution Exists**: The execution with the specified ID must exist
2. **Version Match**: The provided version must match the current execution version
3. **Valid Status**: The `executionStatus` must be a valid execution status abbreviation (e.g., "NEW", "SENT", "PART", "FILL", "CANC")
4. **Quantity Validation**: `quantityFilled` must be >= 0 and <= `quantityPlaced`

## Error Handling

### HTTP Status Codes
- **200 OK**: Successful update
- **400 Bad Request**: Invalid input (invalid status, quantity out of range, etc.)
- **404 Not Found**: Execution not found
- **409 Conflict**: Version mismatch (optimistic locking failure)

### Error Response Format
```json
{
  "error": "Error message description"
}
```

### Specific Error Messages
- Version mismatch: "Version mismatch. Expected version: {expected}, provided: {provided}"
- Invalid status: "Invalid execution status: {status}. Valid values are: NEW, SENT, PART, FILL, CANC"
- Invalid quantity: "Quantity filled ({quantityFilled}) cannot exceed quantity placed ({quantityPlaced})"
- Execution not found: "Execution not found with id: {id}"

## Example Payloads

### Successful Request
```
PUT /api/v1/executions/123/fill
Content-Type: application/json

{
  "executionStatus": "PART",
  "quantityFilled": 50.00,
  "version": 1
}
```

### Successful Response
```
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": 123,
  "executionTimestamp": "2024-06-10T12:00:00Z",
  "executionStatus": {
    "id": 3,
    "abbreviation": "PART",
    "description": "Partially Filled",
    "version": 1
  },
  "quantityOrdered": "100.00",
  "quantityPlaced": "100.00",
  "quantityFilled": "50.00",
  "limitPrice": "10.00",
  "executionServiceId": 55555,
  "version": 2,
  // ... other fields
}
```

## Implementation Requirements

### Code Changes
1. **DTO Creation**: Create `ExecutionPutFillDTO` class
2. **Controller Method**: Add `fillExecution` method to `ExecutionController`
3. **Service Method**: Add `fillExecution` method to `ExecutionService` and `ExecutionServiceImpl`
4. **Status Mapping**: Implement logic to map status abbreviations to IDs

### Testing Requirements
1. **Unit Tests**: Test service layer logic with various scenarios
2. **Integration Tests**: Test controller endpoint with valid and invalid inputs
3. **Error Cases**: Test all error conditions (404, 400, 409)
4. **Edge Cases**: Test boundary conditions for quantity validation

### Documentation Updates
1. **README.md**: Add the new endpoint to the Execution API section
2. **openapi.yaml**: Add the new endpoint specification with request/response schemas

## Consistency Notes
- Follow existing patterns for error handling and response formats
- Use the same optimistic concurrency approach as other PUT endpoints
- Maintain consistency with existing DTO naming conventions
- Follow the same testing patterns as other API endpoints

