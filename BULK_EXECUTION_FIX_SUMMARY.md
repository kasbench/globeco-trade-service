# Bulk Execution Submission Fix

## Problem
The batch trade order submission endpoint `/api/v1/tradeOrders/batch/submit` was not properly using bulk execution submission. Instead, it was submitting each execution individually, resulting in logs showing:

```
Starting bulk execution submission for 1 execution IDs
Split 1 executions into 1 batches
```

This happened even when submitting multiple trade orders in a single batch request.

## Root Cause
The issue was in the `BatchTradeOrderService.submitTradeOrdersBatch()` method. The original implementation was:

1. For each trade order in the batch:
   - Call `tradeOrderService.submitTradeOrder(tradeOrderId, dto, noExecuteSubmit)`
   - This method would create an execution AND immediately submit it to the external service individually

This resulted in N individual execution submissions instead of 1 bulk submission for N executions.

## Solution
Modified `BatchTradeOrderService.submitTradeOrdersBatch()` to use a two-phase approach:

### Phase 1: Create All Executions Locally
```java
// Step 1: Create all executions locally (without submitting to external service)
for (int i = 0; i < request.getSubmissions().size(); i++) {
    // Create execution locally (with noExecuteSubmit=true to skip external submission)
    BatchSubmitResponseDTO.TradeOrderSubmitResultDTO result = 
        processTradeOrderSubmission(submission, requestIndex, true); // Always skip external submission initially
    
    if (result is successful) {
        executionIds.add(result.getExecution().getId());
    }
}
```

### Phase 2: Bulk Submit All Executions
```java
// Step 2: If noExecuteSubmit is false and we have executions to submit, use bulk submission
if (!noExecuteSubmit && !executionIds.isEmpty()) {
    // Use the bulk execution submission service
    ExecutionService.BulkSubmitResult bulkResult = executionService.submitExecutions(executionIds);
    
    // Update results based on bulk submission outcome
    updateResultsFromBulkSubmission(results, bulkResult, executionToRequestIndex);
}
```

## Key Changes

### 1. Modified BatchTradeOrderService Constructor
Added `ExecutionService` dependency:
```java
public BatchTradeOrderService(
        TradeOrderRepository tradeOrderRepository,
        TradeOrderService tradeOrderService,
        ExecutionRepository executionRepository,
        ExecutionService executionService) { // Added this
    // ...
}
```

### 2. Two-Phase Execution Processing
- **Phase 1**: Create all executions with `noExecuteSubmit=true`
- **Phase 2**: Submit all execution IDs in a single bulk call

### 3. Added Helper Method
```java
private void updateResultsFromBulkSubmission(
        List<BatchSubmitResponseDTO.TradeOrderSubmitResultDTO> results,
        ExecutionService.BulkSubmitResult bulkResult,
        Map<Integer, Integer> executionToRequestIndex)
```

## Expected Behavior After Fix

### Before Fix (Individual Submissions)
```
Starting bulk execution submission for 1 execution IDs
Split 1 executions into 1 batches
Starting bulk execution submission for 1 execution IDs  
Split 1 executions into 1 batches
```

### After Fix (True Bulk Submission)
```
Starting bulk execution submission for 25 execution IDs
Split 25 executions into 1 batches
```

## Configuration
The bulk execution submission respects the existing configuration:
- `execution.service.batch.enable-batching=true` (enables bulk processing)
- `execution.service.batch.size=50` (default batch size)
- `execution.service.batch.max-size=100` (maximum batch size)

## Backward Compatibility
- The fix maintains full backward compatibility
- The `noExecuteSubmit` parameter still works as expected
- All existing API contracts are preserved
- Error handling and response formats remain unchanged

## Testing
To verify the fix works:
1. Submit a batch of trade orders using `/api/v1/tradeOrders/batch/submit`
2. Check the logs for bulk execution submission messages
3. Should see a single bulk submission with multiple execution IDs instead of multiple individual submissions

## Files Modified
- `src/main/java/org/kasbench/globeco_trade_service/service/BatchTradeOrderService.java`
  - Added ExecutionService dependency
  - Implemented two-phase execution processing
  - Added updateResultsFromBulkSubmission helper method