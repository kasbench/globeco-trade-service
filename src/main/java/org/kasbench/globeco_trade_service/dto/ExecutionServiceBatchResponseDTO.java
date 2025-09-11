package org.kasbench.globeco_trade_service.dto;

import java.util.List;

/**
 * DTO for batch execution responses from the external Execution Service API.
 * This matches the format returned by the execution service's batch endpoint.
 */
public class ExecutionServiceBatchResponseDTO {
    private String status;
    private String message;
    private Integer totalRequested;
    private Integer successful;
    private Integer failed;
    private List<ExecutionServiceResultDTO> results;

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

    public List<ExecutionServiceResultDTO> getResults() {
        return results;
    }

    public void setResults(List<ExecutionServiceResultDTO> results) {
        this.results = results;
    }

    @Override
    public String toString() {
        return "ExecutionServiceBatchResponseDTO{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", totalRequested=" + totalRequested +
                ", successful=" + successful +
                ", failed=" + failed +
                ", results=" + results +
                '}';
    }
}