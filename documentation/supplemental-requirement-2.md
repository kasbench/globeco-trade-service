# Supplemental Requirement 2

1. Add a migration for a new nullable column to the `trade_order` table:
   - Name: `submitted`
   - Data Type: boolean
   - Default: false
   - Not Null: false
   - No migration data is required for existing rows (all will default to false).

2. Add the new column to `TradeOrderResponseDTO` after `blotter` and before `version`. **Do not** update `TradeOrderPostDTO` or `TradeOrderPutDTO`.

3. Add the new column to the `TradeOrder` entity.

4. Add the new column to the `TradeOrderService` service:
   - Ensure the `submitted` field is included in all relevant service methods (e.g., get, list, create, update, etc.)
   - The field should be readable and updatable via the service as appropriate.

5. Update tests:
   - Update entity, service, and controller tests to cover the new field.
   - Test scenarios should include:
     - Default value is `false` for new rows
     - Field is nullable and can be set to `null` or `true`
     - Field is correctly serialized/deserialized in API responses
     - Field is not present in POST/PUT DTOs or requests

6. Update the `README.md` to document the new field in the data model, DTO, and API response examples.

7. Update `openapi.yaml` to include the new field in the response schema for `TradeOrderResponseDTO`.

8. Update anything else that requires updating to ensure consistency across the codebase and documentation.








