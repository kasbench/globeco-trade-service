package org.kasbench.globeco_trade_service.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class TradeOrderV2ResponseDTO {
    private Integer id;
    private Integer orderId;
    private PortfolioDTO portfolio;
    private String orderType;
    private SecurityDTO security;
    private BigDecimal quantity;
    private BigDecimal quantitySent;
    private BigDecimal limitPrice;
    private OffsetDateTime tradeTimestamp;
    private BlotterResponseDTO blotter;
    private Boolean submitted;
    private Integer version;
    
    public TradeOrderV2ResponseDTO() {
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
    
    public String getOrderType() {
        return orderType;
    }
    
    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }
    
    public SecurityDTO getSecurity() {
        return security;
    }
    
    public void setSecurity(SecurityDTO security) {
        this.security = security;
    }
    
    public BigDecimal getQuantity() {
        return quantity;
    }
    
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
    
    public BigDecimal getQuantitySent() {
        return quantitySent;
    }
    
    public void setQuantitySent(BigDecimal quantitySent) {
        this.quantitySent = quantitySent;
    }
    
    public BigDecimal getLimitPrice() {
        return limitPrice;
    }
    
    public void setLimitPrice(BigDecimal limitPrice) {
        this.limitPrice = limitPrice;
    }
    
    public OffsetDateTime getTradeTimestamp() {
        return tradeTimestamp;
    }
    
    public void setTradeTimestamp(OffsetDateTime tradeTimestamp) {
        this.tradeTimestamp = tradeTimestamp;
    }
    
    public BlotterResponseDTO getBlotter() {
        return blotter;
    }
    
    public void setBlotter(BlotterResponseDTO blotter) {
        this.blotter = blotter;
    }
    
    public Boolean getSubmitted() {
        return submitted;
    }
    
    public void setSubmitted(Boolean submitted) {
        this.submitted = submitted;
    }
    
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    @Override
    public String toString() {
        return "TradeOrderV2ResponseDTO{" +
                "id=" + id +
                ", orderId=" + orderId +
                ", portfolio=" + portfolio +
                ", orderType='" + orderType + '\'' +
                ", security=" + security +
                ", quantity=" + quantity +
                ", quantitySent=" + quantitySent +
                ", limitPrice=" + limitPrice +
                ", tradeTimestamp=" + tradeTimestamp +
                ", blotter=" + blotter +
                ", submitted=" + submitted +
                ", version=" + version +
                '}';
    }
} 