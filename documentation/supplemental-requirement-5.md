# Supplemental Requirement 5

Add a new API
- PUT api/v1/execution/{id}/fill
- Request DTO called ExecutionPutFillResponseDTO:
```json
{
  "executionStatus": "PART",
  "quantityFilled": 0,
  "version": 0,
}
```
- Response DTO: ExecutionResponseDTO
- Logic
    - Update the quantity_filled column
    - Map executionStatus to execution_status_id and update column
    - Leave all other columns unchanged
    - Apply normal optimistic concurrency
- HTTP return codes and error handling should be consistent with other APIs
- Tests should be consistent with other APIs
- Update README.md and openapi.yaml

