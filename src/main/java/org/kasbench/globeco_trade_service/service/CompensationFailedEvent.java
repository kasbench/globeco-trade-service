package org.kasbench.globeco_trade_service.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Event representing a failed compensation operation that needs to be sent
 * to the dead letter queue for manual intervention.
 */
public class CompensationFailedEvent {
    
    private final Integer executionId;
    private final Integer tradeOrderId;
    private final BigDecimal originalQuantitySent;
    private final Boolean originalSubmittedStatus;
    private final String errorMessage;
    private final OffsetDateTime failureTime;
    
    public CompensationFailedEvent(Integer executionId, Integer tradeOrderId, 
            BigDecimal originalQuantitySent, Boolean originalSubmittedStatus,
            String errorMessage, OffsetDateTime failureTime) {
        this.executionId = executionId;
        this.tradeOrderId = tradeOrderId;
        this.originalQuantitySent = originalQuantitySent;
        this.originalSubmittedStatus = originalSubmittedStatus;
        this.errorMessage = errorMessage;
        this.failureTime = failureTime;
    }
    
    public Integer getExecutionId() {
        return executionId;
    }
    
    public Integer getTradeOrderId() {
        return tradeOrderId;
    }
    
    public BigDecimal getOriginalQuantitySent() {
        return originalQuantitySent;
    }
    
    public Boolean getOriginalSubmittedStatus() {
        return originalSubmittedStatus;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public OffsetDateTime getFailureTime() {
        return failureTime;
    }
    
    @Override
    public String toString() {
        return "CompensationFailedEvent{" +
                "executionId=" + executionId +
                ", tradeOrderId=" + tradeOrderId +
                ", originalQuantitySent=" + originalQuantitySent +
                ", originalSubmittedStatus=" + originalSubmittedStatus +
                ", errorMessage='" + errorMessage + '\'' +
                ", failureTime=" + failureTime +
                '}';
    }
}