package org.kasbench.globeco_trade_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.kasbench.globeco_trade_service.dto.*;
import org.kasbench.globeco_trade_service.entity.*;
import org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.BulkSubmitResult;
import org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.ExecutionSubmitResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionBatchProcessorTest {
    
    private ExecutionBatchProcessor processor;
    
    @BeforeEach
    void setUp() {
        processor = new ExecutionBatchProcessor();
    }
    
    @Test
    @DisplayName("buildBatchRequest should convert executions to BatchExecutionRequestDTO")
    void testBuildBatchRequest_Success() {
        // Given
        List<Execution> executions = createTestExecutions(3);
        
        // When
        BatchExecutionRequestDTO result = processor.buildBatchRequest(executions);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.getExecutions());
        assertEquals(3, result.getExecutions().size());
        
        // Verify first execution conversion
        ExecutionPostDTO firstDto = result.getExecutions().get(0);
        Execution firstExecution = executions.get(0);
        assertEquals(firstExecution.getExecutionTimestamp(), firstDto.getExecutionTimestamp());
        assertEquals(firstExecution.getExecutionStatus().getId(), firstDto.getExecutionStatusId());
        assertEquals(firstExecution.getBlotter().getId(), firstDto.getBlotterId());
        assertEquals(firstExecution.getTradeType().getId(), firstDto.getTradeTypeId());
        assertEquals(firstExecution.getTradeOrder().getId(), firstDto.getTradeOrderId());
        assertEquals(firstExecution.getDestination().getId(), firstDto.getDestinationId());
        assertEquals(firstExecution.getQuantityOrdered(), firstDto.getQuantityOrdered());
        assertEquals(firstExecution.getQuantityPlaced(), firstDto.getQuantityPlaced());
        assertEquals(firstExecution.getQuantityFilled(), firstDto.getQuantityFilled());
        assertEquals(firstExecution.getLimitPrice(), firstDto.getLimitPrice());
    }
    
    @Test
    @DisplayName("buildBatchRequest should handle null executions list")
    void testBuildBatchRequest_NullExecutions() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> processor.buildBatchRequest(null)
        );
        assertEquals("Executions list cannot be null or empty", exception.getMessage());
    }
    
    @Test
    @DisplayName("buildBatchRequest should handle empty executions list")
    void testBuildBatchRequest_EmptyExecutions() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> processor.buildBatchRequest(new ArrayList<>())
        );
        assertEquals("Executions list cannot be null or empty", exception.getMessage());
    }
    
    @Test
    @DisplayName("buildBatchRequest should handle executions with null relationships")
    void testBuildBatchRequest_NullRelationships() {
        // Given
        Execution execution = new Execution();
        execution.setId(1);
        execution.setExecutionTimestamp(OffsetDateTime.now());
        execution.setQuantityOrdered(BigDecimal.valueOf(100));
        execution.setQuantityPlaced(BigDecimal.valueOf(100));
        execution.setQuantityFilled(BigDecimal.ZERO);
        // Leave relationships null
        
        List<Execution> executions = Arrays.asList(execution);
        
        // When
        BatchExecutionRequestDTO result = processor.buildBatchRequest(executions);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getExecutions().size());
        
        ExecutionPostDTO dto = result.getExecutions().get(0);
        assertNull(dto.getExecutionStatusId());
        assertNull(dto.getBlotterId());
        assertNull(dto.getTradeTypeId());
        assertNull(dto.getTradeOrderId());
        assertNull(dto.getDestinationId());
    }
    
    @Test
    @DisplayName("processResponse should handle successful batch response without individual results")
    void testProcessResponse_AllSuccessful() {
        // Given
        List<Execution> executions = createTestExecutions(2);
        BatchExecutionResponseDTO response = new BatchExecutionResponseDTO(
            "SUCCESS", "All executions processed successfully", 2, 2, 0, null
        );
        
        // When
        BulkSubmitResult result = processor.processResponse(response, executions);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalRequested());
        assertEquals(2, result.getSuccessful());
        assertEquals(0, result.getFailed());
        assertEquals("SUCCESS", result.getOverallStatus());
        assertEquals("All executions processed successfully", result.getMessage());
        assertEquals(2, result.getResults().size());
        
        // Verify individual results
        for (ExecutionSubmitResult submitResult : result.getResults()) {
            assertEquals("SUCCESS", submitResult.getStatus());
            assertEquals("Batch submission successful", submitResult.getMessage());
        }
    }
    
    @Test
    @DisplayName("processResponse should handle failed batch response without individual results")
    void testProcessResponse_AllFailed() {
        // Given
        List<Execution> executions = createTestExecutions(2);
        BatchExecutionResponseDTO response = new BatchExecutionResponseDTO(
            "FAILED", "Batch validation failed", 2, 0, 2, null
        );
        
        // When
        BulkSubmitResult result = processor.processResponse(response, executions);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalRequested());
        assertEquals(0, result.getSuccessful());
        assertEquals(2, result.getFailed());
        assertEquals("FAILED", result.getOverallStatus());
        assertEquals("Batch validation failed", result.getMessage());
        assertEquals(2, result.getResults().size());
        
        // Verify individual results
        for (ExecutionSubmitResult submitResult : result.getResults()) {
            assertEquals("FAILED", submitResult.getStatus());
            assertEquals("Batch validation failed", submitResult.getMessage());
        }
    }
    
    @Test
    @DisplayName("processResponse should handle partial success with individual results")
    void testProcessResponse_PartialSuccess() {
        // Given
        List<Execution> executions = createTestExecutions(3);
        
        List<ExecutionResultDTO> individualResults = Arrays.asList(
            createExecutionResultDTO(0, "SUCCESS", "Execution successful", 101),
            createExecutionResultDTO(1, "FAILED", "Validation error", null),
            createExecutionResultDTO(2, "SUCCESS", "Execution successful", 102)
        );
        
        BatchExecutionResponseDTO response = new BatchExecutionResponseDTO(
            "PARTIAL_SUCCESS", "Some executions failed", 3, 2, 1, individualResults
        );
        
        // When
        BulkSubmitResult result = processor.processResponse(response, executions);
        
        // Then
        assertNotNull(result);
        assertEquals(3, result.getTotalRequested());
        assertEquals(2, result.getSuccessful());
        assertEquals(1, result.getFailed());
        assertEquals("PARTIAL_SUCCESS", result.getOverallStatus());
        assertEquals("Some executions failed", result.getMessage());
        assertEquals(3, result.getResults().size());
        
        // Verify individual results
        List<ExecutionSubmitResult> results = result.getResults();
        assertEquals("SUCCESS", results.get(0).getStatus());
        assertEquals(Integer.valueOf(101), results.get(0).getExecutionServiceId());
        assertEquals("FAILED", results.get(1).getStatus());
        assertEquals("Validation error", results.get(1).getMessage());
        assertEquals("SUCCESS", results.get(2).getStatus());
        assertEquals(Integer.valueOf(102), results.get(2).getExecutionServiceId());
    }
    
    @Test
    @DisplayName("processResponse should handle null response")
    void testProcessResponse_NullResponse() {
        // Given
        List<Execution> executions = createTestExecutions(1);
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> processor.processResponse(null, executions)
        );
        assertEquals("Response cannot be null", exception.getMessage());
    }
    
    @Test
    @DisplayName("processResponse should handle null executions")
    void testProcessResponse_NullExecutions() {
        // Given
        BatchExecutionResponseDTO response = new BatchExecutionResponseDTO(
            "SUCCESS", "All successful", 1, 1, 0, null
        );
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> processor.processResponse(response, null)
        );
        assertEquals("Original executions list cannot be null or empty", exception.getMessage());
    }
    
    @Test
    @DisplayName("processResponse should handle empty executions")
    void testProcessResponse_EmptyExecutions() {
        // Given
        BatchExecutionResponseDTO response = new BatchExecutionResponseDTO(
            "SUCCESS", "All successful", 0, 0, 0, null
        );
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> processor.processResponse(response, new ArrayList<>())
        );
        assertEquals("Original executions list cannot be null or empty", exception.getMessage());
    }
    
    @Test
    @DisplayName("extractFailedExecutions should return executions with retryable failures")
    void testExtractFailedExecutions_RetryableFailures() {
        // Given
        List<Execution> executions = createTestExecutions(4);
        
        List<ExecutionSubmitResult> submitResults = Arrays.asList(
            new ExecutionSubmitResult(executions.get(0).getId(), "SUCCESS", "Success", 101),
            new ExecutionSubmitResult(executions.get(1).getId(), "FAILED", "Connection timeout", null),
            new ExecutionSubmitResult(executions.get(2).getId(), "FAILED", "Validation error", null),
            new ExecutionSubmitResult(executions.get(3).getId(), "FAILED", "Service unavailable", null)
        );
        
        BulkSubmitResult bulkResult = new BulkSubmitResult(4, 1, 3, submitResults, "PARTIAL_SUCCESS", "Mixed results");
        
        // When
        List<Execution> failedExecutions = processor.extractFailedExecutions(bulkResult, executions);
        
        // Then
        assertEquals(2, failedExecutions.size());
        
        // Should include executions with retryable failures (timeout and service unavailable)
        Set<Integer> failedIds = new HashSet<>();
        for (Execution exec : failedExecutions) {
            failedIds.add(exec.getId());
        }
        
        assertTrue(failedIds.contains(executions.get(1).getId())); // timeout - retryable
        assertFalse(failedIds.contains(executions.get(2).getId())); // validation - not retryable
        assertTrue(failedIds.contains(executions.get(3).getId())); // service unavailable - retryable
    }
    
    @Test
    @DisplayName("extractFailedExecutions should handle null result")
    void testExtractFailedExecutions_NullResult() {
        // Given
        List<Execution> executions = createTestExecutions(1);
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> processor.extractFailedExecutions(null, executions)
        );
        assertEquals("Result cannot be null", exception.getMessage());
    }
    
    @Test
    @DisplayName("extractFailedExecutions should handle null executions")
    void testExtractFailedExecutions_NullExecutions() {
        // Given
        BulkSubmitResult result = new BulkSubmitResult(0, 0, 0, new ArrayList<>(), "SUCCESS", "");
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> processor.extractFailedExecutions(result, null)
        );
        assertEquals("Original executions cannot be null", exception.getMessage());
    }
    
    @Test
    @DisplayName("extractFailedExecutions should return empty list when no failures")
    void testExtractFailedExecutions_NoFailures() {
        // Given
        List<Execution> executions = createTestExecutions(2);
        
        List<ExecutionSubmitResult> submitResults = Arrays.asList(
            new ExecutionSubmitResult(executions.get(0).getId(), "SUCCESS", "Success", 101),
            new ExecutionSubmitResult(executions.get(1).getId(), "SUCCESS", "Success", 102)
        );
        
        BulkSubmitResult bulkResult = new BulkSubmitResult(2, 2, 0, submitResults, "SUCCESS", "All successful");
        
        // When
        List<Execution> failedExecutions = processor.extractFailedExecutions(bulkResult, executions);
        
        // Then
        assertTrue(failedExecutions.isEmpty());
    }
    
    @Test
    @DisplayName("extractFailedExecutions should return empty list when all failures are non-retryable")
    void testExtractFailedExecutions_NonRetryableFailures() {
        // Given
        List<Execution> executions = createTestExecutions(3);
        
        List<ExecutionSubmitResult> submitResults = Arrays.asList(
            new ExecutionSubmitResult(executions.get(0).getId(), "FAILED", "Validation error", null),
            new ExecutionSubmitResult(executions.get(1).getId(), "FAILED", "Invalid data", null),
            new ExecutionSubmitResult(executions.get(2).getId(), "FAILED", "Not found", null)
        );
        
        BulkSubmitResult bulkResult = new BulkSubmitResult(3, 0, 3, submitResults, "FAILED", "All failed");
        
        // When
        List<Execution> failedExecutions = processor.extractFailedExecutions(bulkResult, executions);
        
        // Then
        assertTrue(failedExecutions.isEmpty());
    }
    
    @Test
    @DisplayName("BulkSubmitResult should be immutable and provide defensive copies")
    void testBulkSubmitResult_Immutability() {
        // Given
        List<ExecutionSubmitResult> originalResults = new ArrayList<>();
        originalResults.add(new ExecutionSubmitResult(1, "SUCCESS", "Success", 101));
        
        BulkSubmitResult result = new BulkSubmitResult(1, 1, 0, originalResults, "SUCCESS", "All good");
        
        // When - try to modify the original list
        originalResults.add(new ExecutionSubmitResult(2, "FAILED", "Failed", null));
        
        // Then - result should not be affected
        assertEquals(1, result.getResults().size());
        
        // When - try to modify the returned list
        List<ExecutionSubmitResult> returnedResults = result.getResults();
        assertThrows(UnsupportedOperationException.class, () -> 
            returnedResults.add(new ExecutionSubmitResult(3, "FAILED", "Failed", null))
        );
    }
    
    // Helper methods
    
    private List<Execution> createTestExecutions(int count) {
        List<Execution> executions = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Execution execution = new Execution();
            execution.setId(i + 1);
            execution.setExecutionTimestamp(OffsetDateTime.now().minusMinutes(i));
            execution.setQuantityOrdered(BigDecimal.valueOf(100 + i));
            execution.setQuantityPlaced(BigDecimal.valueOf(100 + i));
            execution.setQuantityFilled(BigDecimal.ZERO);
            execution.setLimitPrice(BigDecimal.valueOf(50.0 + i));
            
            // Create related entities
            ExecutionStatus status = new ExecutionStatus();
            status.setId(1);
            execution.setExecutionStatus(status);
            
            Blotter blotter = new Blotter();
            blotter.setId(10 + i);
            execution.setBlotter(blotter);
            
            TradeType tradeType = new TradeType();
            tradeType.setId(20 + i);
            execution.setTradeType(tradeType);
            
            TradeOrder tradeOrder = new TradeOrder();
            tradeOrder.setId(30 + i);
            execution.setTradeOrder(tradeOrder);
            
            Destination destination = new Destination();
            destination.setId(40 + i);
            execution.setDestination(destination);
            
            executions.add(execution);
        }
        
        return executions;
    }
    
    private ExecutionResultDTO createExecutionResultDTO(int requestIndex, String status, String message, Integer executionServiceId) {
        ExecutionResultDTO result = new ExecutionResultDTO();
        result.setRequestIndex(requestIndex);
        result.setStatus(status);
        result.setMessage(message);
        
        if (executionServiceId != null) {
            ExecutionResponseDTO execution = new ExecutionResponseDTO();
            execution.setExecutionServiceId(executionServiceId);
            result.setExecution(execution);
        }
        
        return result;
    }
}