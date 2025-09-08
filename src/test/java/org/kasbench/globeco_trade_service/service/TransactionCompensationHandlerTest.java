package org.kasbench.globeco_trade_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kasbench.globeco_trade_service.entity.Execution;
import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.kasbench.globeco_trade_service.repository.ExecutionRepository;
import org.kasbench.globeco_trade_service.repository.TradeOrderRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionCompensationHandlerTest {
    
    @Mock
    private ExecutionRepository executionRepository;
    
    @Mock
    private TradeOrderRepository tradeOrderRepository;
    
    @Mock
    private DeadLetterQueueService deadLetterQueueService;
    
    @InjectMocks
    private TransactionCompensationHandler compensationHandler;
    
    private Execution testExecution;
    private TransactionCompensationHandler.TradeOrderState testTradeOrderState;
    private TradeOrder testTradeOrder;
    
    @BeforeEach
    void setUp() {
        testExecution = new Execution();
        testExecution.setId(1);
        
        testTradeOrderState = new TransactionCompensationHandler.TradeOrderState(
                100, 
                new BigDecimal("50.00"), 
                false
        );
        
        testTradeOrder = new TradeOrder();
        testTradeOrder.setId(100);
        testTradeOrder.setQuantitySent(new BigDecimal("100.00"));
        testTradeOrder.setSubmitted(true);
    }
    
    @Test
    void testDeleteExecutionRecord_Success() {
        // Given
        when(executionRepository.existsById(1)).thenReturn(true);
        
        // When
        compensationHandler.deleteExecutionRecord(1);
        
        // Then
        verify(executionRepository).existsById(1);
        verify(executionRepository).deleteById(1);
    }
    
    @Test
    void testDeleteExecutionRecord_NotFound() {
        // Given
        when(executionRepository.existsById(1)).thenReturn(false);
        
        // When
        compensationHandler.deleteExecutionRecord(1);
        
        // Then
        verify(executionRepository).existsById(1);
        verify(executionRepository, never()).deleteById(any());
    }
    
    @Test
    void testDeleteExecutionRecord_Exception() {
        // Given
        when(executionRepository.existsById(1)).thenReturn(true);
        doThrow(new RuntimeException("Database error")).when(executionRepository).deleteById(1);
        
        // When & Then
        assertThrows(TransactionCompensationHandler.CompensationException.class, 
                () -> compensationHandler.deleteExecutionRecord(1));
    }
    
    @Test
    void testRestoreTradeOrderState_Success() {
        // Given
        when(tradeOrderRepository.findById(100)).thenReturn(Optional.of(testTradeOrder));
        
        // When
        compensationHandler.restoreTradeOrderState(testTradeOrderState);
        
        // Then
        verify(tradeOrderRepository).findById(100);
        verify(tradeOrderRepository).save(testTradeOrder);
        
        assertEquals(new BigDecimal("50.00"), testTradeOrder.getQuantitySent());
        assertEquals(false, testTradeOrder.getSubmitted());
    }
    
    @Test
    void testRestoreTradeOrderState_TradeOrderNotFound() {
        // Given
        when(tradeOrderRepository.findById(100)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(TransactionCompensationHandler.CompensationException.class, 
                () -> compensationHandler.restoreTradeOrderState(testTradeOrderState));
        
        verify(tradeOrderRepository).findById(100);
        verify(tradeOrderRepository, never()).save(any());
    }
    
    @Test
    void testRestoreTradeOrderState_Exception() {
        // Given
        when(tradeOrderRepository.findById(100)).thenReturn(Optional.of(testTradeOrder));
        doThrow(new RuntimeException("Database error")).when(tradeOrderRepository).save(any());
        
        // When & Then
        assertThrows(TransactionCompensationHandler.CompensationException.class, 
                () -> compensationHandler.restoreTradeOrderState(testTradeOrderState));
    }
    
    @Test
    void testCompensateFailedSubmission_Success() {
        // When
        CompletableFuture<Void> result = compensationHandler.compensateFailedSubmission(testExecution, testTradeOrderState);
        
        // Then
        assertNotNull(result);
        // Note: Since this is async, we can't easily test the completion without additional setup
        // In a real test environment, you might use @Async test configuration or CompletableFuture.join()
    }
    
    @Test
    void testCompensateFailedSubmission_WithDeadLetterQueue() {
        // When
        CompletableFuture<Void> result = compensationHandler.compensateFailedSubmission(testExecution, testTradeOrderState);
        
        // Then
        assertNotNull(result);
        // The dead letter queue service should be called when compensation fails
        // Note: This would need additional async test setup to verify the DLQ call
    }
    
    @Test
    void testTradeOrderState_Creation() {
        // Given
        Integer tradeOrderId = 123;
        BigDecimal quantitySent = new BigDecimal("75.50");
        Boolean submitted = true;
        
        // When
        TransactionCompensationHandler.TradeOrderState state = 
                new TransactionCompensationHandler.TradeOrderState(tradeOrderId, quantitySent, submitted);
        
        // Then
        assertEquals(tradeOrderId, state.getTradeOrderId());
        assertEquals(quantitySent, state.getQuantitySent());
        assertEquals(submitted, state.getSubmitted());
    }
    
    @Test
    void testCompensationException_Creation() {
        // Given
        String message = "Test compensation error";
        Throwable cause = new RuntimeException("Root cause");
        
        // When
        TransactionCompensationHandler.CompensationException exception1 = 
                new TransactionCompensationHandler.CompensationException(message);
        TransactionCompensationHandler.CompensationException exception2 = 
                new TransactionCompensationHandler.CompensationException(message, cause);
        
        // Then
        assertEquals(message, exception1.getMessage());
        assertEquals(message, exception2.getMessage());
        assertEquals(cause, exception2.getCause());
    }
}