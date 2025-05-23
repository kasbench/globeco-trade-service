# Supplemental Requirements


## Update database

This is the first supplemental requirement for the trade service. The data model has been modified:  
- The `execution` table now has a new column called `execution_service_id` (type: integer, nullable).  
- This column will store the `id` field returned when posting to the execution service (`POST /api/v1/executions`).  
- The updated schema is shown in [trade-service.sql](../trade-service.sql).
- A new index named `execution_service_id_ndx` must be created on the `execution_service_id` column.

**Steps:**

1. **Database Migration:**  
   - Update the schema in [V1__init_schema.sql](../src/main/resources/db/migration/V1__init_schema.sql) to add the `execution_service_id` column to the `execution` table.  Please note that there is also an index on the execution table that needs to be added.

2. **DTO and Service Update:**  
   - Update all relevant execution DTOs and the service layer to include a `executionServiceId` field mapped to the new column.
   - Update all relevant tests to reflect this change.

3. **Documentation:**  
   - Update [README.md](../README.md) and [openapi.yaml](../openapi.yaml) to reflect these changes, including schema definitions and example payloads.



## Cross Origin Resource Sharing (CORS)
* Must allow all origins


## GitHub CI-CD
* Add GitHub CI-CD (actions) to perform a multiarchitecture build (ARM and AMD for Linux) and deploy to DockerHub.  

<!-- ## Test Containers

* Modify tests to use testcontainers -->

## New requirement

Create a new API

`POST /api/v1/execution/{id}/submit`

This API calls the the POST /api/v1/executions API of the globeco-execution-service on port 8084.  The openapi schema for the execution service is at [documentation/execution-service-openapi.yaml](execution-service-openapi.yaml).

Map the fields to the DTO as follows:

| DTO Field | Trade Service Field |
| --- | --- |
| executionStatus | execution.executionStatus.abbreviation |
| tradeType |execution.tradeType.abbreviation |
| destination | execution.destination.abbreviation |
| securityId | execution.tradeOrder.securityId |
| quantity | execution.quantity |
| limitPrice | execution.limitPrice |
| version | 1 |

After calling the execution service, save the `id` returned from the service service as the `execution_service_id` (`executionServiceId`) for the execution.  If the execution service call fails, ensure the `executionServiceId` remains null and handle the error appropriately (e.g., return a 400 or 500 error).  If successful, change the execution status from "NEW" to "SENT".  Return 200 and status of "submitted" if successful.

Please also generate tests for this new API.

## Update documentation

* Update README.md and openapi.yaml to reflect these changes