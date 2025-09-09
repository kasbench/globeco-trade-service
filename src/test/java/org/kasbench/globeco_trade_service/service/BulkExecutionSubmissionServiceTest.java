package org.kasbench.globeco_trade_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.kasbench.globeco_trade_service.client.ExecutionServiceClient;
import org.kasbench.globeco_trade_service.config.ExecutionBatchProperties;
import org.kasbench.globeco_trade_service.dto.BatchExecutionRequestDTO;
import org.kasbench.globeco_trade_service.dto.BatchExecutionResponseDTO;
import org.kasbench.globeco_trade_service.entity.*;
import org.kasbench.globeco_trade_service.repository.ExecutionRepository;
import org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.BulkSubmitResult;
import org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.ExecutionSubmitResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulkExecutionSubmissionServiceTest {

    @Mock
    private ExecutionRepository executionRepository;

    @Mock
    private ExecutionBatchProcessor batchProcessor;

    @Mock
    private ExecutionServiceClient executionServiceClient;

    @Mock
    private ExecutionBatchProperties batchProperties;

    @Mock
    private ExecutionFailureHandler failureHandler;

    private BulkExecutionSubmissionService service;

    @BeforeEach
    void setUp() {
        service = new BulkExecutionSubmissionService(
            executionRepository, batchProcessor, executionServiceClient, batchProperties, failureHandler);
    }

    @Test
    void submitExecutionsBulk_WithValidExecutionIds_ShouldReturnSuccessResult() {
        // Arrange
        List<Integer> executionIds = Arrays.asList(1, 2, 3);
        List<Execution> executions = createTestExecutions(executionIds);
        
        when(batchProperties.isEnableBatching()).thenReturn(true);
        when(batchProperties.getEffectiveBatchSize()).thenReturn(100);
        
        // Mock repository calls
        when(executionRepository.findByIdWithAllRelations(1)).thenReturn(Optional.of(executions.get(0)));
        when(executionRepository.findByIdWithAllRelations(2)).thenReturn(Optional.of(executions.get(1)));
        when(executionRepository.findByIdWithAllRelations(3)).thenReturn(Optional.of(executions.get(2)));
        
        // Mock batch processing
        BatchExecutionRequestDTO batchRequest = new BatchExecutionRequestDTO();
        when(batchProcessor.buildBatchRequest(executions)).thenReturn(batchRequest);
        
        BatchExecutionResponseDTO response = createSuccessResponse();
        when(executionServiceClient.submitBatch(batchRequest)).thenReturn(response);
        
        BulkSubmitResult expectedResult = createSuccessResult(executions);
        when(batchProcessor.processResponse(response, executions)).thenReturn(expectedResult);
        
        // Act
        BulkSubmitResult result = service.submitExecutionsBulk(executionIds);
        
        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalRequested());
        assertEquals(3, result.getSuccessful());
        assertEquals(0, result.getFailed());
        assertEquals("SUCCESS", result.getOverallStatus());
        
        verify(executionRepository).saveAll(executions);
        verify(batchProcessor).buildBatchRequest(executions);
        verify(executionServiceClient).submitBatch(batchRequest);
        verify(batchProcessor).processResponse(response, executions);
    }

    @Test
    void submitExecutionsBulk_WithNullExecutionIds_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> service.submitExecutionsBulk(null));
    }

    @Test
    void submitExecutionsBulk_WithEmptyExecutionIds_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> service.submitExecutionsBulk(new ArrayList<>()));
    }

    @Test
    void submitExecutionsBulk_WithNonExistentExecutions_ShouldReturnEmptyResult() {
        // Arrange
        List<Integer> executionIds = Arrays.asList(999, 1000);
        
        when(executionRepository.findByIdWithAllRelations(999)).thenReturn(Optional.empty());
        when(executionRepository.findByIdWithAllRelations(1000)).thenReturn(Optional.empty());
        
        // Act
        BulkSubmitResult result = service.submitExecutionsBulk(executionIds);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalRequested());
        assertEquals(0, result.getSuccessful());
        assertEquals(2, result.getFailed());
        assertEquals("FAILED", result.getOverallStatus());
        assertEquals("No valid executions found", result.getMessage());
    }

    @Test
    void submitExecutionsBulk_WithBatchingDisabled_ShouldProcessIndividually() {
        // Arrange
        List<Integer> executionIds = Arrays.asList(1, 2);
        List<Execution> executions = createTestExecutions(executionIds);
        
        when(batchProperties.isEnableBatching()).thenReturn(false);
        
        // Mock repository calls
        when(executionRepository.findByIdWithAllRelations(1)).thenReturn(Optional.of(executions.get(0)));
        when(executionRepository.findByIdWithAllRelations(2)).thenReturn(Optional.of(executions.get(1)));
        
        // Mock individual processing
        for (Execution execution : executions) {
            BatchExecutionRequestDTO batchRequest = new BatchExecutionRequestDTO();
            when(batchProcessor.buildBatchRequest(List.of(execution))).thenReturn(batchRequest);
            
            BatchExecutionResponseDTO response = createSuccessResponse();
            when(executionServiceClient.submitBatch(batchRequest)).thenReturn(response);
            
            BulkSubmitResult singleResult = createSuccessResult(List.of(execution));
            when(batchProcessor.processResponse(response, List.of(execution))).thenReturn(singleResult);
        }
        
        // Act
        BulkSubmitResult result = service.submitExecutionsBulk(executionIds);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalRequested());
        assertEquals(2, result.getSuccessful());
        assertEquals(0, result.getFailed());
        assertEquals("SUCCESS", result.getOverallStatus());
        assertTrue(result.getMessage().contains("Individual processing"));
        
        // Verify individual processing calls
        verify(batchProcessor, times(2)).buildBatchRequest(any());
        verify(executionServiceClient, times(2)).submitBatch(any());
    }

    @Test
    void submitExecutionsBulk_WithLargeList_ShouldSplitIntoBatches() {
        // Arrange
        List<Integer> executionIds = Arrays.asList(1, 2, 3, 4, 5);
        List<Execution> executions = createTestExecutions(executionIds);
        
        when(batchProperties.isEnableBatching()).thenReturn(true);
        when(batchProperties.getEffectiveBatchSize()).thenReturn(2); // Force splitting
        
        // Mock repository calls
        for (int i = 0; i < executionIds.size(); i++) {
            when(executionRepository.findByIdWithAllRelations(executionIds.get(i)))
                .thenReturn(Optional.of(executions.get(i)));
        }
        
        // Mock batch processing for each batch
        BatchExecutionRequestDTO batchRequest = new BatchExecutionRequestDTO();
        when(batchProcessor.buildBatchRequest(any())).thenReturn(batchRequest);
        
        BatchExecutionResponseDTO response = createSuccessResponse();
        when(executionServiceClient.submitBatch(batchRequest)).thenReturn(response);
        
        // Mock responses for different batch sizes
        when(batchProcessor.processResponse(eq(response), any()))
            .thenAnswer(invocation -> {
                List<Execution> batchExecutions = invocation.getArgument(1);
                return createSuccessResult(batchExecutions);
            });
        
        // Act
        BulkSubmitResult result = service.submitExecutionsBulk(executionIds);
        
        // Assert
        assertNotNull(result);
        assertEquals(5, result.getTotalRequested());
        assertEquals(5, result.getSuccessful());
        assertEquals(0, result.getFailed());
        assertEquals("SUCCESS", result.getOverallStatus());
        
        // Should have made 3 batch calls (2+2+1)
        verify(batchProcessor, times(3)).buildBatchRequest(any());
        verify(executionServiceClient, times(3)).submitBatch(any());
    }

    @Test
    void submitExecutionsBulk_WithPartialFailure_ShouldReturnPartialResult() {
        // Arrange
        List<Integer> executionIds = Arrays.asList(1, 2, 3);
        List<Execution> executions = createTestExecutions(executionIds);
        
        when(batchProperties.isEnableBatching()).thenReturn(true);
        when(batchProperties.getEffectiveBatchSize()).thenReturn(100);
        
        // Mock repository calls
        for (int i = 0; i < executionIds.size(); i++) {
            when(executionRepository.findByIdWithAllRelations(executionIds.get(i)))
                .thenReturn(Optional.of(executions.get(i)));
        }
        
        // Mock batch processing with partial failure
        BatchExecutionRequestDTO batchRequest = new BatchExecutionRequestDTO();
        when(batchProcessor.buildBatchRequest(executions)).thenReturn(batchRequest);
        
        BatchExecutionResponseDTO response = createPartialSuccessResponse();
        when(executionServiceClient.submitBatch(batchRequest)).thenReturn(response);
        
        BulkSubmitResult partialResult = createPartialResult(executions);
        when(batchProcessor.processResponse(response, executions)).thenReturn(partialResult);
        
        // Act
        BulkSubmitResult result = service.submitExecutionsBulk(executionIds);
        
        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalRequested());
        assertEquals(2, result.getSuccessful());
        assertEquals(1, result.getFailed());
        assertEquals("PARTIAL_SUCCESS", result.getOverallStatus());
    }

    @Test
    void processBatch_WithValidExecutions_ShouldReturnSuccessResult() {
        // Arrange
        List<Execution> executions = createTestExecutions(Arrays.asList(1, 2));
        
        BatchExecutionRequestDTO batchRequest = new BatchExecutionRequestDTO();
        when(batchProcessor.buildBatchRequest(executions)).thenReturn(batchRequest);
        
        BatchExecutionResponseDTO response = createSuccessResponse();
        when(executionServiceClient.submitBatch(batchRequest)).thenReturn(response);
        
        BulkSubmitResult expectedResult = createSuccessResult(executions);
        when(batchProcessor.processResponse(response, executions)).thenReturn(expectedResult);
        
        // Act
        BulkSubmitResult result = service.processBatch(executions);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalRequested());
        assertEquals(2, result.getSuccessful());
        assertEquals(0, result.getFailed());
        assertEquals("SUCCESS", result.getOverallStatus());
        
        verify(batchProcessor).buildBatchRequest(executions);
        verify(executionServiceClient).submitBatch(batchRequest);
        verify(batchProcessor).processResponse(response, executions);
    }

    @Test
    void processBatch_WithNullExecutions_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> service.processBatch(null));
    }

    @Test
    void processBatch_WithEmptyExecutions_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> service.processBatch(new ArrayList<>()));
    }

    @Test
    void processBatch_WithClientException_ShouldReturnFailureResult() {
        // Arrange
        List<Execution> executions = createTestExecutions(Arrays.asList(1, 2));
        
        BatchExecutionRequestDTO batchRequest = new BatchExecutionRequestDTO();
        when(batchProcessor.buildBatchRequest(executions)).thenReturn(batchRequest);
        
        when(executionServiceClient.submitBatch(batchRequest))
            .thenThrow(new RuntimeException("Service unavailable"));
        
        // Act
        BulkSubmitResult result = service.processBatch(executions);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalRequested());
        assertEquals(0, result.getSuccessful());
        assertEquals(2, result.getFailed());
        assertEquals("FAILED", result.getOverallStatus());
        assertTrue(result.getMessage().contains("Service unavailable"));
        
        // Verify all executions marked as failed
        assertEquals(2, result.getResults().size());
        for (ExecutionSubmitResult submitResult : result.getResults()) {
            assertEquals("FAILED", submitResult.getStatus());
        }
    }

    @Test
    void processBatch_WithBatchProcessorException_ShouldReturnFailureResult() {
        // Arrange
        List<Execution> executions = createTestExecutions(Arrays.asList(1));
        
        when(batchProcessor.buildBatchRequest(executions))
            .thenThrow(new RuntimeException("Batch processing error"));
        
        // Act
        BulkSubmitResult result = service.processBatch(executions);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalRequested());
        assertEquals(0, result.getSuccessful());
        assertEquals(1, result.getFailed());
        assertEquals("FAILED", result.getOverallStatus());
        assertTrue(result.getMessage().contains("Batch processing error"));
    }

    // Helper methods for creating test data

    private List<Execution> createTestExecutions(List<Integer> ids) {
        List<Execution> executions = new ArrayList<>();
        
        for (Integer id : ids) {
            Execution execution = new Execution();
            execution.setId(id);
            execution.setExecutionTimestamp(OffsetDateTime.now());
            execution.setQuantityOrdered(new BigDecimal("100"));
            execution.setQuantityPlaced(new BigDecimal("0"));
            execution.setQuantityFilled(new BigDecimal("0"));
            execution.setLimitPrice(new BigDecimal("50.00"));
            
            // Set up required relationships
            ExecutionStatus status = new ExecutionStatus();
            status.setId(1);
            status.setAbbreviation("NEW");
            execution.setExecutionStatus(status);
            
            TradeOrder tradeOrder = new TradeOrder();
            tradeOrder.setId(id);
            tradeOrder.setSecurityId("SEC" + (1000 + id));
            execution.setTradeOrder(tradeOrder);
            
            TradeType tradeType = new TradeType();
            tradeType.setId(1);
            tradeType.setAbbreviation("BUY");
            execution.setTradeType(tradeType);
            
            Destination destination = new Destination();
            destination.setId(1);
            destination.setAbbreviation("NYSE");
            execution.setDestination(destination);
            
            executions.add(execution);
        }
        
        return executions;
    }

    private BatchExecutionResponseDTO createSuccessResponse() {
        BatchExecutionResponseDTO response = new BatchExecutionResponseDTO();
        response.setStatus("SUCCESS");
        response.setMessage("All executions submitted successfully");
        response.setTotalRequested(3);
        response.setSuccessful(3);
        response.setFailed(0);
        return response;
    }

    private BatchExecutionResponseDTO createPartialSuccessResponse() {
        BatchExecutionResponseDTO response = new BatchExecutionResponseDTO();
        response.setStatus("PARTIAL_SUCCESS");
        response.setMessage("Some executions failed");
        response.setTotalRequested(3);
        response.setSuccessful(2);
        response.setFailed(1);
        return response;
    }

    private BulkSubmitResult createSuccessResult(List<Execution> executions) {
        List<ExecutionSubmitResult> results = new ArrayList<>();
        
        for (Execution execution : executions) {
            results.add(new ExecutionSubmitResult(
                execution.getId(), 
                "SUCCESS", 
                "Submitted successfully",
                100 + execution.getId() // Mock execution service ID
            ));
        }
        
        return new BulkSubmitResult(
            executions.size(), 
            executions.size(), 
            0, 
            results, 
            "SUCCESS", 
            "All executions successful"
        );
    }

    private BulkSubmitResult createPartialResult(List<Execution> executions) {
        List<ExecutionSubmitResult> results = new ArrayList<>();
        
        for (int i = 0; i < executions.size(); i++) {
            Execution execution = executions.get(i);
            if (i < executions.size() - 1) {
                // Success
                results.add(new ExecutionSubmitResult(
                    execution.getId(), 
                    "SUCCESS", 
                    "Submitted successfully",
                    100 + execution.getId()
                ));
            } else {
                // Failure
                results.add(new ExecutionSubmitResult(
                    execution.getId(), 
                    "FAILED", 
                    "Validation error",
                    null
                ));
            }
        }
        
        return new BulkSubmitResult(
            executions.size(), 
            executions.size() - 1, 
            1, 
            results, 
            "PARTIAL_SUCCESS", 
            "Some executions failed"
        );
    }

    @Test
    void processBatch_WithPartialFailuresAndRetryEnabled_ShouldHandleRetries() {
        // Arrange
        List<Execution> executions = createTestExecutions(Arrays.asList(1, 2, 3));
        
        when(batchProperties.getRetryFailedIndividually()).thenReturn(3);
        
        BatchExecutionRequestDTO batchRequest = new BatchExecutionRequestDTO();
        when(batchProcessor.buildBatchRequest(executions)).thenReturn(batchRequest);
        
        BatchExecutionResponseDTO response = createPartialSuccessResponse();
        when(executionServiceClient.submitBatch(batchRequest)).thenReturn(response);
        
        BulkSubmitResult initialResult = createPartialResult(executions);
        when(batchProcessor.processResponse(response, executions)).thenReturn(initialResult);
        
        // Mock retry handling
        BulkSubmitResult retryResult = createRetrySuccessResult(executions);
        when(failureHandler.handlePartialFailures(initialResult, executions)).thenReturn(retryResult);
        
        // Act
        BulkSubmitResult result = service.processBatch(executions);
        
        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalRequested());
        assertEquals(3, result.getSuccessful()); // All successful after retry
        assertEquals(0, result.getFailed());
        assertEquals("SUCCESS", result.getOverallStatus());
        
        verify(failureHandler).handlePartialFailures(initialResult, executions);
        verify(failureHandler).clearRetryCounters(any());
    }

    @Test
    void processBatch_WithPartialFailuresAndRetryDisabled_ShouldNotRetry() {
        // Arrange
        List<Execution> executions = createTestExecutions(Arrays.asList(1, 2, 3));
        
        when(batchProperties.getRetryFailedIndividually()).thenReturn(0); // Retry disabled
        
        BatchExecutionRequestDTO batchRequest = new BatchExecutionRequestDTO();
        when(batchProcessor.buildBatchRequest(executions)).thenReturn(batchRequest);
        
        BatchExecutionResponseDTO response = createPartialSuccessResponse();
        when(executionServiceClient.submitBatch(batchRequest)).thenReturn(response);
        
        BulkSubmitResult partialResult = createPartialResult(executions);
        when(batchProcessor.processResponse(response, executions)).thenReturn(partialResult);
        
        // Act
        BulkSubmitResult result = service.processBatch(executions);
        
        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalRequested());
        assertEquals(2, result.getSuccessful());
        assertEquals(1, result.getFailed());
        assertEquals("PARTIAL_SUCCESS", result.getOverallStatus());
        
        // Verify retry handler was not called
        verify(failureHandler, never()).handlePartialFailures(any(), any());
        verify(failureHandler).clearRetryCounters(any()); // Still clears counters
    }

    @Test
    void processBatch_WithAllFailuresAndRetryEnabled_ShouldAttemptRetries() {
        // Arrange
        List<Execution> executions = createTestExecutions(Arrays.asList(1, 2));
        
        when(batchProperties.getRetryFailedIndividually()).thenReturn(3);
        
        BatchExecutionRequestDTO batchRequest = new BatchExecutionRequestDTO();
        when(batchProcessor.buildBatchRequest(executions)).thenReturn(batchRequest);
        
        BatchExecutionResponseDTO response = createFailureResponse();
        when(executionServiceClient.submitBatch(batchRequest)).thenReturn(response);
        
        BulkSubmitResult allFailedResult = createAllFailedResult(executions);
        when(batchProcessor.processResponse(response, executions)).thenReturn(allFailedResult);
        
        // Mock retry handling - some succeed on retry
        BulkSubmitResult retryResult = createPartialRetryResult(executions);
        when(failureHandler.handlePartialFailures(allFailedResult, executions)).thenReturn(retryResult);
        
        // Act
        BulkSubmitResult result = service.processBatch(executions);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalRequested());
        assertEquals(1, result.getSuccessful()); // One succeeded on retry
        assertEquals(1, result.getFailed());
        assertEquals("PARTIAL_SUCCESS", result.getOverallStatus());
        
        verify(failureHandler).handlePartialFailures(allFailedResult, executions);
    }

    @Test
    void submitExecutionsBulk_WithMultipleBatchesAndRetries_ShouldHandleAllRetries() {
        // Arrange
        List<Integer> executionIds = Arrays.asList(1, 2, 3, 4);
        List<Execution> executions = createTestExecutions(executionIds);
        
        when(batchProperties.isEnableBatching()).thenReturn(true);
        when(batchProperties.getEffectiveBatchSize()).thenReturn(2); // Force 2 batches
        when(batchProperties.getRetryFailedIndividually()).thenReturn(3);
        
        // Mock repository calls
        for (int i = 0; i < executionIds.size(); i++) {
            when(executionRepository.findByIdWithAllRelations(executionIds.get(i)))
                .thenReturn(Optional.of(executions.get(i)));
        }
        
        // Mock batch processing with partial failures in both batches
        BatchExecutionRequestDTO batchRequest = new BatchExecutionRequestDTO();
        when(batchProcessor.buildBatchRequest(any())).thenReturn(batchRequest);
        
        BatchExecutionResponseDTO response = createPartialSuccessResponse();
        when(executionServiceClient.submitBatch(batchRequest)).thenReturn(response);
        
        // Mock partial results for each batch
        when(batchProcessor.processResponse(eq(response), any()))
            .thenAnswer(invocation -> {
                List<Execution> batchExecutions = invocation.getArgument(1);
                return createPartialResult(batchExecutions);
            });
        
        // Mock retry handling for each batch
        when(failureHandler.handlePartialFailures(any(), any()))
            .thenAnswer(invocation -> {
                BulkSubmitResult originalResult = invocation.getArgument(0);
                List<Execution> batchExecutions = invocation.getArgument(1);
                return createRetrySuccessResult(batchExecutions);
            });
        
        // Act
        BulkSubmitResult result = service.submitExecutionsBulk(executionIds);
        
        // Assert
        assertNotNull(result);
        assertEquals(4, result.getTotalRequested());
        assertEquals(4, result.getSuccessful()); // All successful after retries
        assertEquals(0, result.getFailed());
        assertEquals("SUCCESS", result.getOverallStatus());
        
        // Verify retry handler called for each batch
        verify(failureHandler, times(2)).handlePartialFailures(any(), any());
        verify(failureHandler, times(2)).clearRetryCounters(any());
    }

    // Additional helper methods for retry tests

    private BatchExecutionResponseDTO createFailureResponse() {
        BatchExecutionResponseDTO response = new BatchExecutionResponseDTO();
        response.setStatus("FAILED");
        response.setMessage("All executions failed");
        response.setTotalRequested(2);
        response.setSuccessful(0);
        response.setFailed(2);
        return response;
    }

    private BulkSubmitResult createAllFailedResult(List<Execution> executions) {
        List<ExecutionSubmitResult> results = new ArrayList<>();
        
        for (Execution execution : executions) {
            results.add(new ExecutionSubmitResult(
                execution.getId(), 
                "FAILED", 
                "timeout error", // Retryable error
                null
            ));
        }
        
        return new BulkSubmitResult(
            executions.size(), 
            0, 
            executions.size(), 
            results, 
            "FAILED", 
            "All executions failed"
        );
    }

    private BulkSubmitResult createRetrySuccessResult(List<Execution> executions) {
        List<ExecutionSubmitResult> results = new ArrayList<>();
        
        for (Execution execution : executions) {
            results.add(new ExecutionSubmitResult(
                execution.getId(), 
                "SUCCESS", 
                "Retry successful",
                200 + execution.getId() // Mock execution service ID from retry
            ));
        }
        
        return new BulkSubmitResult(
            executions.size(), 
            executions.size(), 
            0, 
            results, 
            "SUCCESS", 
            "All executions successful after retry"
        );
    }

    private BulkSubmitResult createPartialRetryResult(List<Execution> executions) {
        List<ExecutionSubmitResult> results = new ArrayList<>();
        
        for (int i = 0; i < executions.size(); i++) {
            Execution execution = executions.get(i);
            if (i == 0) {
                // First execution succeeds on retry
                results.add(new ExecutionSubmitResult(
                    execution.getId(), 
                    "SUCCESS", 
                    "Retry successful",
                    200 + execution.getId()
                ));
            } else {
                // Others still fail (retry exhausted)
                results.add(new ExecutionSubmitResult(
                    execution.getId(), 
                    "RETRY_EXHAUSTED", 
                    "Maximum retry attempts exceeded",
                    null
                ));
            }
        }
        
        return new BulkSubmitResult(
            executions.size(), 
            1, 
            executions.size() - 1, 
            results, 
            "PARTIAL_SUCCESS", 
            "Some executions succeeded on retry"
        );
    }}
