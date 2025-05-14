package org.kasbench.globeco_trade_service.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "execution")
public class Execution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "execution_timestamp", nullable = false)
    private OffsetDateTime executionTimestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_status_id", nullable = false)
    private ExecutionStatus executionStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blotter_id")
    private Blotter blotter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_type_id")
    private TradeType tradeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_order_id", nullable = false)
    private TradeOrder tradeOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;

    @Column(name = "quantity_ordered")
    private Short quantityOrdered;

    @Column(name = "quantity_placed", nullable = false, precision = 18, scale = 8)
    private BigDecimal quantityPlaced;

    @Column(name = "quantity_filled", nullable = false, precision = 18, scale = 8)
    private BigDecimal quantityFilled = BigDecimal.ZERO;

    @Column(name = "limit_price", precision = 18, scale = 8)
    private BigDecimal limitPrice;

    @Version
    @Column(nullable = false)
    private Integer version = 1;

    // Getters and setters for all fields
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public OffsetDateTime getExecutionTimestamp() { return executionTimestamp; }
    public void setExecutionTimestamp(OffsetDateTime executionTimestamp) { this.executionTimestamp = executionTimestamp; }
    public ExecutionStatus getExecutionStatus() { return executionStatus; }
    public void setExecutionStatus(ExecutionStatus executionStatus) { this.executionStatus = executionStatus; }
    public Blotter getBlotter() { return blotter; }
    public void setBlotter(Blotter blotter) { this.blotter = blotter; }
    public TradeType getTradeType() { return tradeType; }
    public void setTradeType(TradeType tradeType) { this.tradeType = tradeType; }
    public TradeOrder getTradeOrder() { return tradeOrder; }
    public void setTradeOrder(TradeOrder tradeOrder) { this.tradeOrder = tradeOrder; }
    public Destination getDestination() { return destination; }
    public void setDestination(Destination destination) { this.destination = destination; }
    public Short getQuantityOrdered() { return quantityOrdered; }
    public void setQuantityOrdered(Short quantityOrdered) { this.quantityOrdered = quantityOrdered; }
    public BigDecimal getQuantityPlaced() { return quantityPlaced; }
    public void setQuantityPlaced(BigDecimal quantityPlaced) { this.quantityPlaced = quantityPlaced; }
    public BigDecimal getQuantityFilled() { return quantityFilled; }
    public void setQuantityFilled(BigDecimal quantityFilled) { this.quantityFilled = quantityFilled; }
    public BigDecimal getLimitPrice() { return limitPrice; }
    public void setLimitPrice(BigDecimal limitPrice) { this.limitPrice = limitPrice; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
} 