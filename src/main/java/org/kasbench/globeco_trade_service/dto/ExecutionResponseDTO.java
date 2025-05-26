package org.kasbench.globeco_trade_service.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.kasbench.globeco_trade_service.dto.BigDecimalTwoPlacesSerializer;

public class ExecutionResponseDTO {
    private Integer id;
    private OffsetDateTime executionTimestamp;
    private ExecutionStatusResponseDTO executionStatus;
    private BlotterResponseDTO blotter;
    private TradeTypeResponseDTO tradeType;
    private TradeOrderResponseDTO tradeOrder;
    private DestinationResponseDTO destination;
    @JsonSerialize(using = BigDecimalTwoPlacesSerializer.class)
    private java.math.BigDecimal quantityOrdered;
    @JsonSerialize(using = BigDecimalTwoPlacesSerializer.class)
    private BigDecimal quantityPlaced;
    @JsonSerialize(using = BigDecimalTwoPlacesSerializer.class)
    private BigDecimal quantityFilled;
    @JsonSerialize(using = BigDecimalTwoPlacesSerializer.class)
    private BigDecimal limitPrice;
    private Integer version;
    private Integer executionServiceId;

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
    public java.math.BigDecimal getQuantityOrdered() {
        return quantityOrdered;
    }
    public void setQuantityOrdered(java.math.BigDecimal quantityOrdered) {
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
    public Integer getExecutionServiceId() {
        return executionServiceId;
    }
    public void setExecutionServiceId(Integer executionServiceId) {
        this.executionServiceId = executionServiceId;
    }

    public String toString() {
        return "ExecutionResponseDTO{" +
                "id=" + id +
                ", executionTimestamp=" + executionTimestamp +
                ", executionStatus=" + executionStatus +
                ", blotter=" + blotter +
                ", tradeType=" + tradeType +
                ", tradeOrder=" + tradeOrder +
                ", destination=" + destination +
                ", quantityOrdered=" + quantityOrdered +
                ", quantityPlaced=" + quantityPlaced +
                ", quantityFilled=" + quantityFilled +
                ", limitPrice=" + limitPrice +
                ", version=" + version +
                ", executionServiceId=" + executionServiceId +
                '}';
    }
} 