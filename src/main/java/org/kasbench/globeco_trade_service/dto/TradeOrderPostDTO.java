package org.kasbench.globeco_trade_service.dto;

public class TradeOrderPostDTO {
    private Integer orderId;
    private String portfolioId;
    private String orderType;
    private String securityId;
    private java.math.BigDecimal quantity;
    private java.math.BigDecimal limitPrice;
    private java.time.OffsetDateTime tradeTimestamp;
    private Integer blotterId;

    public Integer getOrderId() {
        return orderId;
    }
    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }
    public String getPortfolioId() {
        return portfolioId;
    }
    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }
    public String getOrderType() {
        return orderType;
    }
    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }
    public String getSecurityId() {
        return securityId;
    }
    public void setSecurityId(String securityId) {
        this.securityId = securityId;
    }
    public java.math.BigDecimal getQuantity() {
        return quantity;
    }
    public void setQuantity(java.math.BigDecimal quantity) {
        this.quantity = quantity;
    }
    public java.math.BigDecimal getLimitPrice() {
        return limitPrice;
    }
    public void setLimitPrice(java.math.BigDecimal limitPrice) {
        this.limitPrice = limitPrice;
    }
    public java.time.OffsetDateTime getTradeTimestamp() {
        return tradeTimestamp;
    }
    public void setTradeTimestamp(java.time.OffsetDateTime tradeTimestamp) {
        this.tradeTimestamp = tradeTimestamp;
    }
    public Integer getBlotterId() {
        return blotterId;
    }
    public void setBlotterId(Integer blotterId) {
        this.blotterId = blotterId;
    }
} 