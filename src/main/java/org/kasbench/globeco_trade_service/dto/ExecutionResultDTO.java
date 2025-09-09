package org.kasbench.globeco_trade_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing the result of a single execution within a batch operation.
 * Contains the status, message, and execution data for each processed execution.
 * The requestIndex field maps back to the original position in the batch request.
 */
public class ExecutionResultDTO {
    
    @JsonProperty("requestIndex")
    private Integer requestIndex;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("execution")
    private ExecutionResponseDTO execution;
    
    public ExecutionResultDTO() {
    }
    
    public ExecutionResultDTO(Integer requestIndex, String status, String message, ExecutionResponseDTO execution) {
        this.requestIndex = requestIndex;
        this.status = status;
        this.message = message;
        this.execution = execution;
    }
    
    public Integer getRequestIndex() {
        return requestIndex;
    }
    
    public void setRequestIndex(Integer requestIndex) {
        this.requestIndex = requestIndex;
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
    
    public ExecutionResponseDTO getExecution() {
        return execution;
    }
    
    public void setExecution(ExecutionResponseDTO execution) {
        this.execution = execution;
    }
    
    @Override
    public String toString() {
        return "ExecutionResultDTO{" +
                "requestIndex=" + requestIndex +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", execution=" + execution +
                '}';
    }
}