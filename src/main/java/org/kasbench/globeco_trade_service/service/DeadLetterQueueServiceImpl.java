package org.kasbench.globeco_trade_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;

/**
 * Implementation of DeadLetterQueueService that writes failed compensation events
 * to a file-based dead letter queue for manual intervention.
 * 
 * In a production environment, this would typically integrate with a message queue
 * system like RabbitMQ, Apache Kafka, or AWS SQS.
 */
@Service
public class DeadLetterQueueServiceImpl implements DeadLetterQueueService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeadLetterQueueServiceImpl.class);
    private final String dlqFilePath;
    
    private final ObjectMapper objectMapper;
    
    public DeadLetterQueueServiceImpl() {
        this("logs/compensation-dlq.log");
    }
    
    public DeadLetterQueueServiceImpl(String dlqFilePath) {
        this.dlqFilePath = dlqFilePath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Override
    public void send(CompensationFailedEvent event) throws DeadLetterQueueException {
        try {
            logger.warn("Sending compensation failed event to dead letter queue: {}", event);
            
            // Ensure the parent directory exists
            Path dlqFile = Paths.get(dlqFilePath);
            Path parentDir = dlqFile.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // Create a structured log entry
            String logEntry = createLogEntry(event);
            
            // Write to the dead letter queue file
            Files.write(dlqFile, (logEntry + System.lineSeparator()).getBytes(), 
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
            logger.error("CRITICAL: Compensation failed event written to dead letter queue - manual intervention required for execution {} and trade order {}", 
                    event.getExecutionId(), event.getTradeOrderId());
            
        } catch (IOException e) {
            logger.error("Failed to write compensation failed event to dead letter queue: {}", e.getMessage(), e);
            throw new DeadLetterQueueException("Failed to send event to dead letter queue", e);
        }
    }
    
    /**
     * Creates a structured log entry for the compensation failed event.
     * 
     * @param event The compensation failed event
     * @return A formatted log entry string
     */
    private String createLogEntry(CompensationFailedEvent event) {
        try {
            // Create a structured log entry with timestamp and event details
            String timestamp = event.getFailureTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            String eventJson = objectMapper.writeValueAsString(event);
            
            return String.format("[%s] COMPENSATION_FAILED: %s", timestamp, eventJson);
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize compensation failed event to JSON: {}", e.getMessage());
            
            // Fallback to simple string representation
            return String.format("[%s] COMPENSATION_FAILED: executionId=%d, tradeOrderId=%d, error=%s", 
                    event.getFailureTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    event.getExecutionId(), 
                    event.getTradeOrderId(), 
                    event.getErrorMessage());
        }
    }
}