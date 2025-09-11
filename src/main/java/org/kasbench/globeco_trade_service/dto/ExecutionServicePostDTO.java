package org.kasbench.globeco_trade_service.dto;

import java.math.BigDecimal;

/**
 * DTO for execution submission to the external Execution Service API.
 * This matches the format expected by the execution service's batch endpoint.
 */
public class ExecutionServicePostDTO {
    private String executionStatus;
    private String tradeType;
    private String destination;
    private String securityId;
    private BigDecimal quantity;
    private BigDecimal limitPrice;
    private Integer version;

    public ExecutionServicePostDTO() {
    }

    public String getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(String executionStatus) {
        this.executionStatus = executionStatus;
    }

    public String getTradeType() {
        return tradeType;
    }

    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getSecurityId() {
        return securityId;
    }

    public void setSecurityId(String securityId) {
        this.securityId = securityId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getLimitPrice() {
        return limitPrice;
    }

    public void setLimitPrice(BigDecimal limitPrice) {
        this.limitPrice = limitPrice;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "ExecutionServicePostDTO{" +
                "executionStatus='" + executionStatus + '\'' +
                ", tradeType='" + tradeType + '\'' +
                ", destination='" + destination + '\'' +
                ", securityId='" + securityId + '\'' +
                ", quantity=" + quantity +
                ", limitPrice=" + limitPrice +
                ", version=" + version +
                '}';
    }
}