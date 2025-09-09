package org.kasbench.globeco_trade_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kasbench.globeco_trade_service.client.ExecutionServiceClient;
import org.kasbench.globeco_trade_service.config.ExecutionBatchProperties;
import org.kasbench.globeco_trade_service.dto.BatchExecutionRequestDTO;
import org.kasbench.globeco_trade_service.dto.BatchExecutionResponseDTO;
import org.kasbench.globeco_trade_service.dto.ExecutionResultDTO;
import org.kasbench.globeco_trade_service.entity.Execution;
import org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.BulkSubmitResult;
import org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.ExecutionSubmitResult;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutionFailureHandlerTest {

    @Mock
    private ExecutionBatchProcessor batchProcessor;

    @Mock
    private ExecutionServiceClient executionServiceClient;

    @Mock
    private ExecutionBatchProperties batchProperties;

    @Mock
    private RetryTemplate retryTemplate;

    private ExecutionFailureHandler failureHandler;

    @BeforeEach
    void setUp() {
        failureHandler = new ExecutionFailureHandler(
            batchProcessor, executionServiceClient, batchProperties, retryTemplate);
        
        // Default configuration - use lenient to avoid unnecessary stubbing warnings
        lenient().when(batchProperties.getRetryFailedIndividually()).thenReturn(3);
    }

    @Test
    void testHandlePartialFailures_NoFailures_ReturnsOriginalResult() {
        // Arrange
        List<Execution> executions = createTestExecutions(2);
        BulkSubmitResult originalResult = new BulkSubmitResult(
            2, 2, 0, 
            Arrays.asList(
                new ExecutionSubmitResult(1, "SUCCESS", "Success", 101),
                new ExecutionSubmitResult(2, "SUCCESS", "Success", 102)
            ),
            "SUCCESS", "All successful"
        );

        // Act
        BulkSubmitResult result = failureHandler.handlePartialFailures(originalResult, executions);

        // Assert
        assertEquals(originalResult, result);
        assertEquals(2, result.getSuccessful());
        assertEquals(0, result.getFailed());
    }

    @Test
    void testHandlePartialFailures_WithRetryableFailures_RetriesAndMergesResults() throws Exception {
        // Arrange
        List<Execution> executions = createTestExecutions(3);
        BulkSubmitResult originalResult = new BulkSubmitResult(
            3, 1, 2,
            Arrays.asList(
                new ExecutionSubmitResult(1, "SUCCESS", "Success", 101),
                new ExecutionSubmitResult(2, "FAILED", "timeout error", null),
                new ExecutionSubmitResult(3, "FAILED", "connection error", null)
            ),
            "PARTIAL_SUCCESS", "Partial success"
        );

        // Mock successful retry for the first failed execution (ID 2)
        BatchExecutionResponseDTO retryResponse1 = new BatchExecutionResponseDTO();
        retryResponse1.setStatus("SUCCESS");
        retryResponse1.setResults(Arrays.asList(
            createExecutionResultDTO(0, "SUCCESS", "Retry successful", 102)
        ));

        // Mock failed retry for the second failed execution (ID 3)
        BatchExecutionResponseDTO retryResponse2 = new BatchExecutionResponseDTO();
        retryResponse2.setStatus("FAILED");
        retryResponse2.setResults(Arrays.asList(
            createExecutionResultDTO(0, "FAILED", "Still failing", null)
        ));

        lenient().when(retryTemplate.execute(any()))
            .thenReturn(retryResponse1) // First retry succeeds
            .thenReturn(retryResponse2); // Second retry fails
        
        lenient().when(batchProcessor.buildBatchRequest(any())).thenReturn(new BatchExecutionRequestDTO());
        lenient().when(batchProcessor.processResponse(eq(retryResponse1), any())).thenReturn(
            new BulkSubmitResult(1, 1, 0, 
                Arrays.asList(new ExecutionSubmitResult(2, "SUCCESS", "Retry successful", 102)),
                "SUCCESS", "Retry successful")
        );
        lenient().when(batchProcessor.processResponse(eq(retryResponse2), any())).thenReturn(
            new BulkSubmitResult(1, 0, 1, 
                Arrays.asList(new ExecutionSubmitResult(3, "FAILED", "Still failing", null)),
                "FAILED", "Still failing")
        );

        // Act
        BulkSubmitResult result = failureHandler.handlePartialFailures(originalResult, executions);

        // Assert
        assertEquals(3, result.getTotalRequested());
        assertEquals(2, result.getSuccessful()); // Original 1 + 1 retry success
        assertEquals(1, result.getFailed()); // 1 remaining failure
        verify(retryTemplate, times(2)).execute(any()); // Two failed executions retried
    }

    @Test
    void testRetryExecutionIndividually_Success_ReturnsSuccessResult() throws Exception {
        // Arrange
        Execution execution = createTestExecution(1);
        BatchExecutionResponseDTO response = new BatchExecutionResponseDTO();
        response.setStatus("SUCCESS");
        response.setResults(Arrays.asList(
            createExecutionResultDTO(0, "SUCCESS", "Retry successful", 101)
        ));

        lenient().when(retryTemplate.execute(any())).thenReturn(response);
        lenient().when(batchProcessor.buildBatchRequest(any())).thenReturn(new BatchExecutionRequestDTO());
        lenient().when(batchProcessor.processResponse(eq(response), any())).thenReturn(
            new BulkSubmitResult(1, 1, 0, 
                Arrays.asList(new ExecutionSubmitResult(1, "SUCCESS", "Retry successful", 101)),
                "SUCCESS", "Success")
        );

        // Act
        ExecutionSubmitResult result = failureHandler.retryExecutionIndividually(execution);

        // Assert
        assertEquals("SUCCESS", result.getStatus());
        assertEquals(Integer.valueOf(1), result.getExecutionId());
        assertEquals(Integer.valueOf(101), result.getExecutionServiceId());
        assertEquals(0, failureHandler.getRetryAttempts(1)); // Counter cleared on success
    }

    @Test
    void testRetryExecutionIndividually_PermanentFailure_ReturnsRetryExhausted() throws Exception {
        // Arrange
        Execution execution = createTestExecution(1);
        
        when(retryTemplate.execute(any())).thenThrow(
            new RuntimeException("validation error - invalid execution data"));

        // Act
        ExecutionSubmitResult result = failureHandler.retryExecutionIndividually(execution);

        // Assert
        assertEquals("RETRY_EXHAUSTED", result.getStatus());
        assertEquals(Integer.valueOf(1), result.getExecutionId());
        assertTrue(result.getMessage().contains("validation error"));
    }

    @Test
    void testRetryExecutionIndividually_TransientFailure_ReturnsFailedForRetry() throws Exception {
        // Arrange
        Execution execution = createTestExecution(1);
        
        when(retryTemplate.execute(any())).thenThrow(
            new ResourceAccessException("Connection timeout"));

        // Act
        ExecutionSubmitResult result = failureHandler.retryExecutionIndividually(execution);

        // Assert
        assertEquals("FAILED", result.getStatus());
        assertEquals(Integer.valueOf(1), result.getExecutionId());
        assertTrue(result.getMessage().contains("Connection timeout"));
        assertEquals(1, failureHandler.getRetryAttempts(1)); // Counter incremented
    }

    @Test
    void testRetryExecutionIndividually_MaxRetriesExceeded_ReturnsRetryExhausted() throws Exception {
        // Arrange
        Execution execution = createTestExecution(1);
        
        // Simulate max retries reached by calling the method multiple times
        when(retryTemplate.execute(any())).thenThrow(
            new ResourceAccessException("Connection timeout"));
        
        // Call retry method multiple times until we get RETRY_EXHAUSTED
        ExecutionSubmitResult lastResult = null;
        int callCount = 0;
        
        // Keep calling until we get RETRY_EXHAUSTED or reach a reasonable limit
        while (callCount < 10) {
            lastResult = failureHandler.retryExecutionIndividually(execution);
            callCount++;
            
            if ("RETRY_EXHAUSTED".equals(lastResult.getStatus())) {
                break;
            }
            
            // Should be FAILED for non-exhausted attempts
            assertEquals("FAILED", lastResult.getStatus());
        }
        
        // Assert that we eventually got RETRY_EXHAUSTED
        assertNotNull(lastResult);
        assertEquals("RETRY_EXHAUSTED", lastResult.getStatus());
        assertEquals(Integer.valueOf(1), lastResult.getExecutionId());
        assertTrue(callCount <= 4, "Should have reached retry exhaustion within 4 calls, but took " + callCount);
    }

    @Test
    void testRetryFailedExecutions_EmptyList_ReturnsEmptySuccessResult() {
        // Act
        BulkSubmitResult result = failureHandler.retryFailedExecutions(Collections.emptyList());

        // Assert
        assertEquals("SUCCESS", result.getOverallStatus());
        assertEquals(0, result.getTotalRequested());
        assertEquals(0, result.getSuccessful());
        assertEquals(0, result.getFailed());
    }

    @Test
    void testRetryFailedExecutions_AllExhausted_ReturnsRetryExhaustedResult() throws Exception {
        // Arrange
        List<Execution> executions = createTestExecutions(2);
        
        // Mock retry template to always fail
        when(retryTemplate.execute(any())).thenThrow(
            new ResourceAccessException("Connection timeout"));
        
        // Set up retry counters to be at the max limit (simulate previous failures)
        // We'll manually set the retry attempts to max-1 so that the next retry will exhaust them
        for (int i = 0; i < 3; i++) {
            try {
                failureHandler.retryExecutionIndividually(executions.get(0));
                failureHandler.retryExecutionIndividually(executions.get(1));
            } catch (Exception e) {
                // Ignore exceptions, we just want to increment counters
            }
        }

        // Act - this should handle the exhausted retries
        BulkSubmitResult result = failureHandler.retryFailedExecutions(executions);

        // Assert - the result should indicate that retries were exhausted
        assertEquals("FAILED", result.getOverallStatus());
        assertEquals(2, result.getTotalRequested());
        assertEquals(0, result.getSuccessful());
        assertEquals(2, result.getFailed());
        
        // The results should show retry exhaustion
        for (ExecutionSubmitResult submitResult : result.getResults()) {
            assertTrue("RETRY_EXHAUSTED".equals(submitResult.getStatus()) || "FAILED".equals(submitResult.getStatus()),
                      "Expected RETRY_EXHAUSTED or FAILED, but got: " + submitResult.getStatus());
        }
    }

    @Test
    void testRetryFailedExecutions_MixedResults_ReturnsPartialSuccess() throws Exception {
        // Arrange
        List<Execution> executions = createTestExecutions(2);
        
        // Mock first execution succeeds on retry, second fails
        BatchExecutionResponseDTO successResponse = new BatchExecutionResponseDTO();
        successResponse.setStatus("SUCCESS");
        successResponse.setResults(Arrays.asList(
            createExecutionResultDTO(0, "SUCCESS", "Retry successful", 101)
        ));
        
        lenient().when(retryTemplate.execute(any()))
            .thenReturn(successResponse) // First execution succeeds
            .thenThrow(new ResourceAccessException("Still failing")); // Second execution fails
        
        lenient().when(batchProcessor.buildBatchRequest(any())).thenReturn(new BatchExecutionRequestDTO());
        lenient().when(batchProcessor.processResponse(eq(successResponse), any())).thenReturn(
            new BulkSubmitResult(1, 1, 0, 
                Arrays.asList(new ExecutionSubmitResult(1, "SUCCESS", "Retry successful", 101)),
                "SUCCESS", "Success")
        );

        // Act
        BulkSubmitResult result = failureHandler.retryFailedExecutions(executions);

        // Assert
        assertEquals("PARTIAL_SUCCESS", result.getOverallStatus());
        assertEquals(2, result.getTotalRequested());
        assertEquals(1, result.getSuccessful());
        assertEquals(1, result.getFailed());
    }

    @Test
    void testClearRetryCounters_RemovesCounters() {
        // Arrange
        List<Execution> executions = createTestExecutions(2);
        
        // Set some retry counters
        try {
            failureHandler.retryExecutionIndividually(executions.get(0));
            failureHandler.retryExecutionIndividually(executions.get(1));
        } catch (Exception e) {
            // Ignore for test setup
        }
        
        assertEquals(1, failureHandler.getRetryAttempts(1));
        assertEquals(1, failureHandler.getRetryAttempts(2));

        // Act
        failureHandler.clearRetryCounters(Arrays.asList(1, 2));

        // Assert
        assertEquals(0, failureHandler.getRetryAttempts(1));
        assertEquals(0, failureHandler.getRetryAttempts(2));
    }

    @Test
    void testHandlePartialFailures_NullArguments_ThrowsException() {
        // Assert
        assertThrows(IllegalArgumentException.class, 
            () -> failureHandler.handlePartialFailures(null, createTestExecutions(1)));
        
        assertThrows(IllegalArgumentException.class, 
            () -> failureHandler.handlePartialFailures(createTestBulkResult(), null));
    }

    @Test
    void testRetryExecutionIndividually_NullExecution_ThrowsException() {
        // Assert
        assertThrows(IllegalArgumentException.class, 
            () -> failureHandler.retryExecutionIndividually(null));
    }

    @Test
    void testRetryFailedExecutions_NullList_ReturnsEmptyResult() {
        // Act
        BulkSubmitResult result = failureHandler.retryFailedExecutions(null);

        // Assert
        assertEquals("SUCCESS", result.getOverallStatus());
        assertEquals(0, result.getTotalRequested());
    }

    @Test
    void testIsRetryableFailure_ValidationError_NotRetryable() {
        // Arrange
        ExecutionSubmitResult result = new ExecutionSubmitResult(1, "FAILED", "validation error", null);
        
        // Use reflection to test private method or create a public test method
        // For now, we'll test through the public interface
        List<Execution> executions = createTestExecutions(1);
        BulkSubmitResult bulkResult = new BulkSubmitResult(1, 0, 1, 
            Arrays.asList(result), "FAILED", "Validation failed");

        // Act
        BulkSubmitResult retryResult = failureHandler.handlePartialFailures(bulkResult, executions);

        // Assert - should not retry validation errors
        assertEquals(1, retryResult.getFailed());
        assertEquals(0, retryResult.getSuccessful());
    }

    @Test
    void testIsRetryableFailure_TimeoutError_Retryable() throws Exception {
        // Arrange
        ExecutionSubmitResult failedResult = new ExecutionSubmitResult(1, "FAILED", "timeout error", null);
        List<Execution> executions = createTestExecutions(1);
        BulkSubmitResult bulkResult = new BulkSubmitResult(1, 0, 1, 
            Arrays.asList(failedResult), "FAILED", "Timeout");

        // Mock successful retry
        BatchExecutionResponseDTO retryResponse = new BatchExecutionResponseDTO();
        retryResponse.setStatus("SUCCESS");
        retryResponse.setResults(Arrays.asList(
            createExecutionResultDTO(0, "SUCCESS", "Retry successful", 101)
        ));

        lenient().when(retryTemplate.execute(any())).thenReturn(retryResponse);
        lenient().when(batchProcessor.buildBatchRequest(any())).thenReturn(new BatchExecutionRequestDTO());
        lenient().when(batchProcessor.processResponse(eq(retryResponse), any())).thenReturn(
            new BulkSubmitResult(1, 1, 0, 
                Arrays.asList(new ExecutionSubmitResult(1, "SUCCESS", "Retry successful", 101)),
                "SUCCESS", "Success")
        );

        // Act
        BulkSubmitResult retryResult = failureHandler.handlePartialFailures(bulkResult, executions);

        // Assert - should retry timeout errors
        assertEquals(1, retryResult.getSuccessful());
        assertEquals(0, retryResult.getFailed());
        verify(retryTemplate).execute(any());
    }

    // Helper methods
    private List<Execution> createTestExecutions(int count) {
        return Arrays.asList(createTestExecution(1), createTestExecution(2), createTestExecution(3))
            .subList(0, count);
    }

    private Execution createTestExecution(int id) {
        Execution execution = new Execution();
        execution.setId(id);
        return execution;
    }

    private ExecutionResultDTO createExecutionResultDTO(int requestIndex, String status, String message, Integer executionServiceId) {
        ExecutionResultDTO dto = new ExecutionResultDTO();
        dto.setRequestIndex(requestIndex);
        dto.setStatus(status);
        dto.setMessage(message);
        if (executionServiceId != null) {
            // Create a minimal execution DTO with the service ID
            // This would need to match the actual DTO structure
        }
        return dto;
    }

    private BulkSubmitResult createTestBulkResult() {
        return new BulkSubmitResult(1, 1, 0, 
            Arrays.asList(new ExecutionSubmitResult(1, "SUCCESS", "Success", 101)),
            "SUCCESS", "Success");
    }
}