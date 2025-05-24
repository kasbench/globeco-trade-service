package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.entity.Execution;
import java.util.List;
import java.util.Optional;

public interface ExecutionService {
    List<Execution> getAllExecutions();
    Optional<Execution> getExecutionById(Integer id);
    Execution createExecution(Execution execution);
    Execution updateExecution(Integer id, Execution execution);
    void deleteExecution(Integer id, Integer version);

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
} 