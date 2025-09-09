package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.entity.Execution;
import org.kasbench.globeco_trade_service.dto.ExecutionPutFillDTO;
import java.util.List;
import java.util.Optional;

public interface ExecutionService {
    List<Execution> getAllExecutions();
    
    /**
     * Get all executions with pagination for v1 API backward compatibility
     * @param limit Maximum number of results to return (null for unlimited)
     * @param offset Number of results to skip (null for 0)
     * @return Paginated result with executions and total count
     */
    PaginatedResult<Execution> getAllExecutions(Integer limit, Integer offset);
    
    Optional<Execution> getExecutionById(Integer id);
    Execution createExecution(Execution execution);
    Execution updateExecution(Integer id, Execution execution);
    void deleteExecution(Integer id, Integer version);
    Execution fillExecution(Integer id, ExecutionPutFillDTO fillDTO);

    class SubmitResult {
        private String status;
        private String error;
        public SubmitResult() {}
        public SubmitResult(String status, String error) {
            this.status = status;
            this.error = error;
        }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    /**
     * Result class for individual execution submission within bulk operations
     */
    class ExecutionSubmitResult {
        private Integer executionId;
        private String status; // "SUCCESS", "FAILED", "RETRY_EXHAUSTED"
        private String message;
        private Integer executionServiceId;
        
        public ExecutionSubmitResult() {}
        
        public ExecutionSubmitResult(Integer executionId, String status, String message) {
            this.executionId = executionId;
            this.status = status;
            this.message = message;
        }
        
        public ExecutionSubmitResult(Integer executionId, String status, String message, Integer executionServiceId) {
            this.executionId = executionId;
            this.status = status;
            this.message = message;
            this.executionServiceId = executionServiceId;
        }
        
        public Integer getExecutionId() { return executionId; }
        public void setExecutionId(Integer executionId) { this.executionId = executionId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Integer getExecutionServiceId() { return executionServiceId; }
        public void setExecutionServiceId(Integer executionServiceId) { this.executionServiceId = executionServiceId; }
    }

    /**
     * Result class for bulk execution submissions
     */
    class BulkSubmitResult {
        private int totalRequested;
        private int successful;
        private int failed;
        private List<ExecutionSubmitResult> results;
        private String overallStatus;
        private String message;
        
        public BulkSubmitResult() {}
        
        public BulkSubmitResult(int totalRequested, int successful, int failed, 
                               List<ExecutionSubmitResult> results, String overallStatus, String message) {
            this.totalRequested = totalRequested;
            this.successful = successful;
            this.failed = failed;
            this.results = results;
            this.overallStatus = overallStatus;
            this.message = message;
        }
        
        public int getTotalRequested() { return totalRequested; }
        public void setTotalRequested(int totalRequested) { this.totalRequested = totalRequested; }
        public int getSuccessful() { return successful; }
        public void setSuccessful(int successful) { this.successful = successful; }
        public int getFailed() { return failed; }
        public void setFailed(int failed) { this.failed = failed; }
        public List<ExecutionSubmitResult> getResults() { return results; }
        public void setResults(List<ExecutionSubmitResult> results) { this.results = results; }
        public String getOverallStatus() { return overallStatus; }
        public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    // Existing single execution submission method - maintains backward compatibility
    SubmitResult submitExecution(Integer id);
    
    /**
     * Submit multiple executions using bulk processing
     * @param executionIds List of execution IDs to submit
     * @return BulkSubmitResult containing overall results and individual execution results
     */
    BulkSubmitResult submitExecutions(List<Integer> executionIds);
    
    /**
     * Submit multiple executions using bulk processing with specified batch size
     * @param executionIds List of execution IDs to submit
     * @param batchSize Maximum number of executions per batch (will be capped at API limit)
     * @return BulkSubmitResult containing overall results and individual execution results
     */
    BulkSubmitResult submitExecutionsBatch(List<Integer> executionIds, int batchSize);
    
    /**
     * Result wrapper for paginated data
     */
    class PaginatedResult<T> {
        private final List<T> data;
        private final long totalCount;
        
        public PaginatedResult(List<T> data, long totalCount) {
            this.data = data;
            this.totalCount = totalCount;
        }
        
        public List<T> getData() {
            return data;
        }
        
        public long getTotalCount() {
            return totalCount;
        }
    }
} 