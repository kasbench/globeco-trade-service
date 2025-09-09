package org.kasbench.globeco_trade_service.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kasbench.globeco_trade_service.dto.BatchExecutionRequestDTO;
import org.kasbench.globeco_trade_service.dto.BatchExecutionResponseDTO;
import org.kasbench.globeco_trade_service.dto.ExecutionPostDTO;
import org.kasbench.globeco_trade_service.dto.ExecutionResultDTO;
import org.kasbench.globeco_trade_service.dto.ExecutionResponseDTO;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutionServiceClientTest {
    
    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private RetryTemplate retryTemplate;
    
    @Mock
    private RetryContext retryContext;
    
    private ExecutionServiceClient executionServiceClient;
    
    private static final String BASE_URL = "http://test-execution-service:8084";
    private static final String BATCH_URL = BASE_URL + "/api/v1/executions/batch";
    
    @BeforeEach
    void setUp() {
        executionServiceClient = new ExecutionServiceClient(restTemplate, retryTemplate, BASE_URL);
    }
    
    @Test
    void testSubmitBatch_SuccessfulSubmission_Http201() throws Exception {
        // Arrange
        BatchExecutionRequestDTO request = createBatchRequest(2);
        BatchExecutionResponseDTO expectedResponse = createSuccessfulResponse(2, 2, 0);
        ResponseEntity<BatchExecutionResponseDTO> responseEntity = 
            new ResponseEntity<>(expectedResponse, HttpStatus.CREATED);
        
        when(retryContext.getRetryCount()).thenReturn(0);
        when(retryTemplate.execute(any())).thenAnswer(invocation -> {
            return invocation.getArgument(0, org.springframework.retry.RetryCallback.class).doWithRetry(retryContext);
        });
        when(restTemplate.postForEntity(eq(BATCH_URL), eq(request), eq(BatchExecutionResponseDTO.class)))
            .thenReturn(responseEntity);
        
        // Act
        BatchExecutionResponseDTO result = executionServiceClient.submitBatch(request);
        
        // Assert
        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals(2, result.getTotalRequested());
        assertEquals(2, result.getSuccessful());
        assertEquals(0, result.getFailed());
        
        verify(restTemplate).postForEntity(BATCH_URL, request, BatchExecutionResponseDTO.class);
        verify(retryTemplate).execute(any());
    }
    
    @Test
    void testSubmitBatch_PartialSuccess_Http207() throws Exception {
        // Arrange
        BatchExecutionRequestDTO request = createBatchRequest(3);
        BatchExecutionResponseDTO expectedResponse = createPartialSuccessResponse(3, 2, 1);
        ResponseEntity<BatchExecutionResponseDTO> responseEntity = 
            new ResponseEntity<>(expectedResponse, HttpStatus.MULTI_STATUS);
        
        when(retryContext.getRetryCount()).thenReturn(0);
        when(retryTemplate.execute(any())).thenAnswer(invocation -> {
            return invocation.getArgument(0, org.springframework.retry.RetryCallback.class).doWithRetry(retryContext);
        });
        when(restTemplate.postForEntity(eq(BATCH_URL), eq(request), eq(BatchExecutionResponseDTO.class)))
            .thenReturn(responseEntity);
        
        // Act
        BatchExecutionResponseDTO result = executionServiceClient.submitBatch(request);
        
        // Assert
        assertNotNull(result);
        assertEquals("PARTIAL_SUCCESS", result.getStatus());
        assertEquals(3, result.getTotalRequested());
        assertEquals(2, result.getSuccessful());
        assertEquals(1, result.getFailed());
        
        verify(restTemplate).postForEntity(BATCH_URL, request, BatchExecutionResponseDTO.class);
    }
    
    @Test
    void testSubmitBatch_BadRequest_Http400() throws Exception {
        // Arrange
        BatchExecutionRequestDTO request = createBatchRequest(1);
        HttpClientErrorException badRequestException = 
            new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid execution data");
        
        when(retryContext.getRetryCount()).thenReturn(0);
        when(retryTemplate.execute(any())).thenAnswer(invocation -> {
            return invocation.getArgument(0, org.springframework.retry.RetryCallback.class).doWithRetry(retryContext);
        });
        when(restTemplate.postForEntity(eq(BATCH_URL), eq(request), eq(BatchExecutionResponseDTO.class)))
            .thenThrow(badRequestException);
        
        // Act
        BatchExecutionResponseDTO result = executionServiceClient.submitBatch(request);
        
        // Assert
        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());
        assertEquals(1, result.getTotalRequested());
        assertEquals(0, result.getSuccessful());
        assertEquals(1, result.getFailed());
        assertTrue(result.getMessage().contains("Bad request"));
        
        verify(restTemplate).postForEntity(BATCH_URL, request, BatchExecutionResponseDTO.class);
    }
    
    @Test
    void testSubmitBatch_ServerError_Http500_RetriesAndFails() throws Exception {
        // Arrange
        BatchExecutionRequestDTO request = createBatchRequest(1);
        HttpServerErrorException serverException = 
            new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server error");
        
        when(retryTemplate.execute(any())).thenThrow(serverException);
        
        // Act & Assert
        ExecutionServiceClient.ExecutionServiceException exception = assertThrows(
            ExecutionServiceClient.ExecutionServiceException.class,
            () -> executionServiceClient.submitBatch(request)
        );
        
        assertTrue(exception.getMessage().contains("Failed to submit batch execution after retries"));
        assertEquals(serverException, exception.getCause());
        
        verify(retryTemplate).execute(any());
    }
    
    @Test
    void testSubmitBatch_NetworkTimeout_RetriesAndFails() throws Exception {
        // Arrange
        BatchExecutionRequestDTO request = createBatchRequest(1);
        ResourceAccessException timeoutException = 
            new ResourceAccessException("Connection timeout");
        
        when(retryTemplate.execute(any())).thenThrow(timeoutException);
        
        // Act & Assert
        ExecutionServiceClient.ExecutionServiceException exception = assertThrows(
            ExecutionServiceClient.ExecutionServiceException.class,
            () -> executionServiceClient.submitBatch(request)
        );
        
        assertTrue(exception.getMessage().contains("Failed to submit batch execution after retries"));
        assertEquals(timeoutException, exception.getCause());
        
        verify(retryTemplate).execute(any());
    }
    
    @Test
    void testSubmitBatch_NullResponse_ThrowsException() throws Exception {
        // Arrange
        BatchExecutionRequestDTO request = createBatchRequest(1);
        ResponseEntity<BatchExecutionResponseDTO> responseEntity = 
            new ResponseEntity<>(null, HttpStatus.CREATED);
        
        when(retryContext.getRetryCount()).thenReturn(0);
        when(retryTemplate.execute(any())).thenAnswer(invocation -> {
            return invocation.getArgument(0, org.springframework.retry.RetryCallback.class).doWithRetry(retryContext);
        });
        when(restTemplate.postForEntity(eq(BATCH_URL), eq(request), eq(BatchExecutionResponseDTO.class)))
            .thenReturn(responseEntity);
        
        // Act & Assert
        ExecutionServiceClient.ExecutionServiceException exception = assertThrows(
            ExecutionServiceClient.ExecutionServiceException.class,
            () -> executionServiceClient.submitBatch(request)
        );
        
        assertTrue(exception.getMessage().contains("Failed to submit batch execution after retries"));
        assertTrue(exception.getCause().getMessage().contains("Received null response from Execution Service"));
        
        verify(restTemplate).postForEntity(BATCH_URL, request, BatchExecutionResponseDTO.class);
    }
    
    @Test
    void testSubmitBatch_NullRequest_ThrowsIllegalArgumentException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> executionServiceClient.submitBatch(null)
        );
        
        assertEquals("Batch request cannot be null or empty", exception.getMessage());
        
        verifyNoInteractions(restTemplate, retryTemplate);
    }
    
    @Test
    void testSubmitBatch_EmptyExecutions_ThrowsIllegalArgumentException() {
        // Arrange
        BatchExecutionRequestDTO request = new BatchExecutionRequestDTO(Collections.emptyList());
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> executionServiceClient.submitBatch(request)
        );
        
        assertEquals("Batch request cannot be null or empty", exception.getMessage());
        
        verifyNoInteractions(restTemplate, retryTemplate);
    }
    
    @Test
    void testSubmitBatch_NullExecutionsList_ThrowsIllegalArgumentException() {
        // Arrange
        BatchExecutionRequestDTO request = new BatchExecutionRequestDTO(null);
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> executionServiceClient.submitBatch(request)
        );
        
        assertEquals("Batch request cannot be null or empty", exception.getMessage());
        
        verifyNoInteractions(restTemplate, retryTemplate);
    }
    
    @Test
    void testSubmitBatch_OtherClientError_Http401_ThrowsException() throws Exception {
        // Arrange
        BatchExecutionRequestDTO request = createBatchRequest(1);
        HttpClientErrorException unauthorizedException = 
            new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        
        when(retryContext.getRetryCount()).thenReturn(0);
        when(retryTemplate.execute(any())).thenAnswer(invocation -> {
            return invocation.getArgument(0, org.springframework.retry.RetryCallback.class).doWithRetry(retryContext);
        });
        when(restTemplate.postForEntity(eq(BATCH_URL), eq(request), eq(BatchExecutionResponseDTO.class)))
            .thenThrow(unauthorizedException);
        
        // Act & Assert
        ExecutionServiceClient.ExecutionServiceException exception = assertThrows(
            ExecutionServiceClient.ExecutionServiceException.class,
            () -> executionServiceClient.submitBatch(request)
        );
        
        assertTrue(exception.getMessage().contains("Failed to submit batch execution after retries"));
        assertTrue(exception.getCause().getMessage().contains("Client error: HTTP 401"));
        
        verify(restTemplate).postForEntity(BATCH_URL, request, BatchExecutionResponseDTO.class);
    }
    
    @Test
    void testSubmitBatch_UnexpectedStatusCode() throws Exception {
        // Arrange
        BatchExecutionRequestDTO request = createBatchRequest(1);
        BatchExecutionResponseDTO expectedResponse = createSuccessfulResponse(1, 1, 0);
        ResponseEntity<BatchExecutionResponseDTO> responseEntity = 
            new ResponseEntity<>(expectedResponse, HttpStatus.ACCEPTED); // HTTP 202
        
        when(retryContext.getRetryCount()).thenReturn(0);
        when(retryTemplate.execute(any())).thenAnswer(invocation -> {
            return invocation.getArgument(0, org.springframework.retry.RetryCallback.class).doWithRetry(retryContext);
        });
        when(restTemplate.postForEntity(eq(BATCH_URL), eq(request), eq(BatchExecutionResponseDTO.class)))
            .thenReturn(responseEntity);
        
        // Act
        BatchExecutionResponseDTO result = executionServiceClient.submitBatch(request);
        
        // Assert
        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals(1, result.getSuccessful());
        
        verify(retryTemplate).execute(any());
    }
    
    // Helper methods
    
    private BatchExecutionRequestDTO createBatchRequest(int count) {
        List<ExecutionPostDTO> executions = Collections.nCopies(count, createExecutionPostDTO());
        return new BatchExecutionRequestDTO(executions);
    }
    
    private ExecutionPostDTO createExecutionPostDTO() {
        ExecutionPostDTO dto = new ExecutionPostDTO();
        dto.setExecutionTimestamp(OffsetDateTime.now());
        dto.setExecutionStatusId(1);
        dto.setBlotterId(1);
        dto.setTradeTypeId(1);
        dto.setTradeOrderId(1);
        dto.setDestinationId(1);
        dto.setQuantityOrdered(new BigDecimal("100"));
        dto.setQuantityPlaced(new BigDecimal("100"));
        dto.setQuantityFilled(BigDecimal.ZERO);
        dto.setLimitPrice(new BigDecimal("50.00"));
        return dto;
    }
    
    private BatchExecutionResponseDTO createSuccessfulResponse(int total, int successful, int failed) {
        BatchExecutionResponseDTO response = new BatchExecutionResponseDTO();
        response.setStatus("SUCCESS");
        response.setMessage("All executions processed successfully");
        response.setTotalRequested(total);
        response.setSuccessful(successful);
        response.setFailed(failed);
        
        // Create successful results
        List<ExecutionResultDTO> results = Collections.nCopies(successful, createSuccessfulResult());
        response.setResults(results);
        
        return response;
    }
    
    private BatchExecutionResponseDTO createPartialSuccessResponse(int total, int successful, int failed) {
        BatchExecutionResponseDTO response = new BatchExecutionResponseDTO();
        response.setStatus("PARTIAL_SUCCESS");
        response.setMessage("Some executions failed");
        response.setTotalRequested(total);
        response.setSuccessful(successful);
        response.setFailed(failed);
        
        // Create mixed results
        List<ExecutionResultDTO> results = Arrays.asList(
            createSuccessfulResult(),
            createSuccessfulResult(),
            createFailedResult()
        );
        response.setResults(results);
        
        return response;
    }
    
    private ExecutionResultDTO createSuccessfulResult() {
        ExecutionResultDTO result = new ExecutionResultDTO();
        result.setRequestIndex(0);
        result.setStatus("SUCCESS");
        result.setMessage("Execution processed successfully");
        
        ExecutionResponseDTO execution = new ExecutionResponseDTO();
        execution.setId(123);
        execution.setExecutionServiceId(456);
        result.setExecution(execution);
        
        return result;
    }
    
    private ExecutionResultDTO createFailedResult() {
        ExecutionResultDTO result = new ExecutionResultDTO();
        result.setRequestIndex(2);
        result.setStatus("FAILED");
        result.setMessage("Invalid security ID");
        result.setExecution(null);
        
        return result;
    }
}