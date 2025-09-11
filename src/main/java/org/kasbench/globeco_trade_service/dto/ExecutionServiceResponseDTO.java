package org.kasbench.globeco_trade_service.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * DTO for execution responses from the external Execution Service API.
 * This matches the format returned by the execution service.
 */
public class ExecutionServiceResponseDTO {
    private Integer id;
    private String executionStatus;
    private String tradeType;
    private String destination;
    private SecurityResponseDTO security;
    private BigDecimal quantity;
    private BigDecimal limitPrice;
    private OffsetDateTime receivedTimestamp;
    private OffsetDateTime sentTimestamp;
    private Integer tradeServiceExecutionId;
    private BigDecimal quantityFilled;
    private BigDecimal averagePrice;
    private Integer version;
    private String ticker;
    private String securityId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public SecurityResponseDTO getSecurity() {
        return security;
    }

    public void setSecurity(SecurityResponseDTO security) {
        this.security = security;
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

    public OffsetDateTime getReceivedTimestamp() {
        return receivedTimestamp;
    }

    public void setReceivedTimestamp(OffsetDateTime receivedTimestamp) {
        this.receivedTimestamp = receivedTimestamp;
    }

    public OffsetDateTime getSentTimestamp() {
        return sentTimestamp;
    }

    public void setSentTimestamp(OffsetDateTime sentTimestamp) {
        this.sentTimestamp = sentTimestamp;
    }

    public Integer getTradeServiceExecutionId() {
        return tradeServiceExecutionId;
    }

    public void setTradeServiceExecutionId(Integer tradeServiceExecutionId) {
        this.tradeServiceExecutionId = tradeServiceExecutionId;
    }

    public BigDecimal getQuantityFilled() {
        return quantityFilled;
    }

    public void setQuantityFilled(BigDecimal quantityFilled) {
        this.quantityFilled = quantityFilled;
    }

    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(BigDecimal averagePrice) {
        this.averagePrice = averagePrice;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getSecurityId() {
        return securityId;
    }

    public void setSecurityId(String securityId) {
        this.securityId = securityId;
    }

    @Override
    public String toString() {
        return "ExecutionServiceResponseDTO{" +
                "id=" + id +
                ", executionStatus='" + executionStatus + '\'' +
                ", tradeType='" + tradeType + '\'' +
                ", destination='" + destination + '\'' +
                ", security=" + security +
                ", quantity=" + quantity +
                ", limitPrice=" + limitPrice +
                ", receivedTimestamp=" + receivedTimestamp +
                ", sentTimestamp=" + sentTimestamp +
                ", tradeServiceExecutionId=" + tradeServiceExecutionId +
                ", quantityFilled=" + quantityFilled +
                ", averagePrice=" + averagePrice +
                ", version=" + version +
                ", ticker='" + ticker + '\'' +
                ", securityId='" + securityId + '\'' +
                '}';
    }

    /**
     * Nested DTO for security information in execution service responses.
     */
    public static class SecurityResponseDTO {
        private String securityId;
        private String ticker;

        public String getSecurityId() {
            return securityId;
        }

        public void setSecurityId(String securityId) {
            this.securityId = securityId;
        }

        public String getTicker() {
            return ticker;
        }

        public void setTicker(String ticker) {
            this.ticker = ticker;
        }

        @Override
        public String toString() {
            return "SecurityResponseDTO{" +
                    "securityId='" + securityId + '\'' +
                    ", ticker='" + ticker + '\'' +
                    '}';
        }
    }
}