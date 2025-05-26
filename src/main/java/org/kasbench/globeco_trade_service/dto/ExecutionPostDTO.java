package org.kasbench.globeco_trade_service.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class ExecutionPostDTO {
    private OffsetDateTime executionTimestamp;
    private Integer executionStatusId;
    private Integer blotterId;
    private Integer tradeTypeId;
    private Integer tradeOrderId;
    private Integer destinationId;
    private BigDecimal quantityOrdered;
    private BigDecimal quantityPlaced;
    private BigDecimal quantityFilled;
    private BigDecimal limitPrice;
    private Integer executionServiceId;

    public OffsetDateTime getExecutionTimestamp() {
        return executionTimestamp;
    }
    public void setExecutionTimestamp(OffsetDateTime executionTimestamp) {
        this.executionTimestamp = executionTimestamp;
    }
    public Integer getExecutionStatusId() {
        return executionStatusId;
    }
    public void setExecutionStatusId(Integer executionStatusId) {
        this.executionStatusId = executionStatusId;
    }
    public Integer getBlotterId() {
        return blotterId;
    }
    public void setBlotterId(Integer blotterId) {
        this.blotterId = blotterId;
    }
    public Integer getTradeTypeId() {
        return tradeTypeId;
    }
    public void setTradeTypeId(Integer tradeTypeId) {
        this.tradeTypeId = tradeTypeId;
    }
    public Integer getTradeOrderId() {
        return tradeOrderId;
    }
    public void setTradeOrderId(Integer tradeOrderId) {
        this.tradeOrderId = tradeOrderId;
    }
    public Integer getDestinationId() {
        return destinationId;
    }
    public void setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
    }
    public BigDecimal getQuantityOrdered() {
        return quantityOrdered;
    }
    public void setQuantityOrdered(BigDecimal quantityOrdered) {
        this.quantityOrdered = quantityOrdered;
    }
    public BigDecimal getQuantityPlaced() {
        return quantityPlaced;
    }
    public void setQuantityPlaced(BigDecimal quantityPlaced) {
        this.quantityPlaced = quantityPlaced;
    }
    public BigDecimal getQuantityFilled() {
        return quantityFilled;
    }
    public void setQuantityFilled(BigDecimal quantityFilled) {
        this.quantityFilled = quantityFilled;
    }
    public BigDecimal getLimitPrice() {
        return limitPrice;
    }
    public void setLimitPrice(BigDecimal limitPrice) {
        this.limitPrice = limitPrice;
    }
    public Integer getExecutionServiceId() {
        return executionServiceId;
    }
    public void setExecutionServiceId(Integer executionServiceId) {
        this.executionServiceId = executionServiceId;
    }
} 