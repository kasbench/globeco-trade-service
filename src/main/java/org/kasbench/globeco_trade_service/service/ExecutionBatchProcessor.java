package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.dto.*;
import org.kasbench.globeco_trade_service.entity.Execution;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Component responsible for processing execution batches for bulk submission.
 * Handles conversion between Execution entities and batch DTOs, processes responses,
 * and manages failed execution extraction for retry scenarios.
 */
@Component
public class ExecutionBatchProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(ExecutionBatchProcessor.class);
    
    /**
     * Builds a BatchExecutionRequestDTO from a list of Execution entities.
     * Converts each execution to ExecutionPostDTO format for API submission.
     * 
     * @param executions List of executions to include in the batch
     * @return BatchExecutionRequestDTO ready for API submission
     * @throws IllegalArgumentException if executions list is null or empty
     */
    public BatchExecutionRequestDTO buildBatchRequest(List<Execution> executions) {
        if (executions == null || executions.isEmpty()) {
            throw new IllegalArgumentException("Executions list cannot be null or empty");
        }
        
        logger.debug("Building batch request for {} executions", executions.size());
        
        List<ExecutionPostDTO> executionDTOs = new ArrayList<>();
        
        for (Execution execution : executions) {
            ExecutionPostDTO dto = convertToExecutionPostDTO(execution);
            executionDTOs.add(dto);
        }
        
        BatchExecutionRequestDTO batchRequest = new BatchExecutionRequestDTO(executionDTOs);
        
        logger.debug("Built batch request with {} execution DTOs", executionDTOs.size());
        return batchRequest;
    }
    
    /**
     * Processes a BatchExecutionResponseDTO and maps results back to original executions.
     * Updates execution statuses based on individual results and creates a summary result.
     * 
     * @param response The batch response from the Execution Service API
     * @param originalExecutions The original executions that were submitted
     * @return BulkSubmitResult containing processing summary and individual results
     * @throws IllegalArgumentException if response or originalExecutions is null
     */
    public BulkSubmitResult processResponse(BatchExecutionResponseDTO response, List<Execution> originalExecutions) {
        if (response == null) {
            throw new IllegalArgumentException("Response cannot be null");
        }
        if (originalExecutions == null || originalExecutions.isEmpty()) {
            throw new IllegalArgumentException("Original executions list cannot be null or empty");
        }
        
        logger.debug("Processing batch response with status: {}, {} results", 
                    response.getStatus(), response.getResults() != null ? response.getResults().size() : 0);
        
        List<ExecutionSubmitResult> results = new ArrayList<>();
        int successful = 0;
        int failed = 0;
        
        // Handle case where response has no individual results (e.g., HTTP 201 - all successful)
        if (response.getResults() == null || response.getResults().isEmpty()) {
            if ("SUCCESS".equals(response.getStatus()) || "COMPLETED".equals(response.getStatus())) {
                // All executions successful
                for (int i = 0; i < originalExecutions.size(); i++) {
                    Execution execution = originalExecutions.get(i);
                    ExecutionSubmitResult result = new ExecutionSubmitResult(
                        execution.getId(), 
                        "SUCCESS", 
                        "Batch submission successful",
                        null // executionServiceId will be set elsewhere if available
                    );
                    results.add(result);
                    successful++;
                }
            } else {
                // All executions failed
                for (int i = 0; i < originalExecutions.size(); i++) {
                    Execution execution = originalExecutions.get(i);
                    ExecutionSubmitResult result = new ExecutionSubmitResult(
                        execution.getId(), 
                        "FAILED", 
                        response.getMessage() != null ? response.getMessage() : "Batch submission failed",
                        null
                    );
                    results.add(result);
                    failed++;
                }
            }
        } else {
            // Process individual results
            Map<Integer, Execution> executionByIndex = createExecutionIndexMap(originalExecutions);
            
            for (ExecutionResultDTO resultDTO : response.getResults()) {
                Integer requestIndex = resultDTO.getRequestIndex();
                Execution originalExecution = executionByIndex.get(requestIndex);
                
                if (originalExecution == null) {
                    logger.warn("No original execution found for request index: {}", requestIndex);
                    continue;
                }
                
                ExecutionSubmitResult result = mapResultDTO(resultDTO, originalExecution);
                results.add(result);
                
                if ("SUCCESS".equals(result.getStatus()) || "COMPLETED".equals(result.getStatus())) {
                    successful++;
                } else {
                    failed++;
                }
            }
        }
        
        BulkSubmitResult bulkResult = new BulkSubmitResult(
            originalExecutions.size(),
            successful,
            failed,
            results,
            determineOverallStatus(successful, failed, originalExecutions.size()),
            response.getMessage()
        );
        
        logger.debug("Processed batch response: {} total, {} successful, {} failed", 
                    originalExecutions.size(), successful, failed);
        
        return bulkResult;
    }
    
    /**
     * Extracts executions that failed from a BulkSubmitResult for retry processing.
     * Only includes executions that failed due to transient errors suitable for retry.
     * 
     * @param result The bulk submit result containing individual execution results
     * @param originalExecutions The original executions that were submitted
     * @return List of executions that should be retried
     * @throws IllegalArgumentException if result or originalExecutions is null
     */
    public List<Execution> extractFailedExecutions(BulkSubmitResult result, List<Execution> originalExecutions) {
        if (result == null) {
            throw new IllegalArgumentException("Result cannot be null");
        }
        if (originalExecutions == null) {
            throw new IllegalArgumentException("Original executions cannot be null");
        }
        
        logger.debug("Extracting failed executions from result with {} total results", 
                    result.getResults() != null ? result.getResults().size() : 0);
        
        List<Execution> failedExecutions = new ArrayList<>();
        Map<Integer, Execution> executionMap = originalExecutions.stream()
            .collect(HashMap::new, (map, exec) -> map.put(exec.getId(), exec), HashMap::putAll);
        
        if (result.getResults() != null) {
            for (ExecutionSubmitResult submitResult : result.getResults()) {
                if (isRetryableFailure(submitResult)) {
                    Execution execution = executionMap.get(submitResult.getExecutionId());
                    if (execution != null) {
                        failedExecutions.add(execution);
                    }
                }
            }
        }
        
        logger.debug("Extracted {} failed executions for retry", failedExecutions.size());
        return failedExecutions;
    }
    
    /**
     * Converts an Execution entity to ExecutionPostDTO for API submission.
     */
    private ExecutionPostDTO convertToExecutionPostDTO(Execution execution) {
        ExecutionPostDTO dto = new ExecutionPostDTO();
        
        dto.setExecutionTimestamp(execution.getExecutionTimestamp());
        dto.setExecutionStatusId(execution.getExecutionStatus() != null ? execution.getExecutionStatus().getId() : null);
        dto.setBlotterId(execution.getBlotter() != null ? execution.getBlotter().getId() : null);
        dto.setTradeTypeId(execution.getTradeType() != null ? execution.getTradeType().getId() : null);
        dto.setTradeOrderId(execution.getTradeOrder() != null ? execution.getTradeOrder().getId() : null);
        dto.setDestinationId(execution.getDestination() != null ? execution.getDestination().getId() : null);
        dto.setQuantityOrdered(execution.getQuantityOrdered());
        dto.setQuantityPlaced(execution.getQuantityPlaced());
        dto.setQuantityFilled(execution.getQuantityFilled());
        dto.setLimitPrice(execution.getLimitPrice());
        dto.setExecutionServiceId(execution.getExecutionServiceId());
        
        return dto;
    }
    
    /**
     * Creates a map of request index to execution for efficient lookup during response processing.
     */
    private Map<Integer, Execution> createExecutionIndexMap(List<Execution> executions) {
        return IntStream.range(0, executions.size())
            .boxed()
            .collect(HashMap::new, 
                    (map, index) -> map.put(index, executions.get(index)), 
                    HashMap::putAll);
    }
    
    /**
     * Maps an ExecutionResultDTO to an ExecutionSubmitResult.
     */
    private ExecutionSubmitResult mapResultDTO(ExecutionResultDTO resultDTO, Execution originalExecution) {
        Integer executionServiceId = null;
        if (resultDTO.getExecution() != null && resultDTO.getExecution().getExecutionServiceId() != null) {
            executionServiceId = resultDTO.getExecution().getExecutionServiceId();
        }
        
        return new ExecutionSubmitResult(
            originalExecution.getId(),
            resultDTO.getStatus(),
            resultDTO.getMessage(),
            executionServiceId
        );
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
    
    /**
     * Determines if a failure is retryable based on the status and message.
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
            lowerMessage.contains("forbidden")) {
            return false;
        }
        
        // Retryable failures (transient errors)
        return lowerMessage.contains("timeout") || 
               lowerMessage.contains("connection") || 
               lowerMessage.contains("service unavailable") ||
               lowerMessage.contains("internal server error") ||
               lowerMessage.contains("temporary");
    }
    
    /**
     * Result class for bulk execution submissions.
     */
    public static class BulkSubmitResult {
        private final int totalRequested;
        private final int successful;
        private final int failed;
        private final List<ExecutionSubmitResult> results;
        private final String overallStatus;
        private final String message;
        
        public BulkSubmitResult(int totalRequested, int successful, int failed, 
                               List<ExecutionSubmitResult> results, String overallStatus, String message) {
            this.totalRequested = totalRequested;
            this.successful = successful;
            this.failed = failed;
            this.results = results != null ? new ArrayList<>(results) : new ArrayList<>();
            this.overallStatus = overallStatus;
            this.message = message;
        }
        
        public int getTotalRequested() { return totalRequested; }
        public int getSuccessful() { return successful; }
        public int getFailed() { return failed; }
        public List<ExecutionSubmitResult> getResults() { return Collections.unmodifiableList(results); }
        public String getOverallStatus() { return overallStatus; }
        public String getMessage() { return message; }
        
        @Override
        public String toString() {
            return "BulkSubmitResult{" +
                    "totalRequested=" + totalRequested +
                    ", successful=" + successful +
                    ", failed=" + failed +
                    ", overallStatus='" + overallStatus + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
    
    /**
     * Result class for individual execution submissions within a bulk operation.
     */
    public static class ExecutionSubmitResult {
        private final Integer executionId;
        private final String status;
        private final String message;
        private final Integer executionServiceId;
        
        public ExecutionSubmitResult(Integer executionId, String status, String message, Integer executionServiceId) {
            this.executionId = executionId;
            this.status = status;
            this.message = message;
            this.executionServiceId = executionServiceId;
        }
        
        public Integer getExecutionId() { return executionId; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public Integer getExecutionServiceId() { return executionServiceId; }
        
        @Override
        public String toString() {
            return "ExecutionSubmitResult{" +
                    "executionId=" + executionId +
                    ", status='" + status + '\'' +
                    ", message='" + message + '\'' +
                    ", executionServiceId=" + executionServiceId +
                    '}';
        }
    }
}