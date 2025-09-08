package org.kasbench.globeco_trade_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeadLetterQueueServiceImplTest {
    
    private DeadLetterQueueServiceImpl deadLetterQueueService;
    private CompensationFailedEvent testEvent;
    private Path dlqFile;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        dlqFile = tempDir.resolve("logs/compensation-dlq.log");
        deadLetterQueueService = new DeadLetterQueueServiceImpl(dlqFile.toString());
        
        testEvent = new CompensationFailedEvent(
                1,
                100,
                new BigDecimal("50.00"),
                false,
                "Test compensation failure",
                OffsetDateTime.now()
        );
    }
    
    @Test
    void testSend_Success() throws IOException {
        // When
        assertDoesNotThrow(() -> deadLetterQueueService.send(testEvent));
        
        // Then
        assertTrue(Files.exists(dlqFile));
        
        List<String> lines = Files.readAllLines(dlqFile);
        assertFalse(lines.isEmpty());
        
        String logEntry = lines.get(0);
        assertTrue(logEntry.contains("COMPENSATION_FAILED"));
        assertTrue(logEntry.contains("executionId"));
        assertTrue(logEntry.contains("tradeOrderId"));
    }
    
    @Test
    void testSend_CreatesLogsDirectory() throws IOException {
        // Given
        Path logsDir = tempDir.resolve("logs");
        assertFalse(Files.exists(logsDir));
        
        // When
        assertDoesNotThrow(() -> deadLetterQueueService.send(testEvent));
        
        // Then
        assertTrue(Files.exists(logsDir));
        assertTrue(Files.isDirectory(logsDir));
    }
    
    @Test
    void testSend_AppendsToExistingFile() throws IOException {
        // Given
        CompensationFailedEvent secondEvent = new CompensationFailedEvent(
                2,
                200,
                new BigDecimal("100.00"),
                true,
                "Second compensation failure",
                OffsetDateTime.now()
        );
        
        // When
        assertDoesNotThrow(() -> deadLetterQueueService.send(testEvent));
        assertDoesNotThrow(() -> deadLetterQueueService.send(secondEvent));
        
        // Then
        List<String> lines = Files.readAllLines(dlqFile);
        assertEquals(2, lines.size());
        
        assertTrue(lines.get(0).contains("\"executionId\":1"));
        assertTrue(lines.get(1).contains("\"executionId\":2"));
    }
    
    @Test
    void testCompensationFailedEvent_ToString() {
        // When
        String result = testEvent.toString();
        
        // Then
        assertTrue(result.contains("CompensationFailedEvent"));
        assertTrue(result.contains("executionId=1"));
        assertTrue(result.contains("tradeOrderId=100"));
        assertTrue(result.contains("originalQuantitySent=50.00"));
        assertTrue(result.contains("originalSubmittedStatus=false"));
        assertTrue(result.contains("errorMessage='Test compensation failure'"));
    }
    
    @Test
    void testCompensationFailedEvent_Getters() {
        // Then
        assertEquals(Integer.valueOf(1), testEvent.getExecutionId());
        assertEquals(Integer.valueOf(100), testEvent.getTradeOrderId());
        assertEquals(new BigDecimal("50.00"), testEvent.getOriginalQuantitySent());
        assertEquals(false, testEvent.getOriginalSubmittedStatus());
        assertEquals("Test compensation failure", testEvent.getErrorMessage());
        assertNotNull(testEvent.getFailureTime());
    }
    
    @Test
    void testDeadLetterQueueException_Creation() {
        // Given
        String message = "Test DLQ error";
        Throwable cause = new RuntimeException("Root cause");
        
        // When
        DeadLetterQueueService.DeadLetterQueueException exception1 = 
                new DeadLetterQueueService.DeadLetterQueueException(message);
        DeadLetterQueueService.DeadLetterQueueException exception2 = 
                new DeadLetterQueueService.DeadLetterQueueException(message, cause);
        
        // Then
        assertEquals(message, exception1.getMessage());
        assertEquals(message, exception2.getMessage());
        assertEquals(cause, exception2.getCause());
    }
}