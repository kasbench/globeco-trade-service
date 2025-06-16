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

    SubmitResult submitExecution(Integer id);
    
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