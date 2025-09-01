package org.kasbench.globeco_trade_service.dto;

import java.util.List;

/**
 * Response DTO for bulk trade order creation operations.
 * Contains the overall status and individual results for each trade order in the bulk request.
 */
public class BulkTradeOrderResponseDTO {
    
    public enum BulkStatus {
        SUCCESS, FAILURE
    }
    
    private BulkStatus status;
    private String message;
    private Integer totalRequested;
    private Integer successful;
    private Integer failed;
    private List<TradeOrderResultDTO> results;
    
    public BulkTradeOrderResponseDTO() {
    }
    
    public BulkTradeOrderResponseDTO(BulkStatus status, String message, Integer totalRequested, 
                                     Integer successful, Integer failed, List<TradeOrderResultDTO> results) {
        this.status = status;
        this.message = message;
        this.totalRequested = totalRequested;
        this.successful = successful;
        this.failed = failed;
        this.results = results;
    }
    
    public BulkStatus getStatus() {
        return status;
    }
    
    public void setStatus(BulkStatus status) {
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
    
    public List<TradeOrderResultDTO> getResults() {
        return results;
    }
    
    public void setResults(List<TradeOrderResultDTO> results) {
        this.results = results;
    }
    
    @Override
    public String toString() {
        return "BulkTradeOrderResponseDTO{" +
                "status=" + status +
                ", message='" + message + '\'' +
                ", totalRequested=" + totalRequested +
                ", successful=" + successful +
                ", failed=" + failed +
                ", results=" + (results != null ? results.size() + " items" : "null") +
                '}';
    }
}