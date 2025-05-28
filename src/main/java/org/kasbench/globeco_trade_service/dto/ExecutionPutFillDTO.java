package org.kasbench.globeco_trade_service.dto;

import java.math.BigDecimal;

public class ExecutionPutFillDTO {
    private String executionStatus;
    private BigDecimal quantityFilled;
    private Integer version;

    public ExecutionPutFillDTO() {
    }

    public ExecutionPutFillDTO(String executionStatus, BigDecimal quantityFilled, Integer version) {
        this.executionStatus = executionStatus;
        this.quantityFilled = quantityFilled;
        this.version = version;
    }

    public String getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(String executionStatus) {
        this.executionStatus = executionStatus;
    }

    public BigDecimal getQuantityFilled() {
        return quantityFilled;
    }

    public void setQuantityFilled(BigDecimal quantityFilled) {
        this.quantityFilled = quantityFilled;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
} 