package org.kasbench.globeco_trade_service.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class ExecutionResponseDTO {
    private Integer id;
    private OffsetDateTime executionTimestamp;
    private ExecutionStatusResponseDTO executionStatus;
    private BlotterResponseDTO blotter;
    private TradeTypeResponseDTO tradeType;
    private TradeOrderResponseDTO tradeOrder;
    private DestinationResponseDTO destination;
    private Short quantityOrdered;
    private BigDecimal quantityPlaced;
    private BigDecimal quantityFilled;
    private BigDecimal limitPrice;
    private Integer version;

    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public OffsetDateTime getExecutionTimestamp() {
        return executionTimestamp;
    }
    public void setExecutionTimestamp(OffsetDateTime executionTimestamp) {
        this.executionTimestamp = executionTimestamp;
    }
    public ExecutionStatusResponseDTO getExecutionStatus() {
        return executionStatus;
    }
    public void setExecutionStatus(ExecutionStatusResponseDTO executionStatus) {
        this.executionStatus = executionStatus;
    }
    public BlotterResponseDTO getBlotter() {
        return blotter;
    }
    public void setBlotter(BlotterResponseDTO blotter) {
        this.blotter = blotter;
    }
    public TradeTypeResponseDTO getTradeType() {
        return tradeType;
    }
    public void setTradeType(TradeTypeResponseDTO tradeType) {
        this.tradeType = tradeType;
    }
    public TradeOrderResponseDTO getTradeOrder() {
        return tradeOrder;
    }
    public void setTradeOrder(TradeOrderResponseDTO tradeOrder) {
        this.tradeOrder = tradeOrder;
    }
    public DestinationResponseDTO getDestination() {
        return destination;
    }
    public void setDestination(DestinationResponseDTO destination) {
        this.destination = destination;
    }
    public Short getQuantityOrdered() {
        return quantityOrdered;
    }
    public void setQuantityOrdered(Short quantityOrdered) {
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
    public Integer getVersion() {
        return version;
    }
    public void setVersion(Integer version) {
        this.version = version;
    }
} 