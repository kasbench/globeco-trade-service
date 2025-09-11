package org.kasbench.globeco_trade_service.dto;

/**
 * DTO for individual execution results from the external Execution Service API.
 * This matches the format returned by the execution service's batch endpoint.
 */
public class ExecutionServiceResultDTO {
    private Integer requestIndex;
    private String status;
    private String message;
    private ExecutionServiceResponseDTO execution;

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

    public ExecutionServiceResponseDTO getExecution() {
        return execution;
    }

    public void setExecution(ExecutionServiceResponseDTO execution) {
        this.execution = execution;
    }

    @Override
    public String toString() {
        return "ExecutionServiceResultDTO{" +
                "requestIndex=" + requestIndex +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", execution=" + execution +
                '}';
    }
}