# Supplemental Requirement 7: Automated Execution Submission

## Overview

Currently, submitting a trade order for execution requires a two-step process:
1. **Trade Order Submission**: Call `POST /api/v1/tradeOrders/{id}/submit` or `POST /api/v1/tradeOrders/batch/submit`
2. **Execution Submission**: Call `POST /api/v1/execution/{id}/submit` (which submits to the external Execution Service)

This requirement modifies the trade order submission endpoints to automatically perform both steps in a single operation, streamlining the user experience and reducing the potential for incomplete submissions.

## Functional Requirements

### 1. Modified Endpoint Behavior

**Affected Endpoints:**
- `POST /api/v1/tradeOrders/{id}/submit`
- `POST /api/v1/tradeOrders/batch/submit`

**New Default Behavior:**
When these endpoints are called, they will automatically:
1. Create the execution record in the local database (existing behavior)
2. **NEW**: Automatically submit the execution to the external Execution Service via `POST /api/v1/executions`
3. Update the execution status to "SENT" and store the external execution service ID

### 2. Optional Query Parameter

**Parameter:** `noExecuteSubmit` (boolean, optional, default: false)

**Behavior:**
- When `noExecuteSubmit=false` (default): Perform both trade order submission and execution submission
- When `noExecuteSubmit=true`: Perform only trade order submission (legacy behavior)

**Examples:**
```
POST /api/v1/tradeOrders/123/submit?noExecuteSubmit=true    # Legacy behavior only
POST /api/v1/tradeOrders/123/submit?noExecuteSubmit=false   # New behavior (explicit)
POST /api/v1/tradeOrders/123/submit                        # New behavior (default)
```

### 3. Rollback Behavior

If the execution submission to the external service fails:
1. **Delete** the execution record from the local `execution` table
2. **Reset** the trade order's `quantity_sent` field to its previous value
3. **Reset** the trade order's `submitted` status if necessary
4. **Return** appropriate error response to the client

### 4. Transaction Management and Compensating Actions

To avoid database locking during external API calls, the implementation will use **compensating transactions** rather than a single database transaction spanning the external service call.

**Transaction Strategy:**
1. **Phase 1**: Complete trade order submission and execution creation in a single database transaction
2. **Phase 2**: Attempt external execution service submission (outside database transaction)
3. **Phase 3**: If external submission fails, execute compensating transaction to rollback Phase 1 changes

### 5. Retry Logic

The implementation will include retry logic for execution service failures following best practices:
- **Retry Scenarios**: Network timeouts, temporary server errors (5xx), connection failures
- **No Retry Scenarios**: Client errors (4xx), authentication failures
- **Retry Strategy**: Exponential backoff with maximum retry attempts (suggested: 3 attempts)
- **Total Timeout**: Maximum time limit for all retry attempts (suggested: 30 seconds)

### 6. Batch Processing Behavior

For batch operations (`POST /api/v1/tradeOrders/batch/submit`):
- Each trade order in the batch is processed independently
- If some executions succeed and others fail, only the failed ones are rolled back
- Successful submissions remain committed
- The response indicates success/failure status for each individual trade order

## Technical Requirements

### 4.1 Endpoint Modifications

**Single Trade Order Submission:**
- Endpoint: `POST /api/v1/tradeOrders/{id}/submit`
- Add query parameter: `noExecuteSubmit` (boolean, optional, default: false)
- Modify service layer to call execution submission when `noExecuteSubmit=false`

**Batch Trade Order Submission:**
- Endpoint: `POST /api/v1/tradeOrders/batch/submit`
- Add query parameter: `noExecuteSubmit` (boolean, optional, default: false)
- Apply the same logic to each trade order in the batch

### 4.2 Service Layer Changes

**TradeOrderServiceImpl.submitTradeOrder:**
- Add `noExecuteSubmit` parameter
- If `noExecuteSubmit=false`, call `ExecutionService.submitExecution()` after creating execution
- Implement compensating transaction rollback logic if execution submission fails
- Implement retry logic with exponential backoff for external service calls

**BatchTradeOrderService.processTradeOrderSubmission:**
- Add support for `noExecuteSubmit` parameter
- Apply the same logic per trade order
- Handle individual trade order failures without affecting successful ones

### 4.3 Error Handling

**Execution Service Failure Scenarios:**
- **400 Bad Request**: Return 400 to client with execution service error message (no retry)
- **500 Server Error**: Retry with exponential backoff, then return 500 if all retries fail
- **Network/Timeout**: Retry with exponential backoff, then return 500 if all retries fail

**Compensating Transaction Actions:**
1. Delete execution record: `executionRepository.deleteById(execution.getId())`
2. Restore trade order state:
   - `tradeOrder.setQuantitySent(originalQuantitySent)`
   - `tradeOrder.setSubmitted(originalSubmittedStatus)`
   - `tradeOrderRepository.save(tradeOrder)`

### 4.4 Retry Implementation Details

**RetryTemplate Configuration:**
```java
RetryTemplate retryTemplate = RetryTemplate.builder()
    .maxAttempts(3)
    .exponentialBackoff(1000, 2, 10000)
    .retryOn(ResourceAccessException.class, HttpServerErrorException.class)
    .build();
```

**Timeout Configuration:**
- Individual request timeout: 10 seconds
- Total operation timeout (including retries): 30 seconds

## API Documentation Updates

### 4.5 OpenAPI Specification

Update `openapi.yaml` to document:
- New `noExecuteSubmit` query parameter for both endpoints
- Updated response documentation explaining the combined operation
- Error scenarios and rollback behavior
- Retry behavior documentation

### 4.6 README Documentation

Update README.md sections:
- Trade Order API documentation
- Business rules explanation
- Example requests/responses
- Error handling and retry behavior

## Execution Plan

### Phase 1: Service Layer Implementation ✅ COMPLETED
- [x] Modify `TradeOrderService.submitTradeOrder()` method signature to include `noExecuteSubmit` parameter
- [x] Implement execution submission logic in `TradeOrderServiceImpl.submitTradeOrder()`
- [x] Implement compensating transaction rollback logic for execution service failures
- [x] Add retry logic with exponential backoff for external service calls
- [x] Configure timeout and retry parameters
- [x] Update `BatchTradeOrderService` to support the new parameter and individual failure handling

### Phase 2: Controller Layer Updates ✅ COMPLETED
- [x] Update `TradeOrderController.submitTradeOrder()` to accept `noExecuteSubmit` query parameter
- [x] Update `BatchTradeOrderController.batchSubmit()` to accept `noExecuteSubmit` query parameter
- [x] Ensure proper parameter validation and error handling
- [x] Update response handling for new error scenarios

### Phase 3: Testing ✅ COMPLETED
- [x] Write unit tests for `TradeOrderServiceImpl` with `noExecuteSubmit=false` scenario
- [x] Write unit tests for compensating transaction rollback scenarios
- [x] Write unit tests for retry logic with various failure scenarios
- [x] Write unit tests for `noExecuteSubmit=true` scenario (legacy behavior)
- [x] Write integration tests for both single and batch endpoints
- [x] Write integration tests for error scenarios and rollback behavior
- [x] Write integration tests for batch processing with mixed success/failure scenarios
- [x] Update existing tests that may be affected by the changes

### Phase 4: Documentation Updates
- [ ] Update `openapi.yaml` with new query parameter and behavior documentation
- [ ] Update README.md with new API behavior and examples
- [ ] Document retry logic and timeout configuration
- [ ] Update any API documentation or guides that reference these endpoints

### Phase 5: Backward Compatibility Validation
- [ ] Verify that existing clients continue to work with new default behavior
- [ ] Verify that `noExecuteSubmit=true` provides exact legacy behavior
- [ ] Ensure no breaking changes in response format or status codes
- [ ] Performance testing to ensure acceptable response times with retry logic