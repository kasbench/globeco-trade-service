package org.kasbench.globeco_trade_service.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class ExecutionV2ResponseDTO {
    private Integer id;
    private OffsetDateTime executionTimestamp;
    private ExecutionStatusResponseDTO executionStatus;
    private BlotterResponseDTO blotter;
    private TradeTypeResponseDTO tradeType;
    private TradeOrderSummaryDTO tradeOrder;
    private DestinationResponseDTO destination;
    private BigDecimal quantityOrdered;
    private BigDecimal quantityPlaced;
    private BigDecimal quantityFilled;
    private BigDecimal limitPrice;
    private Integer executionServiceId;
    private Integer version;
    
    public ExecutionV2ResponseDTO() {
    }
    
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
    
    public TradeOrderSummaryDTO getTradeOrder() {
        return tradeOrder;
    }
    
    public void setTradeOrder(TradeOrderSummaryDTO tradeOrder) {
        this.tradeOrder = tradeOrder;
    }
    
    public DestinationResponseDTO getDestination() {
        return destination;
    }
    
    public void setDestination(DestinationResponseDTO destination) {
        this.destination = destination;
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
    
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    @Override
    public String toString() {
        return "ExecutionV2ResponseDTO{" +
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
                ", executionServiceId=" + executionServiceId +
                ", version=" + version +
                '}';
    }
    
    /**
     * Summary information about the related trade order for execution responses
     */
    public static class TradeOrderSummaryDTO {
        private Integer id;
        private Integer orderId;
        private PortfolioDTO portfolio;
        private SecurityDTO security;
        
        public TradeOrderSummaryDTO() {
        }
        
        public TradeOrderSummaryDTO(Integer id, Integer orderId, PortfolioDTO portfolio, SecurityDTO security) {
            this.id = id;
            this.orderId = orderId;
            this.portfolio = portfolio;
            this.security = security;
        }
        
        public Integer getId() {
            return id;
        }
        
        public void setId(Integer id) {
            this.id = id;
        }
        
        public Integer getOrderId() {
            return orderId;
        }
        
        public void setOrderId(Integer orderId) {
            this.orderId = orderId;
        }
        
        public PortfolioDTO getPortfolio() {
            return portfolio;
        }
        
        public void setPortfolio(PortfolioDTO portfolio) {
            this.portfolio = portfolio;
        }
        
        public SecurityDTO getSecurity() {
            return security;
        }
        
        public void setSecurity(SecurityDTO security) {
            this.security = security;
        }
        
        @Override
        public String toString() {
            return "TradeOrderSummaryDTO{" +
                    "id=" + id +
                    ", orderId=" + orderId +
                    ", portfolio=" + portfolio +
                    ", security=" + security +
                    '}';
        }
    }
} 