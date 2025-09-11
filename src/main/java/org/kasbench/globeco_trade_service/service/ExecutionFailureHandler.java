package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.client.ExecutionServiceClient;
import org.kasbench.globeco_trade_service.config.ExecutionBatchProperties;
import org.kasbench.globeco_trade_service.dto.ExecutionServiceBatchRequestDTO;
import org.kasbench.globeco_trade_service.dto.BatchExecutionResponseDTO;
import org.kasbench.globeco_trade_service.entity.Execution;
import org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.BulkSubmitResult;
import org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.ExecutionSubmitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Component responsible for handling execution failures and implementing retry logic.
 * Processes partial failures from batch submissions and retries failed executions
 * individually or in smaller batches using exponential backoff strategy.
 */
@Component
public class ExecutionFailureHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ExecutionFailureHandler.class);
    
    private final ExecutionBatchProcessor batchProcessor;
    private final ExecutionServiceClient executionServiceClient;
    private final ExecutionBatchProperties batchProperties;
    private final RetryTemplate retryTemplate;
    private final BulkExecutionErrorHandler errorHandler;
    
    // Track retry attempts per execution to prevent infinite retries
    private final Map<Integer, Integer> retryAttempts = new ConcurrentHashMap<>();
    
    @Autowired
    public ExecutionFailureHandler(
            ExecutionBatchProcessor batchProcessor,
            ExecutionServiceClient executionServiceClient,
            ExecutionBatchProperties batchProperties,
            @Qualifier("executionServiceRetryTemplate") RetryTemplate retryTemplate,
            BulkExecutionErrorHandler errorHandler) {
        this.batchProcessor = batchProcessor;
        this.executionServiceClient = executionServiceClient;
        this.batchProperties = batchProperties;
        this.retryTemplate = retryTemplate;
        this.errorHandler = errorHandler;
    }
    
    /**
     * Processes partial failures from a batch submission and retries failed executions.
     * Extracts failed executions and attempts to retry them individually or in smaller batches.
     * 
     * @param result The bulk submit result containing partial failures
     * @param originalExecutions The original executions that were submitted
     * @return Updated BulkSubmitResult with retry results incorporated
     */
    public BulkSubmitResult handlePartialFailures(BulkSubmitResult result, List<Execution> originalExecutions) {
        if (result == null || originalExecutions == null) {
            throw new IllegalArgumentException("Result and original executions cannot be null");
        }
        
        if (result.getFailed() == 0) {
            logger.debug("No failures to handle in batch result");
            return result;
        }
        
        logger.info("Handling partial failures: {} failed out of {} total executions", 
                   result.getFailed(), result.getTotalRequested());
        
        // Extract failed executions that are suitable for retry
        List<Execution> failedExecutions = extractRetryableFailures(result, originalExecutions);
        
        if (failedExecutions.isEmpty()) {
            logger.info("No retryable failures found, returning original result");
            return result;
        }
        
        logger.info("Found {} retryable failures, attempting retry", failedExecutions.size());
        
        // Retry failed executions
        BulkSubmitResult retryResult = retryFailedExecutions(failedExecutions);
        
        // Merge original result with retry results
        return mergeResults(result, retryResult, originalExecutions);
    }
    
    /**
     * Retries failed executions individually or in smaller batches.
     * Uses exponential backoff and respects retry limits from configuration.
     * 
     * @param failedExecutions List of executions that failed and should be retried
     * @return BulkSubmitResult containing retry attempt results
     */
    public BulkSubmitResult retryFailedExecutions(List<Execution> failedExecutions) {
        if (failedExecutions == null || failedExecutions.isEmpty()) {
            return new BulkSubmitResult(0, 0, 0, new ArrayList<>(), "SUCCESS", "No executions to retry");
        }
        
        logger.info("Retrying {} failed executions", failedExecutions.size());
        
        List<ExecutionSubmitResult> allRetryResults = new ArrayList<>();
        int totalRetrySuccessful = 0;
        int totalRetryFailed = 0;
        
        // Filter executions that haven't exceeded retry limits
        List<Execution> retryableExecutions = filterRetryableExecutions(failedExecutions);
        
        if (retryableExecutions.isEmpty()) {
            logger.info("All failed executions have exceeded retry limits");
            return createRetryExhaustedResult(failedExecutions);
        }
        
        // Determine retry strategy: individual vs small batches
        if (retryableExecutions.size() == 1 || batchProperties.getRetryFailedIndividually() > 0) {
            // Retry individually for better isolation
            for (Execution execution : retryableExecutions) {
                ExecutionSubmitResult retryResult = retryExecutionIndividually(execution);
                allRetryResults.add(retryResult);
                
                if ("SUCCESS".equals(retryResult.getStatus()) || "COMPLETED".equals(retryResult.getStatus())) {
                    totalRetrySuccessful++;
                } else {
                    totalRetryFailed++;
                }
            }
        } else {
            // Retry in smaller batches (max 10 executions per retry batch)
            int retryBatchSize = Math.min(10, retryableExecutions.size());
            List<List<Execution>> retryBatches = splitIntoRetryBatches(retryableExecutions, retryBatchSize);
            
            for (List<Execution> retryBatch : retryBatches) {
                BulkSubmitResult batchRetryResult = retryBatch(retryBatch);
                allRetryResults.addAll(batchRetryResult.getResults());
                totalRetrySuccessful += batchRetryResult.getSuccessful();
                totalRetryFailed += batchRetryResult.getFailed();
            }
        }
        
        // Add retry exhausted results for executions that exceeded limits
        List<Execution> exhaustedExecutions = failedExecutions.stream()
            .filter(exec -> !retryableExecutions.contains(exec))
            .collect(Collectors.toList());
        
        for (Execution execution : exhaustedExecutions) {
            allRetryResults.add(new ExecutionSubmitResult(
                execution.getId(),
                "RETRY_EXHAUSTED",
                "Maximum retry attempts exceeded",
                null
            ));
            totalRetryFailed++;
        }
        
        String overallStatus = determineOverallStatus(totalRetrySuccessful, totalRetryFailed, failedExecutions.size());
        String message = String.format("Retry completed: %d successful, %d failed, %d retry exhausted", 
                                      totalRetrySuccessful, totalRetryFailed - exhaustedExecutions.size(), 
                                      exhaustedExecutions.size());
        
        logger.info("Retry process completed: {} successful, {} failed out of {} retried executions", 
                   totalRetrySuccessful, totalRetryFailed, failedExecutions.size());
        
        return new BulkSubmitResult(failedExecutions.size(), totalRetrySuccessful, totalRetryFailed, 
                                   allRetryResults, overallStatus, message);
    }
    
    /**
     * Retries a single execution individually with exponential backoff.
     * 
     * @param execution The execution to retry
     * @return ExecutionSubmitResult containing the retry result
     */
    public ExecutionSubmitResult retryExecutionIndividually(Execution execution) {
        if (execution == null) {
            throw new IllegalArgumentException("Execution cannot be null");
        }
        
        Integer executionId = execution.getId();
        int currentAttempts = retryAttempts.getOrDefault(executionId, 0);
        
        // Check if max retries exceeded before attempting
        if (currentAttempts >= batchProperties.getRetryFailedIndividually()) {
            logger.debug("Execution {} has exceeded max retry attempts ({}/{})", 
                        executionId, currentAttempts, batchProperties.getRetryFailedIndividually());
            retryAttempts.remove(executionId); // Clear counter
            return new ExecutionSubmitResult(executionId, "RETRY_EXHAUSTED", 
                                           "Maximum retry attempts exceeded", null);
        }
        
        logger.debug("Retrying execution {} individually (attempt {} of {})", 
                    executionId, currentAttempts + 1, batchProperties.getRetryFailedIndividually());
        
        try {
            // Increment retry counter
            retryAttempts.put(executionId, currentAttempts + 1);
            
            // Use retry template for exponential backoff
            BatchExecutionResponseDTO response = retryTemplate.execute(context -> {
                logger.debug("Executing retry attempt {} for execution {}", 
                           context.getRetryCount() + 1, executionId);
                
                ExecutionServiceBatchRequestDTO request = batchProcessor.buildBatchRequest(List.of(execution));
                return executionServiceClient.submitBatch(request);
            });
            
            // Process the response
            BulkSubmitResult result = batchProcessor.processResponse(response, List.of(execution));
            
            if (!result.getResults().isEmpty()) {
                ExecutionSubmitResult submitResult = result.getResults().get(0);
                
                if ("SUCCESS".equals(submitResult.getStatus()) || "COMPLETED".equals(submitResult.getStatus())) {
                    logger.info("Execution {} retry succeeded after {} attempts", executionId, currentAttempts + 1);
                    // Clear retry counter on success
                    retryAttempts.remove(executionId);
                    return submitResult;
                } else {
                    logger.warn("Execution {} retry failed: {}", executionId, submitResult.getMessage());
                    return submitResult;
                }
            } else {
                logger.warn("Execution {} retry returned empty results", executionId);
                return new ExecutionSubmitResult(executionId, "FAILED", "Empty retry response", null);
            }
            
        } catch (Exception ex) {
            // Map exception to detailed error information
            List<Integer> executionIdList = List.of(executionId);
            Map<String, Object> executionContext = errorHandler.createExecutionContext(executionIdList, 1, currentAttempts + 1);
            BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(ex, executionContext);
            
            // Log detailed error information
            errorHandler.logError(errorInfo, executionIdList, 1);
            
            logger.error("Execution {} retry failed with exception after {} attempts: [{}] {}", 
                        executionId, currentAttempts + 1, errorInfo.getErrorCode(), errorInfo.getMessage(), ex);
            
            // Determine if this is a permanent failure or should be retried again
            boolean shouldRetry = errorHandler.shouldRetry(errorInfo, currentAttempts + 1, batchProperties.getRetryFailedIndividually());
            
            if (!shouldRetry || currentAttempts + 1 >= batchProperties.getRetryFailedIndividually()) {
                retryAttempts.remove(executionId); // Clear counter for permanent failures
                return new ExecutionSubmitResult(executionId, "RETRY_EXHAUSTED", 
                                               String.format("[%s] %s", errorInfo.getErrorCode(), errorInfo.getMessage()), null);
            } else {
                return new ExecutionSubmitResult(executionId, "FAILED", 
                                               String.format("[%s] %s", errorInfo.getErrorCode(), errorInfo.getMessage()), null);
            }
        }
    }
    
    /**
     * Extracts executions from a bulk result that failed but are suitable for retry.
     * Filters out permanent failures that should not be retried.
     * 
     * @param result The bulk submit result containing failures
     * @param originalExecutions The original executions that were submitted
     * @return List of executions that should be retried
     */
    private List<Execution> extractRetryableFailures(BulkSubmitResult result, List<Execution> originalExecutions) {
        List<Execution> retryableFailures = new ArrayList<>();
        
        if (result.getResults() == null || result.getResults().isEmpty()) {
            return retryableFailures;
        }
        
        Map<Integer, Execution> executionMap = originalExecutions.stream()
            .collect(Collectors.toMap(Execution::getId, execution -> execution));
        
        for (ExecutionSubmitResult submitResult : result.getResults()) {
            if (isRetryableFailure(submitResult)) {
                Execution execution = executionMap.get(submitResult.getExecutionId());
                if (execution != null) {
                    retryableFailures.add(execution);
                }
            }
        }
        
        logger.debug("Extracted {} retryable failures from {} total results", 
                    retryableFailures.size(), result.getResults().size());
        
        return retryableFailures;
    }
    
    /**
     * Filters executions that haven't exceeded their retry limits.
     * 
     * @param executions List of executions to filter
     * @return List of executions that can still be retried
     */
    private List<Execution> filterRetryableExecutions(List<Execution> executions) {
        int maxRetries = batchProperties.getRetryFailedIndividually();
        
        return executions.stream()
            .filter(execution -> {
                int attempts = retryAttempts.getOrDefault(execution.getId(), 0);
                return attempts < maxRetries;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Retries a batch of executions together.
     * 
     * @param executions List of executions to retry as a batch
     * @return BulkSubmitResult containing the batch retry results
     */
    private BulkSubmitResult retryBatch(List<Execution> executions) {
        logger.debug("Retrying batch of {} executions", executions.size());
        
        try {
            // Update retry counters for all executions in the batch
            for (Execution execution : executions) {
                int currentAttempts = retryAttempts.getOrDefault(execution.getId(), 0);
                retryAttempts.put(execution.getId(), currentAttempts + 1);
            }
            
            // Use retry template for the batch
            BatchExecutionResponseDTO response = retryTemplate.execute(context -> {
                logger.debug("Executing batch retry attempt {}", context.getRetryCount() + 1);
                
                ExecutionServiceBatchRequestDTO request = batchProcessor.buildBatchRequest(executions);
                return executionServiceClient.submitBatch(request);
            });
            
            // Process the response
            BulkSubmitResult result = batchProcessor.processResponse(response, executions);
            
            // Clear retry counters for successful executions
            for (ExecutionSubmitResult submitResult : result.getResults()) {
                if ("SUCCESS".equals(submitResult.getStatus()) || "COMPLETED".equals(submitResult.getStatus())) {
                    retryAttempts.remove(submitResult.getExecutionId());
                }
            }
            
            logger.debug("Batch retry completed: {} successful, {} failed", 
                        result.getSuccessful(), result.getFailed());
            
            return result;
            
        } catch (Exception ex) {
            // Map exception to detailed error information
            List<Integer> executionIdList = executions.stream().map(Execution::getId).collect(Collectors.toList());
            Map<String, Object> executionContext = errorHandler.createExecutionContext(executionIdList, executions.size(), 1);
            BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(ex, executionContext);
            
            // Log detailed error information
            errorHandler.logError(errorInfo, executionIdList, executions.size());
            
            logger.error("Batch retry failed: [{}] {}", errorInfo.getErrorCode(), errorInfo.getMessage(), ex);
            
            // Determine retry status based on error analysis
            boolean shouldRetry = errorHandler.shouldRetry(errorInfo, 1, batchProperties.getRetryFailedIndividually());
            String status = shouldRetry ? "FAILED" : "RETRY_EXHAUSTED";
            String message = String.format("[%s] %s", errorInfo.getErrorCode(), errorInfo.getMessage());
            
            // Create failure results for all executions in the batch
            List<ExecutionSubmitResult> failureResults = executions.stream()
                .map(execution -> new ExecutionSubmitResult(
                    execution.getId(),
                    status,
                    message,
                    null
                ))
                .collect(Collectors.toList());
            
            return new BulkSubmitResult(executions.size(), 0, executions.size(), 
                                       failureResults, "FAILED", message);
        }
    }
    
    /**
     * Splits executions into smaller retry batches.
     * 
     * @param executions List of executions to split
     * @param batchSize Size of each retry batch
     * @return List of execution batches
     */
    private List<List<Execution>> splitIntoRetryBatches(List<Execution> executions, int batchSize) {
        List<List<Execution>> batches = new ArrayList<>();
        
        for (int i = 0; i < executions.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, executions.size());
            List<Execution> batch = new ArrayList<>(executions.subList(i, endIndex));
            batches.add(batch);
        }
        
        return batches;
    }
    
    /**
     * Creates a result for executions that have exhausted their retry attempts.
     * 
     * @param executions List of executions that exhausted retries
     * @return BulkSubmitResult indicating retry exhaustion
     */
    private BulkSubmitResult createRetryExhaustedResult(List<Execution> executions) {
        List<ExecutionSubmitResult> results = executions.stream()
            .map(execution -> new ExecutionSubmitResult(
                execution.getId(),
                "RETRY_EXHAUSTED",
                "Maximum retry attempts exceeded",
                null
            ))
            .collect(Collectors.toList());
        
        return new BulkSubmitResult(executions.size(), 0, executions.size(), 
                                   results, "FAILED", "All executions exhausted retry attempts");
    }
    
    /**
     * Merges original batch results with retry results.
     * 
     * @param originalResult The original batch submission result
     * @param retryResult The retry attempt results
     * @param originalExecutions The original executions that were submitted
     * @return Merged BulkSubmitResult
     */
    private BulkSubmitResult mergeResults(BulkSubmitResult originalResult, BulkSubmitResult retryResult, 
                                         List<Execution> originalExecutions) {
        
        // Create a map of retry results by execution ID
        Map<Integer, ExecutionSubmitResult> retryResultMap = new HashMap<>();
        for (ExecutionSubmitResult result : retryResult.getResults()) {
            retryResultMap.put(result.getExecutionId(), result);
        }
        
        // Merge results, preferring retry results for executions that were retried
        List<ExecutionSubmitResult> mergedResults = new ArrayList<>();
        int mergedSuccessful = 0;
        int mergedFailed = 0;
        
        for (ExecutionSubmitResult originalSubmitResult : originalResult.getResults()) {
            Integer executionId = originalSubmitResult.getExecutionId();
            
            if (retryResultMap.containsKey(executionId)) {
                // Use retry result
                ExecutionSubmitResult retrySubmitResult = retryResultMap.get(executionId);
                mergedResults.add(retrySubmitResult);
                
                if ("SUCCESS".equals(retrySubmitResult.getStatus()) || "COMPLETED".equals(retrySubmitResult.getStatus())) {
                    mergedSuccessful++;
                } else {
                    mergedFailed++;
                }
            } else {
                // Use original result
                mergedResults.add(originalSubmitResult);
                
                if ("SUCCESS".equals(originalSubmitResult.getStatus()) || "COMPLETED".equals(originalSubmitResult.getStatus())) {
                    mergedSuccessful++;
                } else {
                    mergedFailed++;
                }
            }
        }
        
        String overallStatus = determineOverallStatus(mergedSuccessful, mergedFailed, originalExecutions.size());
        String message = String.format("Batch with retries: %d successful, %d failed (including %d retries)", 
                                      mergedSuccessful, mergedFailed, retryResult.getResults().size());
        
        return new BulkSubmitResult(originalExecutions.size(), mergedSuccessful, mergedFailed, 
                                   mergedResults, overallStatus, message);
    }
    
    /**
     * Determines if a failure is retryable based on the status and message.
     * 
     * @param result The execution submit result to check
     * @return true if the failure should be retried, false otherwise
     */
    private boolean isRetryableFailure(ExecutionSubmitResult result) {
        if (!"FAILED".equals(result.getStatus())) {
            return false;
        }
        
        String message = result.getMessage();
        if (message == null) {
            return true; // Assume retryable if no specific message
        }
        
        String lowerMessage = message.toLowerCase();
        
        // Non-retryable failures (permanent errors)
        if (lowerMessage.contains("validation") || 
            lowerMessage.contains("invalid") || 
            lowerMessage.contains("not found") ||
            lowerMessage.contains("duplicate") ||
            lowerMessage.contains("unauthorized") ||
            lowerMessage.contains("forbidden") ||
            lowerMessage.contains("bad request")) {
            return false;
        }
        
        // Retryable failures (transient errors)
        return lowerMessage.contains("timeout") || 
               lowerMessage.contains("connection") || 
               lowerMessage.contains("service unavailable") ||
               lowerMessage.contains("internal server error") ||
               lowerMessage.contains("temporary") ||
               lowerMessage.contains("retry");
    }
    

    
    /**
     * Determines the overall status based on success/failure counts.
     * 
     * @param successful Number of successful executions
     * @param failed Number of failed executions
     * @param total Total number of executions
     * @return Overall status string
     */
    private String determineOverallStatus(int successful, int failed, int total) {
        if (failed == 0) {
            return "SUCCESS";
        } else if (successful == 0) {
            return "FAILED";
        } else {
            return "PARTIAL_SUCCESS";
        }
    }
    
    /**
     * Clears retry attempt counters for completed executions.
     * Should be called periodically to prevent memory leaks.
     * 
     * @param executionIds List of execution IDs to clear from retry tracking
     */
    public void clearRetryCounters(List<Integer> executionIds) {
        if (executionIds != null) {
            for (Integer executionId : executionIds) {
                retryAttempts.remove(executionId);
            }
            logger.debug("Cleared retry counters for {} executions", executionIds.size());
        }
    }
    
    /**
     * Gets the current retry attempt count for an execution.
     * 
     * @param executionId The execution ID to check
     * @return Number of retry attempts made for this execution
     */
    public int getRetryAttempts(Integer executionId) {
        return retryAttempts.getOrDefault(executionId, 0);
    }
}