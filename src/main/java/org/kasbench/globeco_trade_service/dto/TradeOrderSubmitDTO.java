package org.kasbench.globeco_trade_service.dto;

import java.math.BigDecimal;

public class TradeOrderSubmitDTO {
    private BigDecimal quantity;
    private Integer destinationId;

    public BigDecimal getQuantity() {
        return quantity;
    }
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
    public Integer getDestinationId() {
        return destinationId;
    }
    public void setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
    }

    @Override
    public String toString() {
        return "TradeOrderSubmitDTO{" +
                "quantity=" + quantity +
                ", destinationId=" + destinationId +
                '}';
    }
} 