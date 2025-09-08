package org.kasbench.globeco_trade_service.service;

/**
 * Service interface for handling dead letter queue operations.
 * This service is responsible for sending failed compensation events
 * to a dead letter queue for manual intervention.
 */
public interface DeadLetterQueueService {
    
    /**
     * Sends a compensation failed event to the dead letter queue.
     * 
     * @param event The compensation failed event to send
     * @throws DeadLetterQueueException if the event cannot be sent
     */
    void send(CompensationFailedEvent event) throws DeadLetterQueueException;
    
    /**
     * Exception thrown when dead letter queue operations fail.
     */
    class DeadLetterQueueException extends RuntimeException {
        public DeadLetterQueueException(String message) {
            super(message);
        }
        
        public DeadLetterQueueException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}