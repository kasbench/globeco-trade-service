package org.kasbench.globeco_trade_service.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for batch execution submission requests to the external Execution Service API.
 * Contains a list of executions to be processed in a single batch operation.
 * Maximum batch size is limited to 100 executions per API specification.
 */
public class ExecutionServiceBatchRequestDTO {
    
    @NotNull(message = "Executions list cannot be null")
    @Size(min = 1, max = 100, message = "Batch size must be between 1 and 100 executions")
    private List<@Valid ExecutionServicePostDTO> executions;
    
    public ExecutionServiceBatchRequestDTO() {
    }
    
    public ExecutionServiceBatchRequestDTO(List<ExecutionServicePostDTO> executions) {
        this.executions = executions;
    }
    
    public List<ExecutionServicePostDTO> getExecutions() {
        return executions;
    }
    
    public void setExecutions(List<ExecutionServicePostDTO> executions) {
        this.executions = executions;
    }
    
    @Override
    public String toString() {
        return "ExecutionServiceBatchRequestDTO{" +
                "executions=" + executions +
                '}';
    }
}