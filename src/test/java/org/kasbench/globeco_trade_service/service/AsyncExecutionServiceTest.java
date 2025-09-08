package org.kasbench.globeco_trade_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kasbench.globeco_trade_service.entity.Execution;
import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.kasbench.globeco_trade_service.repository.ExecutionRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.atLeastOnce;

@ExtendWith(MockitoExtension.class)
class AsyncExecutionServiceTest {

    @Mock
    private ExecutionService executionService;

    @Mock
    private ExecutionRepository executionRepository;

    @Mock
    private TransactionCompensationHandler compensationHandler;

    private AsyncExecutionService asyncExecutionService;

    @BeforeEach
    void setUp() {
        asyncExecutionService = new AsyncExecutionService(
                executionService, executionRepository, compensationHandler);
    }

    @Test
    void testSubmitExecutionAsync_Success() throws ExecutionException, InterruptedException {
        // Given
        Integer executionId = 1;
        Execution mockExecution = createMockExecution(executionId);
        ExecutionService.SubmitResult successResult = new ExecutionService.SubmitResult("submitted", null);

        when(executionRepository.findById(executionId)).thenReturn(Optional.of(mockExecution));
        when(executionService.submitExecution(executionId)).thenReturn(successResult);

        // When
        CompletableFuture<ExecutionService.SubmitResult> future = asyncExecutionService.submitExecutionAsync(executionId);
        ExecutionService.SubmitResult result = future.get();

        // Then
        assertNotNull(result);
        assertEquals("submitted", result.getStatus());
        assertNull(result.getError());
        
        verify(executionRepository).findById(executionId);
        verify(executionService).submitExecution(executionId);
        verifyNoInteractions(compensationHandler);
    }

    @Test
    void testSubmitExecutionAsync_FailureWithCompensation() throws ExecutionException, InterruptedException {
        // Given
        Integer executionId = 1;
        Execution mockExecution = createMockExecution(executionId);
        ExecutionService.SubmitResult failureResult = new ExecutionService.SubmitResult(null, "Service unavailable");
        CompletableFuture<Void> compensationFuture = CompletableFuture.completedFuture(null);

        when(executionRepository.findById(executionId)).thenReturn(Optional.of(mockExecution));
        when(executionService.submitExecution(executionId)).thenReturn(failureResult);
        when(compensationHandler.compensateFailedSubmission(any(Execution.class), 
                any(TransactionCompensationHandler.TradeOrderState.class))).thenReturn(compensationFuture);

        // When
        CompletableFuture<ExecutionService.SubmitResult> future = asyncExecutionService.submitExecutionAsync(executionId);
        ExecutionService.SubmitResult result = future.get();

        // Then
        assertNotNull(result);
        assertNull(result.getStatus());
        assertEquals("Service unavailable", result.getError());
        
        verify(executionRepository, atLeastOnce()).findById(executionId);
        verify(executionService).submitExecution(executionId);
        verify(compensationHandler).compensateFailedSubmission(any(Execution.class), 
                any(TransactionCompensationHandler.TradeOrderState.class));
    }

    @Test
    void testSubmitExecutionAsync_ExecutionNotFound() throws ExecutionException, InterruptedException {
        // Given
        Integer executionId = 999;
        when(executionRepository.findById(executionId)).thenReturn(Optional.empty());

        // When
        CompletableFuture<ExecutionService.SubmitResult> future = asyncExecutionService.submitExecutionAsync(executionId);
        ExecutionService.SubmitResult result = future.get();

        // Then
        assertNotNull(result);
        assertNull(result.getStatus());
        assertEquals("Execution not found: 999", result.getError());
        
        verify(executionRepository).findById(executionId);
        verifyNoInteractions(executionService);
        verifyNoInteractions(compensationHandler);
    }

    @Test
    void testSubmitExecutionAsync_ExceptionDuringSubmission() {
        // Given
        Integer executionId = 1;
        Execution mockExecution = createMockExecution(executionId);
        RuntimeException submissionException = new RuntimeException("Network timeout");
        CompletableFuture<Void> compensationFuture = CompletableFuture.completedFuture(null);

        when(executionRepository.findById(executionId)).thenReturn(Optional.of(mockExecution));
        when(executionService.submitExecution(executionId)).thenThrow(submissionException);
        when(compensationHandler.compensateFailedSubmission(any(Execution.class), 
                any(TransactionCompensationHandler.TradeOrderState.class))).thenReturn(compensationFuture);

        // When & Then
        CompletableFuture<ExecutionService.SubmitResult> future = asyncExecutionService.submitExecutionAsync(executionId);
        
        assertThrows(ExecutionException.class, () -> {
            future.get();
        });
        
        verify(executionRepository, atLeastOnce()).findById(executionId);
        verify(executionService).submitExecution(executionId);
        verify(compensationHandler).compensateFailedSubmission(any(Execution.class), 
                any(TransactionCompensationHandler.TradeOrderState.class));
    }

    @Test
    void testSubmitExecutionsAsync_Success() throws ExecutionException, InterruptedException {
        // Given
        Integer[] executionIds = {1, 2, 3};
        
        // Mock individual async calls to return immediately
        AsyncExecutionService spyService = spy(asyncExecutionService);
        
        ExecutionService.SubmitResult result1 = new ExecutionService.SubmitResult("submitted", null);
        ExecutionService.SubmitResult result2 = new ExecutionService.SubmitResult("submitted", null);
        ExecutionService.SubmitResult result3 = new ExecutionService.SubmitResult("submitted", null);
        
        when(spyService.submitExecutionAsync(1)).thenReturn(CompletableFuture.completedFuture(result1));
        when(spyService.submitExecutionAsync(2)).thenReturn(CompletableFuture.completedFuture(result2));
        when(spyService.submitExecutionAsync(3)).thenReturn(CompletableFuture.completedFuture(result3));

        // When
        CompletableFuture<ExecutionService.SubmitResult[]> future = spyService.submitExecutionsAsync(executionIds);
        ExecutionService.SubmitResult[] results = future.get();

        // Then
        assertNotNull(results);
        assertEquals(3, results.length);
        
        for (ExecutionService.SubmitResult result : results) {
            assertEquals("submitted", result.getStatus());
            assertNull(result.getError());
        }
        
        verify(spyService).submitExecutionAsync(1);
        verify(spyService).submitExecutionAsync(2);
        verify(spyService).submitExecutionAsync(3);
    }

    @Test
    void testSubmitExecutionsAsync_PartialFailure() throws ExecutionException, InterruptedException {
        // Given
        Integer[] executionIds = {1, 2};
        
        AsyncExecutionService spyService = spy(asyncExecutionService);
        
        ExecutionService.SubmitResult successResult = new ExecutionService.SubmitResult("submitted", null);
        ExecutionService.SubmitResult failureResult = new ExecutionService.SubmitResult(null, "Submission failed");
        
        when(spyService.submitExecutionAsync(1)).thenReturn(CompletableFuture.completedFuture(successResult));
        when(spyService.submitExecutionAsync(2)).thenReturn(CompletableFuture.completedFuture(failureResult));

        // When
        CompletableFuture<ExecutionService.SubmitResult[]> future = spyService.submitExecutionsAsync(executionIds);
        ExecutionService.SubmitResult[] results = future.get();

        // Then
        assertNotNull(results);
        assertEquals(2, results.length);
        
        // First result should be successful
        assertEquals("submitted", results[0].getStatus());
        assertNull(results[0].getError());
        
        // Second result should indicate failure
        assertNull(results[1].getStatus());
        assertEquals("Submission failed", results[1].getError());
        
        verify(spyService).submitExecutionAsync(1);
        verify(spyService).submitExecutionAsync(2);
    }

    @Test
    void testCaptureOriginalExecutionState_Success() {
        // Given
        Integer executionId = 1;
        Execution mockExecution = createMockExecution(executionId);
        when(executionRepository.findById(executionId)).thenReturn(Optional.of(mockExecution));

        // When
        Execution result = ReflectionTestUtils.invokeMethod(asyncExecutionService, 
                "captureOriginalExecutionState", executionId);

        // Then
        assertNotNull(result);
        assertEquals(executionId, result.getId());
        verify(executionRepository).findById(executionId);
    }

    @Test
    void testCaptureOriginalExecutionState_NotFound() {
        // Given
        Integer executionId = 999;
        when(executionRepository.findById(executionId)).thenReturn(Optional.empty());

        // When
        Execution result = ReflectionTestUtils.invokeMethod(asyncExecutionService, 
                "captureOriginalExecutionState", executionId);

        // Then
        assertNull(result);
        verify(executionRepository).findById(executionId);
    }

    @Test
    void testCaptureOriginalTradeOrderState_Success() {
        // Given
        Execution mockExecution = createMockExecution(1);

        // When
        TransactionCompensationHandler.TradeOrderState result = ReflectionTestUtils.invokeMethod(
                asyncExecutionService, "captureOriginalTradeOrderState", mockExecution);

        // Then
        assertNotNull(result);
        assertEquals(Integer.valueOf(100), result.getTradeOrderId());
        assertEquals(new BigDecimal("1000.00"), result.getQuantitySent());
        assertEquals(Boolean.FALSE, result.getSubmitted());
    }

    @Test
    void testCaptureOriginalTradeOrderState_NullExecution() {
        // When
        TransactionCompensationHandler.TradeOrderState result = ReflectionTestUtils.invokeMethod(
                asyncExecutionService, "captureOriginalTradeOrderState", (Execution) null);

        // Then
        assertNotNull(result);
        assertNull(result.getTradeOrderId());
        assertNull(result.getQuantitySent());
        assertNull(result.getSubmitted());
    }

    @Test
    void testHandleAsyncSubmissionFailure_Success() {
        // Given
        Execution mockExecution = createMockExecution(1);
        TransactionCompensationHandler.TradeOrderState mockState = 
                new TransactionCompensationHandler.TradeOrderState(100, new BigDecimal("1000.00"), false);
        String errorMessage = "Service unavailable";
        CompletableFuture<Void> compensationFuture = CompletableFuture.completedFuture(null);

        when(compensationHandler.compensateFailedSubmission(mockExecution, mockState))
                .thenReturn(compensationFuture);

        // When
        ReflectionTestUtils.invokeMethod(asyncExecutionService, "handleAsyncSubmissionFailure", 
                mockExecution, mockState, errorMessage);

        // Then
        verify(compensationHandler).compensateFailedSubmission(mockExecution, mockState);
    }

    @Test
    void testHandleAsyncSubmissionFailure_NullParameters() {
        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> {
            ReflectionTestUtils.invokeMethod(asyncExecutionService, "handleAsyncSubmissionFailure", 
                    null, null, "Error message");
        });

        verifyNoInteractions(compensationHandler);
    }

    private Execution createMockExecution(Integer executionId) {
        Execution execution = new Execution();
        execution.setId(executionId);
        
        TradeOrder tradeOrder = new TradeOrder();
        tradeOrder.setId(100);
        tradeOrder.setQuantitySent(new BigDecimal("1000.00"));
        tradeOrder.setSubmitted(false);
        
        execution.setTradeOrder(tradeOrder);
        
        return execution;
    }
}