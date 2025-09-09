package org.kasbench.globeco_trade_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kasbench.globeco_trade_service.entity.*;
import org.kasbench.globeco_trade_service.repository.*;
import org.kasbench.globeco_trade_service.service.ExecutionService.BulkSubmitResult;
import org.kasbench.globeco_trade_service.service.ExecutionService.ExecutionSubmitResult;
import org.kasbench.globeco_trade_service.service.ExecutionService.SubmitResult;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Integration test to verify ExecutionServiceImpl correctly integrates with BulkExecutionSubmissionService
 */
@ExtendWith(MockitoExtension.class)
public class ExecutionServiceImplBulkIntegrationTest {

    @Mock
    private ExecutionRepository executionRepository;
    
    @Mock
    private ExecutionStatusRepository executionStatusRepository;
    
    @Mock
    private BlotterRepository blotterRepository;
    
    @Mock
    private TradeTypeRepository tradeTypeRepository;
    
    @Mock
    private TradeOrderRepository tradeOrderRepository;
    
    @Mock
    private DestinationRepository destinationRepository;
    
    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private RetryTemplate retryTemplate;
    
    @Mock
    private BulkExecutionSubmissionService bulkExecutionSubmissionService;

    private ExecutionServiceImpl executionService;

    @BeforeEach
    void setUp() {
        executionService = new ExecutionServiceImpl(
            executionRepository,
            executionStatusRepository,
            blotterRepository,
            tradeTypeRepository,
            tradeOrderRepository,
            destinationRepository,
            restTemplate,
            retryTemplate,
            bulkExecutionSubmissionService
        );
    }

    @Test
    void testSubmitExecution_RoutesThroughBulkProcessor() {
        // Arrange
        Integer executionId = 123;
        
        // Mock the bulk submission service to return a successful result
        org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.ExecutionSubmitResult batchResult = 
            new org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.ExecutionSubmitResult(
                executionId, "SUCCESS", "Execution submitted successfully", 456
            );
        
        org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.BulkSubmitResult bulkResult = 
            new org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.BulkSubmitResult(
                1, 1, 0, Arrays.asList(batchResult), "SUCCESS", "All executions submitted successfully"
            );
        
        when(bulkExecutionSubmissionService.submitExecutionsBulk(anyList())).thenReturn(bulkResult);

        // Act
        SubmitResult result = executionService.submitExecution(executionId);

        // Assert
        assertNotNull(result);
        assertEquals("submitted", result.getStatus());
        assertNull(result.getError());
        
        // Verify that the bulk service was called with a single-item list
        verify(bulkExecutionSubmissionService).submitExecutionsBulk(Arrays.asList(executionId));
    }

    @Test
    void testSubmitExecution_HandlesFailureFromBulkProcessor() {
        // Arrange
        Integer executionId = 123;
        
        // Mock the bulk submission service to return a failure result
        org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.ExecutionSubmitResult batchResult = 
            new org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.ExecutionSubmitResult(
                executionId, "FAILED", "Network error", null
            );
        
        org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.BulkSubmitResult bulkResult = 
            new org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.BulkSubmitResult(
                1, 0, 1, Arrays.asList(batchResult), "FAILED", "Execution submission failed"
            );
        
        when(bulkExecutionSubmissionService.submitExecutionsBulk(anyList())).thenReturn(bulkResult);

        // Act
        SubmitResult result = executionService.submitExecution(executionId);

        // Assert
        assertNotNull(result);
        assertNull(result.getStatus());
        assertEquals("Network error", result.getError());
        
        // Verify that the bulk service was called
        verify(bulkExecutionSubmissionService).submitExecutionsBulk(Arrays.asList(executionId));
    }

    @Test
    void testSubmitExecutions_CallsBulkService() {
        // Arrange
        List<Integer> executionIds = Arrays.asList(1, 2, 3);
        
        // Mock successful bulk result
        List<org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.ExecutionSubmitResult> batchResults = Arrays.asList(
            new org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.ExecutionSubmitResult(1, "SUCCESS", "OK", 101),
            new org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.ExecutionSubmitResult(2, "SUCCESS", "OK", 102),
            new org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.ExecutionSubmitResult(3, "SUCCESS", "OK", 103)
        );
        
        org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.BulkSubmitResult bulkResult = 
            new org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.BulkSubmitResult(
                3, 3, 0, batchResults, "SUCCESS", "All executions submitted successfully"
            );
        
        when(bulkExecutionSubmissionService.submitExecutionsBulk(executionIds)).thenReturn(bulkResult);

        // Act
        BulkSubmitResult result = executionService.submitExecutions(executionIds);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalRequested());
        assertEquals(3, result.getSuccessful());
        assertEquals(0, result.getFailed());
        assertEquals("SUCCESS", result.getOverallStatus());
        assertEquals(3, result.getResults().size());
        
        // Verify conversion from batch processor results to service results
        ExecutionSubmitResult firstResult = result.getResults().get(0);
        assertEquals(Integer.valueOf(1), firstResult.getExecutionId());
        assertEquals("SUCCESS", firstResult.getStatus());
        assertEquals("OK", firstResult.getMessage());
        assertEquals(Integer.valueOf(101), firstResult.getExecutionServiceId());
        
        // Verify that the bulk service was called
        verify(bulkExecutionSubmissionService).submitExecutionsBulk(executionIds);
    }

    @Test
    void testSubmitExecutionsBatch_WithCustomBatchSize() {
        // Arrange
        List<Integer> executionIds = Arrays.asList(1, 2, 3, 4, 5);
        int batchSize = 2;
        
        // Mock successful results for each batch call
        org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.BulkSubmitResult batch1Result = 
            new org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.BulkSubmitResult(
                2, 2, 0, Arrays.asList(
                    new org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.ExecutionSubmitResult(1, "SUCCESS", "OK", 101),
                    new org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.ExecutionSubmitResult(2, "SUCCESS", "OK", 102)
                ), "SUCCESS", "Batch 1 successful"
            );
        
        org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.BulkSubmitResult batch2Result = 
            new org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.BulkSubmitResult(
                2, 2, 0, Arrays.asList(
                    new org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.ExecutionSubmitResult(3, "SUCCESS", "OK", 103),
                    new org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.ExecutionSubmitResult(4, "SUCCESS", "OK", 104)
                ), "SUCCESS", "Batch 2 successful"
            );
        
        org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.BulkSubmitResult batch3Result = 
            new org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.BulkSubmitResult(
                1, 1, 0, Arrays.asList(
                    new org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.ExecutionSubmitResult(5, "SUCCESS", "OK", 105)
                ), "SUCCESS", "Batch 3 successful"
            );
        
        when(bulkExecutionSubmissionService.submitExecutionsBulk(Arrays.asList(1, 2))).thenReturn(batch1Result);
        when(bulkExecutionSubmissionService.submitExecutionsBulk(Arrays.asList(3, 4))).thenReturn(batch2Result);
        when(bulkExecutionSubmissionService.submitExecutionsBulk(Arrays.asList(5))).thenReturn(batch3Result);

        // Act
        BulkSubmitResult result = executionService.submitExecutionsBatch(executionIds, batchSize);

        // Assert
        assertNotNull(result);
        assertEquals(5, result.getTotalRequested());
        assertEquals(5, result.getSuccessful());
        assertEquals(0, result.getFailed());
        assertEquals("SUCCESS", result.getOverallStatus());
        assertEquals(5, result.getResults().size());
        
        // Verify that the bulk service was called for each batch
        verify(bulkExecutionSubmissionService).submitExecutionsBulk(Arrays.asList(1, 2));
        verify(bulkExecutionSubmissionService).submitExecutionsBulk(Arrays.asList(3, 4));
        verify(bulkExecutionSubmissionService).submitExecutionsBulk(Arrays.asList(5));
    }

    @Test
    void testSubmitExecutionsBatch_WithInvalidBatchSize() {
        // Arrange
        List<Integer> executionIds = Arrays.asList(1, 2, 3);
        int invalidBatchSize = 0;

        // Act & Assert
        BulkSubmitResult result = executionService.submitExecutionsBatch(executionIds, invalidBatchSize);
        
        assertNotNull(result);
        assertEquals(3, result.getTotalRequested());
        assertEquals(0, result.getSuccessful());
        assertEquals(3, result.getFailed());
        assertEquals("FAILED", result.getOverallStatus());
        assertTrue(result.getMessage().contains("Batch size must be greater than 0"));
        
        // Verify that the bulk service was not called
        verify(bulkExecutionSubmissionService, never()).submitExecutionsBulk(any());
    }
}