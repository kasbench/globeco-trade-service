package org.kasbench.globeco_trade_service.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for batch execution submission responses from the Execution Service API.
 * Contains overall batch status, counts, and individual execution results.
 * Used to process responses from POST /api/v1/executions/batch endpoint.
 */
public class BatchExecutionResponseDTO {
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("totalRequested")
    private Integer totalRequested;
    
    @JsonProperty("successful")
    private Integer successful;
    
    @JsonProperty("failed")
    private Integer failed;
    
    @JsonProperty("results")
    private List<ExecutionResultDTO> results;
    
    public BatchExecutionResponseDTO() {
    }
    
    public BatchExecutionResponseDTO(String status, String message, Integer totalRequested, 
                                   Integer successful, Integer failed, List<ExecutionResultDTO> results) {
        this.status = status;
        this.message = message;
        this.totalRequested = totalRequested;
        this.successful = successful;
        this.failed = failed;
        this.results = results;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
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
    
    public List<ExecutionResultDTO> getResults() {
        return results;
    }
    
    public void setResults(List<ExecutionResultDTO> results) {
        this.results = results;
    }
    
    @Override
    public String toString() {
        return "BatchExecutionResponseDTO{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", totalRequested=" + totalRequested +
                ", successful=" + successful +
                ", failed=" + failed +
                ", results=" + (results != null ? results.size() + " items" : "null") +
                '}';
    }
}