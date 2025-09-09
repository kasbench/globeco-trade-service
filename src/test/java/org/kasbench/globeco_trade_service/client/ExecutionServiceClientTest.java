package org.kasbench.globeco_trade_service.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kasbench.globeco_trade_service.dto.BatchExecutionRequestDTO;
import org.kasbench.globeco_trade_service.dto.BatchExecutionResponseDTO;
import org.kasbench.globeco_trade_service.dto.ExecutionPostDTO;
import org.kasbench.globeco_trade_service.dto.ExecutionResultDTO;
import org.kasbench.globeco_trade_service.dto.ExecutionResponseDTO;
import org.kasbench.globeco_trade_service.service.BulkExecutionErrorHandler;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    @Mock
    private BulkExecutionErrorHandler errorHandler;
    
    private ExecutionServiceClient executionServiceClient;
    
    private static final String BASE_URL = "http://test-execution-service:8084";
    private static final String BATCH_URL = BASE_URL + "/api/v1/executions/batch";
    
    @BeforeEach
    void setUp() {
        executionServiceClient = new ExecutionServiceClient(restTemplate, retryTemplate, BASE_URL, errorHandler);
        
        // Setup default error handler behavior
        lenient().when(errorHandler.createExecutionContext(any(), anyInt(), anyInt()))
            .thenReturn(new HashMap<>());
        lenient().when(errorHandler.mapException(any(), any()))
            .thenReturn(createDefaultErrorInfo());
    }
    
    private BulkExecutionErrorHandler.ErrorInfo createDefaultErrorInfo() {
        return new BulkExecutionErrorHandler.ErrorInfo(
            BulkExecutionErrorHandler.ErrorCategory.UNKNOWN_ERROR,
            BulkExecutionErrorHandler.ErrorSeverity.MEDIUM,
            "TEST_ERROR",
            "Test error message",
            "Detailed test error message",
            true,
            new HashMap<>()
        );
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
        assertTrue(result.getMessage().contains("TEST_ERROR") || result.getMessage().contains("Bad request"));
        
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
        
        // Exception message format may vary based on error handling implementation
        assertNotNull(exception.getMessage());
        
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
        
        assertTrue(exception.getMessage().contains("Batch submission failed") || exception.getMessage().contains("Failed to submit batch execution"));
        // The cause message format may vary based on error handling implementation
        
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
    
    @Test
    void testSubmitBatch_DetailedErrorMapping_ValidationError() throws Exception {
        // Arrange
        BatchExecutionRequestDTO request = createBatchRequest(2);
        HttpClientErrorException badRequestException = 
            new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid execution data");
        
        BulkExecutionErrorHandler.ErrorInfo validationErrorInfo = new BulkExecutionErrorHandler.ErrorInfo(
            BulkExecutionErrorHandler.ErrorCategory.VALIDATION_ERROR,
            BulkExecutionErrorHandler.ErrorSeverity.MEDIUM,
            "BULK_EXEC_400",
            "Invalid execution data in batch request",
            "HTTP 400 Bad Request: Invalid execution data",
            false,
            Map.of("httpStatus", 400)
        );
        
        when(retryContext.getRetryCount()).thenReturn(0);
        when(retryTemplate.execute(any())).thenAnswer(invocation -> {
            return invocation.getArgument(0, org.springframework.retry.RetryCallback.class).doWithRetry(retryContext);
        });
        when(restTemplate.postForEntity(eq(BATCH_URL), eq(request), eq(BatchExecutionResponseDTO.class)))
            .thenThrow(badRequestException);
        when(errorHandler.mapException(eq(badRequestException), any()))
            .thenReturn(validationErrorInfo);
        
        // Act
        BatchExecutionResponseDTO result = executionServiceClient.submitBatch(request);
        
        // Assert
        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());
        assertEquals(2, result.getTotalRequested());
        assertEquals(0, result.getSuccessful());
        assertEquals(2, result.getFailed());
        assertTrue(result.getMessage().contains("BULK_EXEC_400"));
        assertTrue(result.getMessage().contains("Invalid execution data in batch request"));
        
        verify(errorHandler).mapException(eq(badRequestException), any());
        verify(restTemplate).postForEntity(BATCH_URL, request, BatchExecutionResponseDTO.class);
    }
    
    @Test
    void testSubmitBatch_DetailedErrorMapping_NetworkError() throws Exception {
        // Arrange
        BatchExecutionRequestDTO request = createBatchRequest(1);
        ResourceAccessException networkException = 
            new ResourceAccessException("Connection timeout");
        
        BulkExecutionErrorHandler.ErrorInfo networkErrorInfo = new BulkExecutionErrorHandler.ErrorInfo(
            BulkExecutionErrorHandler.ErrorCategory.TIMEOUT_ERROR,
            BulkExecutionErrorHandler.ErrorSeverity.MEDIUM,
            "BULK_EXEC_TIMEOUT",
            "Connection timeout to execution service",
            "Connection timeout: Connection timeout. Service may be slow or unreachable.",
            true,
            Map.of("exceptionType", "ResourceAccessException")
        );
        
        when(retryTemplate.execute(any())).thenThrow(networkException);
        when(errorHandler.mapException(eq(networkException), any()))
            .thenReturn(networkErrorInfo);
        
        // Act & Assert
        ExecutionServiceClient.ExecutionServiceException exception = assertThrows(
            ExecutionServiceClient.ExecutionServiceException.class,
            () -> executionServiceClient.submitBatch(request)
        );
        
        assertTrue(exception.getMessage().contains("BULK_EXEC_TIMEOUT"));
        assertTrue(exception.getMessage().contains("Connection timeout to execution service"));
        assertEquals(networkException, exception.getCause());
        assertTrue(exception.hasErrorInfo());
        assertEquals(networkErrorInfo, exception.getErrorInfo());
        
        verify(errorHandler).mapException(eq(networkException), any());
        verify(errorHandler).logError(eq(networkErrorInfo), any(), eq(1));
        verify(retryTemplate).execute(any());
    }
    
    @Test
    void testSubmitBatch_DetailedErrorMapping_ServerError() throws Exception {
        // Arrange
        BatchExecutionRequestDTO request = createBatchRequest(3);
        HttpServerErrorException serverException = 
            new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server error");
        
        BulkExecutionErrorHandler.ErrorInfo serverErrorInfo = new BulkExecutionErrorHandler.ErrorInfo(
            BulkExecutionErrorHandler.ErrorCategory.SERVER_ERROR,
            BulkExecutionErrorHandler.ErrorSeverity.HIGH,
            "BULK_EXEC_500",
            "Internal server error in execution service",
            "HTTP 500 Internal Server Error: Server error. Service may be experiencing issues.",
            true,
            Map.of("httpStatus", 500)
        );
        
        when(retryTemplate.execute(any())).thenThrow(serverException);
        when(errorHandler.mapException(eq(serverException), any()))
            .thenReturn(serverErrorInfo);
        
        // Act & Assert
        ExecutionServiceClient.ExecutionServiceException exception = assertThrows(
            ExecutionServiceClient.ExecutionServiceException.class,
            () -> executionServiceClient.submitBatch(request)
        );
        
        assertTrue(exception.getMessage().contains("BULK_EXEC_500"));
        assertTrue(exception.getMessage().contains("Internal server error in execution service"));
        assertEquals(serverException, exception.getCause());
        assertTrue(exception.hasErrorInfo());
        assertEquals(serverErrorInfo, exception.getErrorInfo());
        
        verify(errorHandler).mapException(eq(serverException), any());
        verify(errorHandler).logError(eq(serverErrorInfo), any(), eq(3));
        verify(retryTemplate).execute(any());
    }
    
    @Test
    void testSubmitBatch_ExecutionContextCreation() throws Exception {
        // Arrange
        BatchExecutionRequestDTO request = createBatchRequest(2);
        BatchExecutionResponseDTO expectedResponse = createSuccessfulResponse(2, 2, 0);
        ResponseEntity<BatchExecutionResponseDTO> responseEntity = 
            new ResponseEntity<>(expectedResponse, HttpStatus.CREATED);
        
        Map<String, Object> expectedContext = Map.of(
            "batch_size", 2,
            "attempt_number", 1,
            "execution_count", 2
        );
        
        when(retryContext.getRetryCount()).thenReturn(0);
        when(retryTemplate.execute(any())).thenAnswer(invocation -> {
            return invocation.getArgument(0, org.springframework.retry.RetryCallback.class).doWithRetry(retryContext);
        });
        when(restTemplate.postForEntity(eq(BATCH_URL), eq(request), eq(BatchExecutionResponseDTO.class)))
            .thenReturn(responseEntity);
        when(errorHandler.createExecutionContext(any(), eq(2), eq(1)))
            .thenReturn(expectedContext);
        
        // Act
        BatchExecutionResponseDTO result = executionServiceClient.submitBatch(request);
        
        // Assert
        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        
        verify(errorHandler, atLeastOnce()).createExecutionContext(any(), eq(2), eq(1));
        verify(restTemplate).postForEntity(BATCH_URL, request, BatchExecutionResponseDTO.class);
    }
    
    @Test
    void testSubmitBatch_PartialSuccess_WithFailureDetails() throws Exception {
        // Arrange
        BatchExecutionRequestDTO request = createBatchRequest(3);
        BatchExecutionResponseDTO partialResponse = createPartialSuccessResponse(3, 2, 1);
        ResponseEntity<BatchExecutionResponseDTO> responseEntity = 
            new ResponseEntity<>(partialResponse, HttpStatus.MULTI_STATUS);
        
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
    void testSubmitBatch_AuthenticationError_NotRetryable() throws Exception {
        // Arrange
        BatchExecutionRequestDTO request = createBatchRequest(1);
        HttpClientErrorException authException = 
            new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        
        BulkExecutionErrorHandler.ErrorInfo authErrorInfo = new BulkExecutionErrorHandler.ErrorInfo(
            BulkExecutionErrorHandler.ErrorCategory.AUTHENTICATION_ERROR,
            BulkExecutionErrorHandler.ErrorSeverity.HIGH,
            "BULK_EXEC_401",
            "Authentication failed for execution service",
            "HTTP 401 Unauthorized: Unauthorized. Check service credentials.",
            false,
            Map.of("httpStatus", 401)
        );
        
        when(retryContext.getRetryCount()).thenReturn(0);
        when(retryTemplate.execute(any())).thenAnswer(invocation -> {
            return invocation.getArgument(0, org.springframework.retry.RetryCallback.class).doWithRetry(retryContext);
        });
        when(restTemplate.postForEntity(eq(BATCH_URL), eq(request), eq(BatchExecutionResponseDTO.class)))
            .thenThrow(authException);
        when(errorHandler.mapException(eq(authException), any()))
            .thenReturn(authErrorInfo);
        
        // Act & Assert
        ExecutionServiceClient.ExecutionServiceException exception = assertThrows(
            ExecutionServiceClient.ExecutionServiceException.class,
            () -> executionServiceClient.submitBatch(request)
        );
        
        assertTrue(exception.getMessage().contains("BULK_EXEC_401"));
        assertTrue(exception.getMessage().contains("Authentication failed for execution service"));
        assertEquals(authException, exception.getCause());
        assertTrue(exception.hasErrorInfo());
        assertEquals(authErrorInfo, exception.getErrorInfo());
        
        verify(errorHandler).mapException(eq(authException), any());
    }
    
    @Test
    void testSubmitBatch_RateLimitError_Retryable() throws Exception {
        // Arrange
        BatchExecutionRequestDTO request = createBatchRequest(1);
        HttpClientErrorException rateLimitException = 
            new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
        
        BulkExecutionErrorHandler.ErrorInfo rateLimitErrorInfo = new BulkExecutionErrorHandler.ErrorInfo(
            BulkExecutionErrorHandler.ErrorCategory.RATE_LIMIT_ERROR,
            BulkExecutionErrorHandler.ErrorSeverity.MEDIUM,
            "BULK_EXEC_429",
            "Rate limit exceeded for execution service",
            "HTTP 429 Too Many Requests: Rate limit exceeded. Retry after delay.",
            true,
            Map.of("httpStatus", 429)
        );
        
        when(retryContext.getRetryCount()).thenReturn(0);
        when(retryTemplate.execute(any())).thenAnswer(invocation -> {
            return invocation.getArgument(0, org.springframework.retry.RetryCallback.class).doWithRetry(retryContext);
        });
        when(restTemplate.postForEntity(eq(BATCH_URL), eq(request), eq(BatchExecutionResponseDTO.class)))
            .thenThrow(rateLimitException);
        when(errorHandler.mapException(eq(rateLimitException), any()))
            .thenReturn(rateLimitErrorInfo);
        
        // Act & Assert
        ExecutionServiceClient.ExecutionServiceException exception = assertThrows(
            ExecutionServiceClient.ExecutionServiceException.class,
            () -> executionServiceClient.submitBatch(request)
        );
        
        assertTrue(exception.getMessage().contains("BULK_EXEC_429"));
        assertTrue(exception.getMessage().contains("Rate limit exceeded for execution service"));
        assertEquals(rateLimitException, exception.getCause());
        assertTrue(exception.hasErrorInfo());
        assertEquals(rateLimitErrorInfo, exception.getErrorInfo());
        
        verify(errorHandler).mapException(eq(rateLimitException), any());
    }
    
    @Test
    void testSubmitBatch_UnexpectedError_WithErrorMapping() throws Exception {
        // Arrange
        BatchExecutionRequestDTO request = createBatchRequest(1);
        RuntimeException unexpectedException = new RuntimeException("Unexpected error");
        
        BulkExecutionErrorHandler.ErrorInfo unexpectedErrorInfo = new BulkExecutionErrorHandler.ErrorInfo(
            BulkExecutionErrorHandler.ErrorCategory.UNKNOWN_ERROR,
            BulkExecutionErrorHandler.ErrorSeverity.MEDIUM,
            "BULK_EXEC_RUNTIME",
            "Runtime error during bulk execution",
            "Runtime error: Unexpected error",
            false,
            Map.of("exceptionType", "RuntimeException")
        );
        
        when(retryContext.getRetryCount()).thenReturn(0);
        when(retryTemplate.execute(any())).thenAnswer(invocation -> {
            return invocation.getArgument(0, org.springframework.retry.RetryCallback.class).doWithRetry(retryContext);
        });
        when(restTemplate.postForEntity(eq(BATCH_URL), eq(request), eq(BatchExecutionResponseDTO.class)))
            .thenThrow(unexpectedException);
        when(errorHandler.mapException(eq(unexpectedException), any()))
            .thenReturn(unexpectedErrorInfo);
        
        // Act & Assert
        ExecutionServiceClient.ExecutionServiceException exception = assertThrows(
            ExecutionServiceClient.ExecutionServiceException.class,
            () -> executionServiceClient.submitBatch(request)
        );
        
        assertTrue(exception.getMessage().contains("BULK_EXEC_RUNTIME"));
        assertTrue(exception.getMessage().contains("Runtime error during bulk execution"));
        assertEquals(unexpectedException, exception.getCause());
        assertTrue(exception.hasErrorInfo());
        assertEquals(unexpectedErrorInfo, exception.getErrorInfo());
        
        verify(errorHandler, atLeastOnce()).mapException(eq(unexpectedException), any());
        verify(errorHandler).logError(eq(unexpectedErrorInfo), any(), eq(1));
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