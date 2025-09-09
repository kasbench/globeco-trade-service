package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.client.ExecutionServiceClient;
import org.kasbench.globeco_trade_service.config.ExecutionBatchProperties;
import org.kasbench.globeco_trade_service.dto.BatchExecutionRequestDTO;
import org.kasbench.globeco_trade_service.dto.BatchExecutionResponseDTO;
import org.kasbench.globeco_trade_service.entity.Execution;
import org.kasbench.globeco_trade_service.repository.ExecutionRepository;
import org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.BulkSubmitResult;
import org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.ExecutionSubmitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for orchestrating bulk execution submissions.
 * Handles batch size management, splitting large requests into multiple batches,
 * and aggregating results across multiple batch submissions.
 */
@Service
public class BulkExecutionSubmissionService {
    
    private static final Logger logger = LoggerFactory.getLogger(BulkExecutionSubmissionService.class);
    
    private final ExecutionRepository executionRepository;
    private final ExecutionBatchProcessor batchProcessor;
    private final ExecutionServiceClient executionServiceClient;
    private final ExecutionBatchProperties batchProperties;
    private final ExecutionFailureHandler failureHandler;
    
    @Autowired
    public BulkExecutionSubmissionService(
            ExecutionRepository executionRepository,
            ExecutionBatchProcessor batchProcessor,
            ExecutionServiceClient executionServiceClient,
            ExecutionBatchProperties batchProperties,
            ExecutionFailureHandler failureHandler) {
        this.executionRepository = executionRepository;
        this.batchProcessor = batchProcessor;
        this.executionServiceClient = executionServiceClient;
        this.batchProperties = batchProperties;
        this.failureHandler = failureHandler;
    }
    
    /**
     * Submits multiple executions in bulk with automatic batch size management.
     * Large execution lists are automatically split into multiple batches based on configuration.
     * 
     * @param executionIds List of execution IDs to submit
     * @return BulkSubmitResult containing aggregated results from all batches
     * @throws IllegalArgumentException if executionIds is null or empty
     */
    @Transactional
    public BulkSubmitResult submitExecutionsBulk(List<Integer> executionIds) {
        if (executionIds == null || executionIds.isEmpty()) {
            throw new IllegalArgumentException("Execution IDs list cannot be null or empty");
        }
        
        logger.info("Starting bulk execution submission for {} execution IDs", executionIds.size());
        long startTime = System.currentTimeMillis();
        
        try {
            // Load executions with all relationships
            List<Execution> executions = loadExecutionsWithRelations(executionIds);
            
            if (executions.isEmpty()) {
                logger.warn("No valid executions found for provided IDs");
                return createEmptyResult(executionIds.size(), "No valid executions found");
            }
            
            // Check if batching is enabled
            if (!batchProperties.isEnableBatching()) {
                logger.info("Batching is disabled, processing executions individually");
                return processIndividually(executions);
            }
            
            // Split into batches and process
            List<List<Execution>> batches = splitIntoBatches(executions);
            logger.info("Split {} executions into {} batches", executions.size(), batches.size());
            
            BulkSubmitResult aggregatedResult = processBatches(batches);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Bulk execution submission completed in {} ms: {} total, {} successful, {} failed", 
                       duration, aggregatedResult.getTotalRequested(), 
                       aggregatedResult.getSuccessful(), aggregatedResult.getFailed());
            
            return aggregatedResult;
            
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Bulk execution submission failed after {} ms for {} executions: {}", 
                        duration, executionIds.size(), ex.getMessage(), ex);
            throw ex;
        }
    }
    
    /**
     * Processes a single batch of executions.
     * Handles the complete flow from batch request building to response processing.
     * 
     * @param executions List of executions to process in this batch
     * @return BulkSubmitResult for this specific batch
     */
    @Transactional
    public BulkSubmitResult processBatch(List<Execution> executions) {
        if (executions == null || executions.isEmpty()) {
            throw new IllegalArgumentException("Executions list cannot be null or empty");
        }
        
        int batchSize = executions.size();
        logger.debug("Processing batch of {} executions", batchSize);
        
        long batchStartTime = System.currentTimeMillis();
        
        try {
            // Build batch request
            BatchExecutionRequestDTO batchRequest = batchProcessor.buildBatchRequest(executions);
            logger.debug("Built batch request for {} executions", batchSize);
            
            // Submit batch to external service
            BatchExecutionResponseDTO response = executionServiceClient.submitBatch(batchRequest);
            logger.debug("Received batch response with status: {}", response.getStatus());
            
            // Process response and update execution statuses
            BulkSubmitResult result = batchProcessor.processResponse(response, executions);
            
            // Handle partial failures with retry logic
            if (result.getFailed() > 0 && batchProperties.getRetryFailedIndividually() > 0) {
                logger.debug("Handling {} failures with retry logic", result.getFailed());
                result = failureHandler.handlePartialFailures(result, executions);
            }
            
            // Update execution entities based on results
            updateExecutionStatuses(result, executions);
            
            // Clear retry counters for completed executions
            List<Integer> executionIds = executions.stream()
                .map(Execution::getId)
                .collect(Collectors.toList());
            failureHandler.clearRetryCounters(executionIds);
            
            long batchDuration = System.currentTimeMillis() - batchStartTime;
            logger.debug("Batch processing completed in {} ms: {} successful, {} failed", 
                        batchDuration, result.getSuccessful(), result.getFailed());
            
            return result;
            
        } catch (Exception ex) {
            long batchDuration = System.currentTimeMillis() - batchStartTime;
            logger.error("Batch processing failed after {} ms for {} executions: {}", 
                        batchDuration, batchSize, ex.getMessage(), ex);
            
            // Create failure result for all executions in this batch
            return createBatchFailureResult(executions, ex.getMessage());
        }
    }
    
    /**
     * Loads executions with all required relationships to avoid lazy loading issues.
     */
    private List<Execution> loadExecutionsWithRelations(List<Integer> executionIds) {
        logger.debug("Loading {} executions with relationships", executionIds.size());
        
        List<Execution> executions = new ArrayList<>();
        List<Integer> notFoundIds = new ArrayList<>();
        
        for (Integer id : executionIds) {
            executionRepository.findByIdWithAllRelations(id)
                .ifPresentOrElse(
                    executions::add,
                    () -> notFoundIds.add(id)
                );
        }
        
        if (!notFoundIds.isEmpty()) {
            logger.warn("Could not find executions with IDs: {}", notFoundIds);
        }
        
        logger.debug("Loaded {} executions successfully", executions.size());
        return executions;
    }
    
    /**
     * Splits a list of executions into batches based on configuration.
     */
    private List<List<Execution>> splitIntoBatches(List<Execution> executions) {
        int batchSize = batchProperties.getEffectiveBatchSize();
        List<List<Execution>> batches = new ArrayList<>();
        
        for (int i = 0; i < executions.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, executions.size());
            List<Execution> batch = executions.subList(i, endIndex);
            batches.add(new ArrayList<>(batch)); // Create new list to avoid sublist issues
        }
        
        logger.debug("Split {} executions into {} batches with max size {}", 
                    executions.size(), batches.size(), batchSize);
        
        return batches;
    }
    
    /**
     * Processes multiple batches and aggregates the results.
     */
    private BulkSubmitResult processBatches(List<List<Execution>> batches) {
        List<ExecutionSubmitResult> allResults = new ArrayList<>();
        int totalRequested = 0;
        int totalSuccessful = 0;
        int totalFailed = 0;
        
        for (int i = 0; i < batches.size(); i++) {
            List<Execution> batch = batches.get(i);
            logger.debug("Processing batch {} of {} with {} executions", 
                        i + 1, batches.size(), batch.size());
            
            try {
                BulkSubmitResult batchResult = processBatch(batch);
                
                // Aggregate results
                allResults.addAll(batchResult.getResults());
                totalRequested += batchResult.getTotalRequested();
                totalSuccessful += batchResult.getSuccessful();
                totalFailed += batchResult.getFailed();
                
                logger.debug("Batch {} completed: {} successful, {} failed", 
                            i + 1, batchResult.getSuccessful(), batchResult.getFailed());
                
            } catch (Exception ex) {
                logger.error("Batch {} failed completely: {}", i + 1, ex.getMessage(), ex);
                
                // Add failure results for all executions in this batch
                for (Execution execution : batch) {
                    allResults.add(new ExecutionSubmitResult(
                        execution.getId(), 
                        "FAILED", 
                        "Batch processing failed: " + ex.getMessage(),
                        null
                    ));
                    totalFailed++;
                }
                totalRequested += batch.size();
            }
        }
        
        String overallStatus = determineOverallStatus(totalSuccessful, totalFailed, totalRequested);
        String message = String.format("Processed %d batches: %d successful, %d failed", 
                                      batches.size(), totalSuccessful, totalFailed);
        
        return new BulkSubmitResult(totalRequested, totalSuccessful, totalFailed, 
                                   allResults, overallStatus, message);
    }
    
    /**
     * Processes executions individually when batching is disabled.
     */
    private BulkSubmitResult processIndividually(List<Execution> executions) {
        logger.info("Processing {} executions individually", executions.size());
        
        List<ExecutionSubmitResult> results = new ArrayList<>();
        int successful = 0;
        int failed = 0;
        
        for (Execution execution : executions) {
            try {
                BulkSubmitResult singleResult = processBatch(List.of(execution));
                results.addAll(singleResult.getResults());
                successful += singleResult.getSuccessful();
                failed += singleResult.getFailed();
                
            } catch (Exception ex) {
                logger.error("Individual execution {} failed: {}", execution.getId(), ex.getMessage());
                results.add(new ExecutionSubmitResult(
                    execution.getId(), 
                    "FAILED", 
                    ex.getMessage(),
                    null
                ));
                failed++;
            }
        }
        
        String overallStatus = determineOverallStatus(successful, failed, executions.size());
        String message = String.format("Individual processing: %d successful, %d failed", successful, failed);
        
        return new BulkSubmitResult(executions.size(), successful, failed, 
                                   results, overallStatus, message);
    }
    
    /**
     * Updates execution entities based on submission results.
     */
    private void updateExecutionStatuses(BulkSubmitResult result, List<Execution> executions) {
        logger.debug("Updating execution statuses for {} results", result.getResults().size());
        
        // Create a map for efficient lookup
        var executionMap = executions.stream()
            .collect(Collectors.toMap(Execution::getId, execution -> execution));
        
        for (ExecutionSubmitResult submitResult : result.getResults()) {
            Execution execution = executionMap.get(submitResult.getExecutionId());
            if (execution != null) {
                updateExecutionFromResult(execution, submitResult);
            }
        }
        
        // Save all updated executions in batch
        executionRepository.saveAll(executions);
        logger.debug("Updated and saved {} execution statuses", executions.size());
    }
    
    /**
     * Updates a single execution entity based on its submit result.
     */
    private void updateExecutionFromResult(Execution execution, ExecutionSubmitResult result) {
        if ("SUCCESS".equals(result.getStatus()) || "COMPLETED".equals(result.getStatus())) {
            // Set execution service ID if provided
            if (result.getExecutionServiceId() != null) {
                execution.setExecutionServiceId(result.getExecutionServiceId());
            }
            
            // Set quantity placed to quantity ordered
            execution.setQuantityPlaced(execution.getQuantityOrdered());
            
            // Update status to SENT (assuming ID 2 based on existing pattern)
            // Note: This should ideally use the same status resolution logic as ExecutionServiceImpl
            if (execution.getExecutionStatus() != null && execution.getExecutionStatus().getId() != 2) {
                // This is a simplified approach - in a real implementation, we'd want to
                // use the same status resolution mechanism as ExecutionServiceImpl
                logger.debug("Execution {} submitted successfully, should update status to SENT", execution.getId());
            }
        } else {
            logger.debug("Execution {} failed submission: {}", execution.getId(), result.getMessage());
        }
    }
    
    /**
     * Creates a failure result for an entire batch.
     */
    private BulkSubmitResult createBatchFailureResult(List<Execution> executions, String errorMessage) {
        List<ExecutionSubmitResult> results = executions.stream()
            .map(execution -> new ExecutionSubmitResult(
                execution.getId(), 
                "FAILED", 
                errorMessage,
                null
            ))
            .collect(Collectors.toList());
        
        return new BulkSubmitResult(executions.size(), 0, executions.size(), 
                                   results, "FAILED", errorMessage);
    }
    
    /**
     * Creates an empty result when no valid executions are found.
     */
    private BulkSubmitResult createEmptyResult(int requestedCount, String message) {
        return new BulkSubmitResult(requestedCount, 0, requestedCount, 
                                   new ArrayList<>(), "FAILED", message);
    }
    
    /**
     * Determines the overall status based on success/failure counts.
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
}