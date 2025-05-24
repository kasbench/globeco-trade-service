# Supplemental Requirements


## Update database

This is the first supplemental requirement for the trade service. The data model has been modified:
- The `execution` table now has a new column called `execution_service_id` (type: integer, nullable).
- This column will store the `id` field returned when posting to the execution service (`POST /api/v1/executions`).
- The updated schema is shown in [trade-service.sql](../trade-service.sql).
- A new index named `execution_service_id_ndx` must be created on the `execution_service_id` column.

**Steps:**

1. **Database Migration:**
   - Update the schema in [V1__init_schema.sql](../src/main/resources/db/migration/V1__init_schema.sql) to add the `execution_service_id` column to the `execution` table.
   - Add a new index named `execution_service_id_ndx` on the `execution_service_id` column.

2. **DTO and Service Update:**
   - Update all relevant execution DTOs (e.g., `ExecutionRequestDTO`, `ExecutionResponseDTO`) to include a `executionServiceId` field mapped to the new column.
   - Update the service layer (e.g., `ExecutionService`) to handle the new field.
   - Ensure that the mapping between the database (snake_case: `execution_service_id`) and Java/DTO (camelCase: `executionServiceId`) is consistent and documented.
   - Update all relevant tests (unit, integration, and controller tests) to reflect this change.

3. **Documentation:**
   - Update [README.md](../README.md) and [openapi.yaml](../openapi.yaml) to reflect these changes, including schema definitions and example payloads for the new field and API.
   - Ensure that naming conventions are clear: use snake_case for database columns and camelCase for Java/DTO fields.


## Cross Origin Resource Sharing (CORS)
* Must allow all origins
* Global CORS configuration


## GitHub CI-CD
* Add GitHub CI-CD (actions) to perform a multiarchitecture build (ARM and AMD for Linux) and deploy to DockerHub.

<!-- ## Test Containers

* Modify tests to use testcontainers -->

## New requirement

Create a new API:

`POST /api/v1/execution/{id}/submit`

This API calls the POST `/api/v1/executions` API of the globeco-execution-service on port 8084. The openapi schema for the execution service is at [documentation/execution-service-openapi.yaml](execution-service-openapi.yaml).

### Field Mapping

| Execution Service DTO Field | Trade Service Field                        |
|----------------------------|--------------------------------------------|
| executionStatus            | execution.executionStatus.abbreviation     |
| tradeType                  | execution.tradeType.abbreviation           |
| destination                | execution.destination.abbreviation         |
| securityId                 | execution.tradeOrder.securityId            |
| quantity                   | execution.quantity                         |
| limitPrice                 | execution.limitPrice                       |
| tradeServiceExecutionId | execution.id
| version                    | 1                                          |
---

### API Behavior

- After calling the execution service:
  - If the call is successful (HTTP 2xx):
    - Save the returned `id` as `execution_service_id` in the `execution` table (persisted in the database).
    - Update the execution status from "NEW" to "SENT" (persisted in the database).
    - Return HTTP 200 with a status of "submitted".
  - If the call fails:
    - Do not update `execution_service_id` (it remains null).
    - Return HTTP 400 for client errors (e.g., invalid data sent to execution service).
    - Return HTTP 500 for server errors (e.g., execution service unavailable).
    - Log the error for audit purposes.

### Example Request

```
POST /api/v1/execution/123/submit
Content-Type: application/json

{
  // No body required; the execution is identified by path variable
}
```

### Example Success Response

```
HTTP/1.1 200 OK
Content-Type: application/json

{
  "status": "submitted"
}
```

### Example Error Response (Execution Service Unavailable)

```
HTTP/1.1 500 Internal Server Error
Content-Type: application/json

{
  "error": "Failed to submit execution: execution service unavailable"
}
```

### Tests
- Generate unit, integration, and controller tests for this new API.
- Test scenarios should include:
  - Successful submission and status update
  - Execution service returns client error (400)
  - Execution service returns server error (500)
  - Database update failure
  - Execution not found

## Update documentation

* Update README.md and openapi.yaml to reflect these changes, including new/updated schema definitions and example payloads for the new API.
* Ensure all naming conventions and field mappings are clearly documented.
