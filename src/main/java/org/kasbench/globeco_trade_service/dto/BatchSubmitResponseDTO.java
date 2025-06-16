package org.kasbench.globeco_trade_service.dto;

import java.util.List;

public class BatchSubmitResponseDTO {
    
    public enum BatchStatus {
        SUCCESS, PARTIAL, FAILURE
    }
    
    private BatchStatus status;
    private String message;
    private Integer totalRequested;
    private Integer successful;
    private Integer failed;
    private List<TradeOrderSubmitResultDTO> results;
    
    public BatchSubmitResponseDTO() {
    }
    
    public BatchSubmitResponseDTO(BatchStatus status, String message, Integer totalRequested, 
                                  Integer successful, Integer failed, List<TradeOrderSubmitResultDTO> results) {
        this.status = status;
        this.message = message;
        this.totalRequested = totalRequested;
        this.successful = successful;
        this.failed = failed;
        this.results = results;
    }
    
    public BatchStatus getStatus() {
        return status;
    }
    
    public void setStatus(BatchStatus status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Integer getTotalRequested() {
        return totalRequested;
    }
    
    public void setTotalRequested(Integer totalRequested) {
        this.totalRequested = totalRequested;
    }
    
    public Integer getSuccessful() {
        return successful;
    }
    
    public void setSuccessful(Integer successful) {
        this.successful = successful;
    }
    
    public Integer getFailed() {
        return failed;
    }
    
    public void setFailed(Integer failed) {
        this.failed = failed;
    }
    
    public List<TradeOrderSubmitResultDTO> getResults() {
        return results;
    }
    
    public void setResults(List<TradeOrderSubmitResultDTO> results) {
        this.results = results;
    }
    
    @Override
    public String toString() {
        return "BatchSubmitResponseDTO{" +
                "status=" + status +
                ", message='" + message + '\'' +
                ", totalRequested=" + totalRequested +
                ", successful=" + successful +
                ", failed=" + failed +
                ", results=" + (results != null ? results.size() + " items" : "null") +
                '}';
    }
    
    /**
     * Individual trade order submission result within a batch response
     */
    public static class TradeOrderSubmitResultDTO {
        
        public enum SubmitStatus {
            SUCCESS, FAILURE
        }
        
        private Integer tradeOrderId;
        private SubmitStatus status;
        private String message;
        private ExecutionResponseDTO execution;
        private Integer requestIndex;
        
        public TradeOrderSubmitResultDTO() {
        }
        
        public TradeOrderSubmitResultDTO(Integer tradeOrderId, SubmitStatus status, String message, 
                                         ExecutionResponseDTO execution, Integer requestIndex) {
            this.tradeOrderId = tradeOrderId;
            this.status = status;
            this.message = message;
            this.execution = execution;
            this.requestIndex = requestIndex;
        }
        
        public Integer getTradeOrderId() {
            return tradeOrderId;
        }
        
        public void setTradeOrderId(Integer tradeOrderId) {
            this.tradeOrderId = tradeOrderId;
        }
        
        public SubmitStatus getStatus() {
            return status;
        }
        
        public void setStatus(SubmitStatus status) {
            this.status = status;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public ExecutionResponseDTO getExecution() {
            return execution;
        }
        
        public void setExecution(ExecutionResponseDTO execution) {
            this.execution = execution;
        }
        
        public Integer getRequestIndex() {
            return requestIndex;
        }
        
        public void setRequestIndex(Integer requestIndex) {
            this.requestIndex = requestIndex;
        }
        
        @Override
        public String toString() {
            return "TradeOrderSubmitResultDTO{" +
                    "tradeOrderId=" + tradeOrderId +
                    ", status=" + status +
                    ", message='" + message + '\'' +
                    ", execution=" + (execution != null ? "ExecutionResponseDTO{id=" + execution.getId() + "}" : "null") +
                    ", requestIndex=" + requestIndex +
                    '}';
        }
    }
} 