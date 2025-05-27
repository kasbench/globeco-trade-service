package org.kasbench.globeco_trade_service.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "trade_order")
public class TradeOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    @Column(name = "portfolio_id", nullable = false, length = 24)
    private String portfolioId;

    @Column(name = "order_type", nullable = false, length = 10)
    private String orderType;

    @Column(name = "security_id", nullable = false, length = 24)
    private String securityId;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity;

    @Column(name = "quantity_sent", precision = 18, scale = 8)
    private BigDecimal quantitySent = BigDecimal.ZERO;

    @Column(name = "limit_price", precision = 18, scale = 8)
    private BigDecimal limitPrice;

    @Column(name = "trade_timestamp", nullable = false)
    private OffsetDateTime tradeTimestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blotter_id")
    private Blotter blotter;

    @Column(name = "submitted")
    private Boolean submitted = false;

    @Version
    @Column(nullable = false)
    private Integer version = 1;

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
    public Integer getVersion() {
        return version;
    }
    public void setVersion(Integer version) {
        this.version = version;
    }
    public Blotter getBlotter() {
        return blotter;
    }
    public void setBlotter(Blotter blotter) {
        this.blotter = blotter;
    }
    public Boolean getSubmitted() {
        return submitted;
    }
    public void setSubmitted(Boolean submitted) {
        this.submitted = submitted;
    }
} 