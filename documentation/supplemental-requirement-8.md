# Supplemental Requirement 8

## Overview

Enhance the executions API to support filtering by `executionServiceId`, improve query performance, and update documentation and tests accordingly.

## Requirements

1. **API Enhancement:**
   - Add `executionServiceId` as an optional query parameter to `GET /api/v2/executions`.
   - When provided, this parameter must filter results to only those executions where `execution.execution_service_id` matches the given value.
   - Only a single integer value is supported per request (no multi-value or range support).

2. **Database Index:**
   - Ensure there is an index on the `execution.execution_service_id` column to optimize queries using this filter.
   - The index `execution_service_id_ndx` already exists; no migration is required.

3. **Testing:**
   - Add or update tests to verify:
     - Filtering by `executionServiceId` returns only matching executions.
     - The filter works in combination with other filters and pagination.
     - Edge cases: no matches, invalid values, and missing parameter.

4. **Documentation:**
   - Update `README.md` to document the new query parameter, including:
     - Description of the parameter.
     - Example requests and responses.
   - Update `openapi.yaml` (and any other OpenAPI specs) to include the new parameter and its behavior.
   - Update any API documentation or guides that reference `/api/v2/executions` to reflect this change.

5. **Error Handling:**
   - No changes to error handling or response DTOs are required for this enhancement.

## Implementation Notes

- The `execution_service_id` column exists in the schema.
- An index named `execution_service_id_ndx` already exists on this column, so no migration is required.
- The controller and service layer for `/api/v2/executions` will need to be updated to accept and process the new query parameter.
- Tests should be added or updated in the relevant test classes (likely under `ExecutionV2ControllerTest` or similar).

---

**Implementation Status:**
- [x] Code changes complete (controller, service, repository/specification)
- [x] Tests added for filtering by executionServiceId
- [x] Documentation updated (OpenAPI, README, API guides, migration guide)

**Ready for review.**

