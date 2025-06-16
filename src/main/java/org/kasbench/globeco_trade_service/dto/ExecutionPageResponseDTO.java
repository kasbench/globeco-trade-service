package org.kasbench.globeco_trade_service.dto;

import java.util.List;

public class ExecutionPageResponseDTO {
    private List<ExecutionV2ResponseDTO> executions;
    private PaginationDTO pagination;
    
    public ExecutionPageResponseDTO() {
    }
    
    public ExecutionPageResponseDTO(List<ExecutionV2ResponseDTO> executions, PaginationDTO pagination) {
        this.executions = executions;
        this.pagination = pagination;
    }
    
    public List<ExecutionV2ResponseDTO> getExecutions() {
        return executions;
    }
    
    public void setExecutions(List<ExecutionV2ResponseDTO> executions) {
        this.executions = executions;
    }
    
    public PaginationDTO getPagination() {
        return pagination;
    }
    
    public void setPagination(PaginationDTO pagination) {
        this.pagination = pagination;
    }
    
    @Override
    public String toString() {
        return "ExecutionPageResponseDTO{" +
                "executions=" + (executions != null ? executions.size() + " items" : "null") +
                ", pagination=" + pagination +
                '}';
    }
} 