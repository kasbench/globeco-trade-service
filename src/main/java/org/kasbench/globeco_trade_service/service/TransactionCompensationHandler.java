package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.entity.Execution;
import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.kasbench.globeco_trade_service.repository.ExecutionRepository;
import org.kasbench.globeco_trade_service.repository.TradeOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Transaction Compensation Handler implementing the saga pattern for handling
 * failed operations with async rollback capabilities and dead letter queue integration.
 * 
 * This component provides sophisticated compensation logic for failed external service
 * submissions, ensuring that only specific failed operations are rolled back while
 * maintaining data consistency.
 */
@Component
public class TransactionCompensationHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionCompensationHandler.class);
    
    private final ExecutionRepository executionRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final DeadLetterQueueService deadLetterQueueService;
    
    @Autowired
    public TransactionCompensationHandler(
            ExecutionRepository executionRepository,
            TradeOrderRepository tradeOrderRepository,
            DeadLetterQueueService deadLetterQueueService) {
        this.executionRepository = executionRepository;
        this.tradeOrderRepository = tradeOrderRepository;
        this.deadLetterQueueService = deadLetterQueueService;
    }
    
    /**
     * Compensates for failed submission with async rollback.
     * This method implements the saga pattern by asynchronously rolling back
     * specific failed operations without affecting successful ones.
     * 
     * @param execution The execution that failed to submit
     * @param originalTradeOrderState The original state of the trade order before submission
     * @return CompletableFuture that completes when compensation is finished
     */
    @Async("compensationExecutor")
    public CompletableFuture<Void> compensateFailedSubmission(Execution execution, TradeOrderState originalTradeOrderState) {
        return CompletableFuture.runAsync(() -> {
            logger.info("Starting async compensation for execution {} and trade order {}", 
                    execution.getId(), originalTradeOrderState.getTradeOrderId());
            
            try {
                // Create compensation record for tracking
                CompensationRecord compensationRecord = createCompensationRecord(execution, originalTradeOrderState);
                
                // Step 1: Delete the execution record (separate transaction)
                deleteExecutionRecord(execution.getId());
                logger.debug("Deleted execution record {} during compensation", execution.getId());
                
                // Step 2: Restore trade order state (separate transaction)
                restoreTradeOrderState(originalTradeOrderState);
                logger.debug("Restored trade order {} state during compensation", originalTradeOrderState.getTradeOrderId());
                
                // Step 3: Mark compensation as successful
                markCompensationSuccessful(compensationRecord);
                
                logger.info("Successfully completed async compensation for execution {} and trade order {}", 
                        execution.getId(), originalTradeOrderState.getTradeOrderId());
                
            } catch (Exception compensationException) {
                logger.error("CRITICAL: Failed to perform async compensation for execution {} and trade order {}: {}", 
                        execution.getId(), originalTradeOrderState.getTradeOrderId(), 
                        compensationException.getMessage(), compensationException);
                
                // Send to dead letter queue for manual intervention
                sendToDeadLetterQueue(execution, originalTradeOrderState, compensationException);
            }
        });
    }
    
    /**
     * Deletes an execution record in a separate transaction for compensation.
     * Uses REQUIRES_NEW propagation to ensure this operation is isolated.
     * 
     * @param executionId The ID of the execution to delete
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteExecutionRecord(Integer executionId) {
        logger.debug("Deleting execution record {} in compensation transaction", executionId);
        
        try {
            if (executionRepository.existsById(executionId)) {
                executionRepository.deleteById(executionId);
                logger.debug("Successfully deleted execution record {} during compensation", executionId);
            } else {
                logger.warn("Execution record {} not found during compensation deletion", executionId);
            }
        } catch (Exception e) {
            logger.error("Failed to delete execution record {} during compensation: {}", 
                    executionId, e.getMessage());
            throw new CompensationException("Failed to delete execution record: " + executionId, e);
        }
    }
    
    /**
     * Restores trade order state in a separate transaction for compensation.
     * Uses REQUIRES_NEW propagation to ensure this operation is isolated.
     * 
     * @param originalState The original state to restore
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restoreTradeOrderState(TradeOrderState originalState) {
        logger.debug("Restoring trade order {} state in compensation transaction", originalState.getTradeOrderId());
        
        try {
            TradeOrder tradeOrder = tradeOrderRepository.findById(originalState.getTradeOrderId())
                    .orElseThrow(() -> new CompensationException("TradeOrder not found during compensation: " + originalState.getTradeOrderId()));
            
            // Restore original values
            tradeOrder.setQuantitySent(originalState.getQuantitySent());
            tradeOrder.setSubmitted(originalState.getSubmitted());
            
            tradeOrderRepository.save(tradeOrder);
            
            logger.debug("Successfully restored trade order {} state during compensation", originalState.getTradeOrderId());
            
        } catch (Exception e) {
            logger.error("Failed to restore trade order {} state during compensation: {}", 
                    originalState.getTradeOrderId(), e.getMessage());
            throw new CompensationException("Failed to restore trade order state: " + originalState.getTradeOrderId(), e);
        }
    }
    
    /**
     * Creates a compensation record for tracking compensation operations.
     * 
     * @param execution The execution being compensated
     * @param originalState The original trade order state
     * @return The created compensation record
     */
    private CompensationRecord createCompensationRecord(Execution execution, TradeOrderState originalState) {
        CompensationRecord record = new CompensationRecord();
        record.setExecutionId(execution.getId());
        record.setTradeOrderId(originalState.getTradeOrderId());
        record.setOriginalQuantitySent(originalState.getQuantitySent());
        record.setOriginalSubmittedStatus(originalState.getSubmitted());
        record.setCompensationStartTime(OffsetDateTime.now());
        record.setStatus(CompensationStatus.IN_PROGRESS);
        
        logger.debug("Created compensation record for execution {} and trade order {}", 
                execution.getId(), originalState.getTradeOrderId());
        
        return record;
    }
    
    /**
     * Marks a compensation as successful.
     * 
     * @param compensationRecord The compensation record to mark as successful
     */
    private void markCompensationSuccessful(CompensationRecord compensationRecord) {
        compensationRecord.setStatus(CompensationStatus.COMPLETED);
        compensationRecord.setCompensationEndTime(OffsetDateTime.now());
        
        logger.debug("Marked compensation as successful for execution {} and trade order {}", 
                compensationRecord.getExecutionId(), compensationRecord.getTradeOrderId());
    }
    
    /**
     * Sends failed compensation to dead letter queue for manual intervention.
     * 
     * @param execution The execution that failed compensation
     * @param originalState The original trade order state
     * @param exception The exception that caused compensation failure
     */
    private void sendToDeadLetterQueue(Execution execution, TradeOrderState originalState, Exception exception) {
        try {
            CompensationFailedEvent event = new CompensationFailedEvent(
                    execution.getId(),
                    originalState.getTradeOrderId(),
                    originalState.getQuantitySent(),
                    originalState.getSubmitted(),
                    exception.getMessage(),
                    OffsetDateTime.now()
            );
            
            deadLetterQueueService.send(event);
            
            logger.error("Sent failed compensation to dead letter queue for execution {} and trade order {}", 
                    execution.getId(), originalState.getTradeOrderId());
            
        } catch (Exception dlqException) {
            logger.error("CRITICAL: Failed to send compensation failure to dead letter queue for execution {} and trade order {}: {}", 
                    execution.getId(), originalState.getTradeOrderId(), dlqException.getMessage(), dlqException);
            
            // This is a critical failure - compensation failed AND we couldn't queue it for manual intervention
            // In a production system, this should trigger immediate alerts
        }
    }
    
    /**
     * Represents the original state of a trade order before submission.
     */
    public static class TradeOrderState {
        private final Integer tradeOrderId;
        private final BigDecimal quantitySent;
        private final Boolean submitted;
        
        public TradeOrderState(Integer tradeOrderId, BigDecimal quantitySent, Boolean submitted) {
            this.tradeOrderId = tradeOrderId;
            this.quantitySent = quantitySent;
            this.submitted = submitted;
        }
        
        public Integer getTradeOrderId() { return tradeOrderId; }
        public BigDecimal getQuantitySent() { return quantitySent; }
        public Boolean getSubmitted() { return submitted; }
    }
    
    /**
     * Represents a compensation record for tracking compensation operations.
     */
    private static class CompensationRecord {
        private Integer executionId;
        private Integer tradeOrderId;
        private BigDecimal originalQuantitySent;
        private Boolean originalSubmittedStatus;
        private OffsetDateTime compensationStartTime;
        private OffsetDateTime compensationEndTime;
        private CompensationStatus status;
        
        // Getters and setters
        public Integer getExecutionId() { return executionId; }
        public void setExecutionId(Integer executionId) { this.executionId = executionId; }
        
        public Integer getTradeOrderId() { return tradeOrderId; }
        public void setTradeOrderId(Integer tradeOrderId) { this.tradeOrderId = tradeOrderId; }
        
        public BigDecimal getOriginalQuantitySent() { return originalQuantitySent; }
        public void setOriginalQuantitySent(BigDecimal originalQuantitySent) { this.originalQuantitySent = originalQuantitySent; }
        
        public Boolean getOriginalSubmittedStatus() { return originalSubmittedStatus; }
        public void setOriginalSubmittedStatus(Boolean originalSubmittedStatus) { this.originalSubmittedStatus = originalSubmittedStatus; }
        
        public OffsetDateTime getCompensationStartTime() { return compensationStartTime; }
        public void setCompensationStartTime(OffsetDateTime compensationStartTime) { this.compensationStartTime = compensationStartTime; }
        
        public OffsetDateTime getCompensationEndTime() { return compensationEndTime; }
        public void setCompensationEndTime(OffsetDateTime compensationEndTime) { this.compensationEndTime = compensationEndTime; }
        
        public CompensationStatus getStatus() { return status; }
        public void setStatus(CompensationStatus status) { this.status = status; }
    }
    
    /**
     * Status of compensation operations.
     */
    private enum CompensationStatus {
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
    
    /**
     * Exception thrown during compensation operations.
     */
    public static class CompensationException extends RuntimeException {
        public CompensationException(String message) {
            super(message);
        }
        
        public CompensationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}