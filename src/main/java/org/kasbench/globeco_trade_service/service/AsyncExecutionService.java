package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.entity.Execution;
import org.kasbench.globeco_trade_service.repository.ExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous execution service for handling external execution service submissions
 * with retry logic and compensation handling for async failures.
 * 
 * This service implements Requirements 8.4 and 8.5 by providing:
 * - Async method for external execution service submission
 * - Retry logic with exponential backoff
 * - Compensation handling for async failures
 */
@Service
public class AsyncExecutionService {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncExecutionService.class);
    
    private final ExecutionService executionService;
    private final ExecutionRepository executionRepository;
    private final TransactionCompensationHandler compensationHandler;
    
    @Autowired
    public AsyncExecutionService(
            ExecutionService executionService,
            ExecutionRepository executionRepository,
            TransactionCompensationHandler compensationHandler) {
        this.executionService = executionService;
        this.executionRepository = executionRepository;
        this.compensationHandler = compensationHandler;
    }
    
    /**
     * Asynchronously submits an execution to the external execution service.
     * This method implements Requirements 8.4 and 8.5 by:
     * - Processing execution submission asynchronously using dedicated thread pool
     * - Implementing retry logic with exponential backoff via the underlying ExecutionService
     * - Handling compensation for async failures
     * 
     * @param executionId The ID of the execution to submit
     * @return CompletableFuture containing the submission result
     */
    @Async("executionSubmissionExecutor")
    public CompletableFuture<ExecutionService.SubmitResult> submitExecutionAsync(Integer executionId) {
        logger.info("Starting async execution submission for execution ID: {}", executionId);
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // Capture original execution state for potential compensation
                Execution originalExecution = captureOriginalExecutionState(executionId);
                if (originalExecution == null) {
                    logger.error("Execution not found for async submission: {}", executionId);
                    return new ExecutionService.SubmitResult(null, "Execution not found: " + executionId);
                }
                
                // Capture original trade order state for compensation
                TransactionCompensationHandler.TradeOrderState originalTradeOrderState = 
                    captureOriginalTradeOrderState(originalExecution);
                
                logger.debug("Captured original state for execution {} and trade order {} before async submission", 
                        executionId, originalTradeOrderState.getTradeOrderId());
                
                // Attempt submission using the existing ExecutionService with retry logic
                ExecutionService.SubmitResult result = executionService.submitExecution(executionId);
                
                long duration = System.currentTimeMillis() - startTime;
                
                if (result.getStatus() != null && "submitted".equals(result.getStatus())) {
                    logger.info("Successfully completed async execution submission for execution {} in {} ms", 
                            executionId, duration);
                    return result;
                } else {
                    logger.warn("Async execution submission failed for execution {}: {}", 
                            executionId, result.getError());
                    
                    // Handle compensation for failed submission
                    handleAsyncSubmissionFailure(originalExecution, originalTradeOrderState, result.getError());
                    
                    return result;
                }
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                logger.error("Exception during async execution submission for execution {} after {} ms: {}", 
                        executionId, duration, e.getMessage(), e);
                
                // Handle compensation for exception during submission
                try {
                    Execution originalExecution = captureOriginalExecutionState(executionId);
                    if (originalExecution != null) {
                        TransactionCompensationHandler.TradeOrderState originalTradeOrderState = 
                            captureOriginalTradeOrderState(originalExecution);
                        handleAsyncSubmissionFailure(originalExecution, originalTradeOrderState, e.getMessage());
                    }
                } catch (Exception compensationException) {
                    logger.error("Failed to handle compensation for async submission exception: {}", 
                            compensationException.getMessage(), compensationException);
                }
                
                throw new AsyncExecutionException("Failed to submit execution asynchronously: " + executionId, e);
            }
        });
    }
    
    /**
     * Submits multiple executions asynchronously in parallel.
     * This method provides batch processing capabilities for improved throughput.
     * 
     * @param executionIds Array of execution IDs to submit
     * @return CompletableFuture containing array of submission results
     */
    @Async("executionSubmissionExecutor")
    public CompletableFuture<ExecutionService.SubmitResult[]> submitExecutionsAsync(Integer... executionIds) {
        logger.info("Starting batch async execution submission for {} executions", executionIds.length);
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // Create individual async submissions
                CompletableFuture<ExecutionService.SubmitResult>[] futures = new CompletableFuture[executionIds.length];
                
                for (int i = 0; i < executionIds.length; i++) {
                    final Integer executionId = executionIds[i];
                    futures[i] = submitExecutionAsync(executionId);
                }
                
                // Wait for all submissions to complete
                CompletableFuture<Void> allOf = CompletableFuture.allOf(futures);
                allOf.join();
                
                // Collect results
                ExecutionService.SubmitResult[] results = new ExecutionService.SubmitResult[executionIds.length];
                for (int i = 0; i < futures.length; i++) {
                    try {
                        results[i] = futures[i].get();
                    } catch (Exception e) {
                        logger.error("Failed to get result for execution {}: {}", executionIds[i], e.getMessage());
                        results[i] = new ExecutionService.SubmitResult(null, "Failed to get async result: " + e.getMessage());
                    }
                }
                
                long duration = System.currentTimeMillis() - startTime;
                logger.info("Completed batch async execution submission for {} executions in {} ms", 
                        executionIds.length, duration);
                
                return results;
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                logger.error("Exception during batch async execution submission after {} ms: {}", 
                        duration, e.getMessage(), e);
                throw new AsyncExecutionException("Failed to submit executions asynchronously", e);
            }
        });
    }
    
    /**
     * Captures the original execution state for potential compensation.
     * 
     * @param executionId The execution ID
     * @return The original execution state, or null if not found
     */
    private Execution captureOriginalExecutionState(Integer executionId) {
        try {
            return executionRepository.findById(executionId).orElse(null);
        } catch (Exception e) {
            logger.error("Failed to capture original execution state for execution {}: {}", 
                    executionId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Captures the original trade order state for potential compensation.
     * 
     * @param execution The execution containing the trade order
     * @return The original trade order state
     */
    private TransactionCompensationHandler.TradeOrderState captureOriginalTradeOrderState(Execution execution) {
        if (execution == null || execution.getTradeOrder() == null) {
            logger.warn("Cannot capture trade order state - execution or trade order is null");
            return new TransactionCompensationHandler.TradeOrderState(null, null, null);
        }
        
        return new TransactionCompensationHandler.TradeOrderState(
                execution.getTradeOrder().getId(),
                execution.getTradeOrder().getQuantitySent(),
                execution.getTradeOrder().getSubmitted()
        );
    }
    
    /**
     * Handles compensation for failed async submission.
     * This method triggers the compensation handler to rollback changes
     * made during the failed submission attempt.
     * 
     * @param originalExecution The original execution state
     * @param originalTradeOrderState The original trade order state
     * @param errorMessage The error message from the failed submission
     */
    private void handleAsyncSubmissionFailure(Execution originalExecution, 
            TransactionCompensationHandler.TradeOrderState originalTradeOrderState, String errorMessage) {
        
        if (originalExecution == null || originalTradeOrderState == null) {
            logger.error("Cannot handle async submission failure - original state is null");
            return;
        }
        
        logger.info("Handling async submission failure for execution {} and trade order {}: {}", 
                originalExecution.getId(), originalTradeOrderState.getTradeOrderId(), errorMessage);
        
        try {
            // Trigger async compensation
            CompletableFuture<Void> compensationFuture = compensationHandler.compensateFailedSubmission(
                    originalExecution, originalTradeOrderState);
            
            // Log compensation initiation (don't wait for completion to avoid blocking)
            compensationFuture.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.error("Async compensation failed for execution {} and trade order {}: {}", 
                            originalExecution.getId(), originalTradeOrderState.getTradeOrderId(), 
                            throwable.getMessage(), throwable);
                } else {
                    logger.info("Async compensation completed successfully for execution {} and trade order {}", 
                            originalExecution.getId(), originalTradeOrderState.getTradeOrderId());
                }
            });
            
        } catch (Exception e) {
            logger.error("Failed to initiate async compensation for execution {} and trade order {}: {}", 
                    originalExecution.getId(), originalTradeOrderState.getTradeOrderId(), e.getMessage(), e);
        }
    }
    
    /**
     * Exception thrown when async execution operations fail.
     */
    public static class AsyncExecutionException extends RuntimeException {
        public AsyncExecutionException(String message) {
            super(message);
        }
        
        public AsyncExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}