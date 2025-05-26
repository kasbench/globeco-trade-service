# Supplemental Requirement 3

In this requirement, we will be adding the following two new APIs

## 1. POST api/v1/tradeOrder/{id}/submit

This API creates an execution record from a trade_order record.  The following table shows the fields in the new input DTO (`TradeOrderSubmitDTO`) and how they map to the new record in the execution table:

| DTO field | `execution` table column |
| --- | --- |
| quantity | quantity_ordered |
| destinationId | destination_id |
---

The remaining fields are mapped as follows

| `execution` table column | Mapping |
| --- | --- |
| execution_timestamp  | Current timestamp |
| execution_status_id | 1 |
| trade_type_id | See mapping table below |
| trade_order_id | id column from the `trade_order` table |
| quantity_placed | 0 |
| quantity_filled | 0 |
| limit_price | limit_price column from the `trade_order` |
| execution_service_id | null |
| version | 1 |
---

`order_type` to `trade_type_id` mapping

| order_type | trade_type_id |
| --- | --- |
| BUY |  1 |
| SELL |  2 |
| SHORT | 3  |
| COVER |  4 |
| EXRC |  5  |
---

Set the `submitted` column on the `trade_order` record to true.

Return the execution response DTO.

**Error Handling and Status Codes:**
- 201 Created: Execution created successfully, returns ExecutionResponseDTO
- 400 Bad Request: Invalid input or mapping
- 404 Not Found: Trade order not found
- 500 Internal Server Error: Unexpected error

### Steps

1. Create the new input DTO (`TradeOrderSubmitDTO`)
2. Update the controller and service with the new API
3. Update the tests
4. Update the README.md
5. Update the openapi.yaml





## 2. POST api/v1/execution/{id}/submit

When an execution is submitted, it is posted to the execution service.  See [execution-service-openapi.yaml](execution-service-openapi.yaml). 

Post to the POST api/v1/executions API using the following mapping:

| Trade Service Execution entity | Execution service ExecutionPostDTO field | Note |
| --- | --- | --- |
| executionStatus.abbreviation | execution_status | |
| tradeType.abbreviation | tradeType |  |
| destination.abbreviation | destination |  |
| securityId | securityId |  | 
| quantity_ordered | quantity |   |
| limit_price | limit_price |   | 
| id | tradeServiceExecutionId |  |
| 1 | version | Hardcoded 1  |

For a successful POST, 
- map the `id` field on the response DTO to the execution_service_id column (executionServiceId field) 
- set quantityPlaced to the value of quantityOrdered
- set executionStatus to 2 (SENT)
- return the ExecutionResponseDTO.

**Error Handling and Status Codes:**
- 200 OK: Submission successful, returns updated ExecutionResponseDTO
- 400 Bad Request: Client error from execution service
- 404 Not Found: Execution not found
- 500 Internal Server Error: Execution service unavailable or unexpected error

If the external execution service call fails, return an appropriate error message and status code as above.

