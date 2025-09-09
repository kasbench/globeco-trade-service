package org.kasbench.globeco_trade_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BulkExecutionErrorHandlerTest {

    private BulkExecutionErrorHandler errorHandler;

    @BeforeEach
    void setUp() {
        errorHandler = new BulkExecutionErrorHandler();
    }

    @Test
    void testMapException_HttpClientError_BadRequest() {
        // Arrange
        HttpClientErrorException exception = new HttpClientErrorException(
            HttpStatus.BAD_REQUEST, "Bad Request", "Invalid execution data".getBytes(), null);
        Map<String, Object> context = new HashMap<>();
        context.put("batch_size", 5);

        // Act
        BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(exception, context);

        // Assert
        assertEquals(BulkExecutionErrorHandler.ErrorCategory.VALIDATION_ERROR, errorInfo.getCategory());
        assertEquals(BulkExecutionErrorHandler.ErrorSeverity.MEDIUM, errorInfo.getSeverity());
        assertEquals("BULK_EXEC_400", errorInfo.getErrorCode());
        assertEquals("Invalid execution data in batch request", errorInfo.getMessage());
        assertFalse(errorInfo.isRetryable());
        assertTrue(errorInfo.getDetailedMessage().contains("HTTP 400 Bad Request"));
        assertEquals(400, errorInfo.getContext().get("httpStatus"));
    }

    @Test
    void testMapException_HttpClientError_Unauthorized() {
        // Arrange
        HttpClientErrorException exception = new HttpClientErrorException(
            HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication failed".getBytes(), null);
        Map<String, Object> context = new HashMap<>();

        // Act
        BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(exception, context);

        // Assert
        assertEquals(BulkExecutionErrorHandler.ErrorCategory.AUTHENTICATION_ERROR, errorInfo.getCategory());
        assertEquals(BulkExecutionErrorHandler.ErrorSeverity.HIGH, errorInfo.getSeverity());
        assertEquals("BULK_EXEC_401", errorInfo.getErrorCode());
        assertEquals("Authentication failed for execution service", errorInfo.getMessage());
        assertFalse(errorInfo.isRetryable());
        assertTrue(errorInfo.getDetailedMessage().contains("Check service credentials"));
    }

    @Test
    void testMapException_HttpClientError_Forbidden() {
        // Arrange
        HttpClientErrorException exception = new HttpClientErrorException(
            HttpStatus.FORBIDDEN, "Forbidden", "Access denied".getBytes(), null);
        Map<String, Object> context = new HashMap<>();

        // Act
        BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(exception, context);

        // Assert
        assertEquals(BulkExecutionErrorHandler.ErrorCategory.AUTHORIZATION_ERROR, errorInfo.getCategory());
        assertEquals(BulkExecutionErrorHandler.ErrorSeverity.HIGH, errorInfo.getSeverity());
        assertEquals("BULK_EXEC_403", errorInfo.getErrorCode());
        assertEquals("Insufficient permissions for bulk execution submission", errorInfo.getMessage());
        assertFalse(errorInfo.isRetryable());
        assertTrue(errorInfo.getDetailedMessage().contains("Check service permissions"));
    }

    @Test
    void testMapException_HttpClientError_NotFound() {
        // Arrange
        HttpClientErrorException exception = new HttpClientErrorException(
            HttpStatus.NOT_FOUND, "Not Found", "Endpoint not found".getBytes(), null);
        Map<String, Object> context = new HashMap<>();

        // Act
        BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(exception, context);

        // Assert
        assertEquals(BulkExecutionErrorHandler.ErrorCategory.CLIENT_ERROR, errorInfo.getCategory());
        assertEquals(BulkExecutionErrorHandler.ErrorSeverity.MEDIUM, errorInfo.getSeverity());
        assertEquals("BULK_EXEC_404", errorInfo.getErrorCode());
        assertEquals("Execution service endpoint not found", errorInfo.getMessage());
        assertFalse(errorInfo.isRetryable());
        assertTrue(errorInfo.getDetailedMessage().contains("Check service URL configuration"));
    }

    @Test
    void testMapException_HttpClientError_TooManyRequests() {
        // Arrange
        HttpClientErrorException exception = new HttpClientErrorException(
            HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", "Rate limit exceeded".getBytes(), null);
        Map<String, Object> context = new HashMap<>();

        // Act
        BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(exception, context);

        // Assert
        assertEquals(BulkExecutionErrorHandler.ErrorCategory.RATE_LIMIT_ERROR, errorInfo.getCategory());
        assertEquals(BulkExecutionErrorHandler.ErrorSeverity.MEDIUM, errorInfo.getSeverity());
        assertEquals("BULK_EXEC_429", errorInfo.getErrorCode());
        assertEquals("Rate limit exceeded for execution service", errorInfo.getMessage());
        assertTrue(errorInfo.isRetryable());
        assertTrue(errorInfo.getDetailedMessage().contains("Retry after delay"));
    }

    @Test
    void testMapException_HttpServerError_InternalServerError() {
        // Arrange
        HttpServerErrorException exception = new HttpServerErrorException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Server error".getBytes(), null);
        Map<String, Object> context = new HashMap<>();

        // Act
        BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(exception, context);

        // Assert
        assertEquals(BulkExecutionErrorHandler.ErrorCategory.SERVER_ERROR, errorInfo.getCategory());
        assertEquals(BulkExecutionErrorHandler.ErrorSeverity.HIGH, errorInfo.getSeverity());
        assertEquals("BULK_EXEC_500", errorInfo.getErrorCode());
        assertEquals("Internal server error in execution service", errorInfo.getMessage());
        assertTrue(errorInfo.isRetryable());
        assertTrue(errorInfo.getDetailedMessage().contains("Service may be experiencing issues"));
    }

    @Test
    void testMapException_HttpServerError_ServiceUnavailable() {
        // Arrange
        HttpServerErrorException exception = new HttpServerErrorException(
            HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", "Service down".getBytes(), null);
        Map<String, Object> context = new HashMap<>();

        // Act
        BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(exception, context);

        // Assert
        assertEquals(BulkExecutionErrorHandler.ErrorCategory.SERVICE_UNAVAILABLE_ERROR, errorInfo.getCategory());
        assertEquals(BulkExecutionErrorHandler.ErrorSeverity.HIGH, errorInfo.getSeverity());
        assertEquals("BULK_EXEC_503", errorInfo.getErrorCode());
        assertEquals("Execution service temporarily unavailable", errorInfo.getMessage());
        assertTrue(errorInfo.isRetryable());
        assertTrue(errorInfo.getDetailedMessage().contains("Service is temporarily down"));
    }

    @Test
    void testMapException_HttpServerError_GatewayTimeout() {
        // Arrange
        HttpServerErrorException exception = new HttpServerErrorException(
            HttpStatus.GATEWAY_TIMEOUT, "Gateway Timeout", "Timeout".getBytes(), null);
        Map<String, Object> context = new HashMap<>();

        // Act
        BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(exception, context);

        // Assert
        assertEquals(BulkExecutionErrorHandler.ErrorCategory.TIMEOUT_ERROR, errorInfo.getCategory());
        assertEquals(BulkExecutionErrorHandler.ErrorSeverity.MEDIUM, errorInfo.getSeverity());
        assertEquals("BULK_EXEC_504", errorInfo.getErrorCode());
        assertEquals("Gateway timeout from execution service", errorInfo.getMessage());
        assertTrue(errorInfo.isRetryable());
        assertTrue(errorInfo.getDetailedMessage().contains("Upstream service timeout"));
    }

    @Test
    void testMapException_ResourceAccessError_Timeout() {
        // Arrange
        ResourceAccessException exception = new ResourceAccessException("Connection timeout occurred");
        Map<String, Object> context = new HashMap<>();

        // Act
        BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(exception, context);

        // Assert
        assertEquals(BulkExecutionErrorHandler.ErrorCategory.TIMEOUT_ERROR, errorInfo.getCategory());
        assertEquals(BulkExecutionErrorHandler.ErrorSeverity.MEDIUM, errorInfo.getSeverity());
        assertEquals("BULK_EXEC_TIMEOUT", errorInfo.getErrorCode());
        assertEquals("Connection timeout to execution service", errorInfo.getMessage());
        assertTrue(errorInfo.isRetryable());
        assertTrue(errorInfo.getDetailedMessage().contains("Service may be slow or unreachable"));
        assertEquals("ResourceAccessException", errorInfo.getContext().get("exceptionType"));
    }

    @Test
    void testMapException_ResourceAccessError_Connection() {
        // Arrange
        ResourceAccessException exception = new ResourceAccessException("Connection refused");
        Map<String, Object> context = new HashMap<>();

        // Act
        BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(exception, context);

        // Assert
        assertEquals(BulkExecutionErrorHandler.ErrorCategory.NETWORK_ERROR, errorInfo.getCategory());
        assertEquals(BulkExecutionErrorHandler.ErrorSeverity.MEDIUM, errorInfo.getSeverity());
        assertEquals("BULK_EXEC_CONN", errorInfo.getErrorCode());
        assertEquals("Connection error to execution service", errorInfo.getMessage());
        assertTrue(errorInfo.isRetryable());
        assertTrue(errorInfo.getDetailedMessage().contains("Network or service connectivity issue"));
    }

    @Test
    void testMapException_TimeoutException() {
        // Arrange
        TimeoutException exception = new TimeoutException("Operation timed out");
        Map<String, Object> context = new HashMap<>();

        // Act
        BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(exception, context);

        // Assert
        assertEquals(BulkExecutionErrorHandler.ErrorCategory.TIMEOUT_ERROR, errorInfo.getCategory());
        assertEquals(BulkExecutionErrorHandler.ErrorSeverity.MEDIUM, errorInfo.getSeverity());
        assertEquals("BULK_EXEC_TIMEOUT", errorInfo.getErrorCode());
        assertEquals("Operation timeout during bulk execution submission", errorInfo.getMessage());
        assertTrue(errorInfo.isRetryable());
        assertTrue(errorInfo.getDetailedMessage().contains("Operation took too long to complete"));
    }

    @Test
    void testMapException_RestClientException() {
        // Arrange
        RestClientException exception = new RestClientException("REST client error");
        Map<String, Object> context = new HashMap<>();

        // Act
        BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(exception, context);

        // Assert
        assertEquals(BulkExecutionErrorHandler.ErrorCategory.CLIENT_ERROR, errorInfo.getCategory());
        assertEquals(BulkExecutionErrorHandler.ErrorSeverity.MEDIUM, errorInfo.getSeverity());
        assertEquals("BULK_EXEC_REST", errorInfo.getErrorCode());
        assertEquals("REST client error during bulk execution submission", errorInfo.getMessage());
        assertTrue(errorInfo.isRetryable());
    }

    @Test
    void testMapException_RuntimeError_Validation() {
        // Arrange
        RuntimeException exception = new RuntimeException("Validation error: invalid data");
        Map<String, Object> context = new HashMap<>();

        // Act
        BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(exception, context);

        // Assert
        assertEquals(BulkExecutionErrorHandler.ErrorCategory.VALIDATION_ERROR, errorInfo.getCategory());
        assertEquals(BulkExecutionErrorHandler.ErrorSeverity.MEDIUM, errorInfo.getSeverity());
        assertEquals("BULK_EXEC_VALIDATION", errorInfo.getErrorCode());
        assertEquals("Data validation error during bulk execution", errorInfo.getMessage());
        assertFalse(errorInfo.isRetryable());
        assertTrue(errorInfo.getDetailedMessage().contains("Validation error"));
    }

    @Test
    void testMapException_RuntimeError_Timeout() {
        // Arrange
        RuntimeException exception = new RuntimeException("Operation timed out");
        Map<String, Object> context = new HashMap<>();

        // Act
        BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(exception, context);

        // Assert
        assertEquals(BulkExecutionErrorHandler.ErrorCategory.TIMEOUT_ERROR, errorInfo.getCategory());
        assertEquals(BulkExecutionErrorHandler.ErrorSeverity.MEDIUM, errorInfo.getSeverity());
        assertEquals("BULK_EXEC_RUNTIME_TIMEOUT", errorInfo.getErrorCode());
        assertEquals("Runtime timeout during bulk execution", errorInfo.getMessage());
        assertTrue(errorInfo.isRetryable());
    }

    @Test
    void testMapException_RuntimeError_Network() {
        // Arrange
        RuntimeException exception = new RuntimeException("Network connection failed");
        Map<String, Object> context = new HashMap<>();

        // Act
        BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(exception, context);

        // Assert
        assertEquals(BulkExecutionErrorHandler.ErrorCategory.NETWORK_ERROR, errorInfo.getCategory());
        assertEquals(BulkExecutionErrorHandler.ErrorSeverity.MEDIUM, errorInfo.getSeverity());
        assertEquals("BULK_EXEC_RUNTIME_NETWORK", errorInfo.getErrorCode());
        assertEquals("Network error during bulk execution", errorInfo.getMessage());
        assertTrue(errorInfo.isRetryable());
    }

    @Test
    void testMapException_RuntimeError_Generic() {
        // Arrange
        RuntimeException exception = new RuntimeException("Some other runtime error");
        Map<String, Object> context = new HashMap<>();

        // Act
        BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(exception, context);

        // Assert
        assertEquals(BulkExecutionErrorHandler.ErrorCategory.UNKNOWN_ERROR, errorInfo.getCategory());
        assertEquals(BulkExecutionErrorHandler.ErrorSeverity.MEDIUM, errorInfo.getSeverity());
        assertEquals("BULK_EXEC_RUNTIME", errorInfo.getErrorCode());
        assertEquals("Runtime error during bulk execution", errorInfo.getMessage());
        assertFalse(errorInfo.isRetryable());
    }

    @Test
    void testMapException_GenericException() {
        // Arrange
        Exception exception = new Exception("Generic exception");
        Map<String, Object> context = new HashMap<>();

        // Act
        BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(exception, context);

        // Assert
        assertEquals(BulkExecutionErrorHandler.ErrorCategory.UNKNOWN_ERROR, errorInfo.getCategory());
        assertEquals(BulkExecutionErrorHandler.ErrorSeverity.MEDIUM, errorInfo.getSeverity());
        assertEquals("BULK_EXEC_GENERIC", errorInfo.getErrorCode());
        assertEquals("Unexpected error during bulk execution submission", errorInfo.getMessage());
        assertFalse(errorInfo.isRetryable());
        assertEquals("Exception", errorInfo.getContext().get("exceptionType"));
    }

    @Test
    void testMapException_NullException() {
        // Arrange
        Map<String, Object> context = new HashMap<>();

        // Act
        BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(null, context);

        // Assert
        assertEquals(BulkExecutionErrorHandler.ErrorCategory.UNKNOWN_ERROR, errorInfo.getCategory());
        assertEquals(BulkExecutionErrorHandler.ErrorSeverity.MEDIUM, errorInfo.getSeverity());
        assertEquals("BULK_EXEC_UNKNOWN", errorInfo.getErrorCode());
        assertEquals("Unknown error during bulk execution", errorInfo.getMessage());
        assertFalse(errorInfo.isRetryable());
        assertTrue(errorInfo.getDetailedMessage().contains("Null exception provided"));
    }

    @Test
    void testCreateExecutionContext_SmallBatch() {
        // Arrange
        List<Integer> executionIds = Arrays.asList(1, 2, 3);
        int batchSize = 3;
        int attemptNumber = 2;

        // Act
        Map<String, Object> context = errorHandler.createExecutionContext(executionIds, batchSize, attemptNumber);

        // Assert
        assertEquals(3, context.get("batch_size"));
        assertEquals(2, context.get("attempt_number"));
        assertEquals(3, context.get("execution_count"));
        assertEquals(1, context.get("first_execution_id"));
        assertEquals(3, context.get("last_execution_id"));
        assertEquals(executionIds, context.get("execution_ids"));
        assertNotNull(context.get("timestamp"));
    }

    @Test
    void testCreateExecutionContext_LargeBatch() {
        // Arrange
        List<Integer> executionIds = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        int batchSize = 12;
        int attemptNumber = 1;

        // Act
        Map<String, Object> context = errorHandler.createExecutionContext(executionIds, batchSize, attemptNumber);

        // Assert
        assertEquals(12, context.get("batch_size"));
        assertEquals(1, context.get("attempt_number"));
        assertEquals(12, context.get("execution_count"));
        assertEquals(1, context.get("first_execution_id"));
        assertEquals(12, context.get("last_execution_id"));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), context.get("execution_ids_sample"));
        assertEquals(12, context.get("total_execution_ids"));
        assertNull(context.get("execution_ids")); // Should not include full list for large batches
    }

    @Test
    void testCreateExecutionContext_NullExecutionIds() {
        // Arrange
        int batchSize = 5;
        int attemptNumber = 1;

        // Act
        Map<String, Object> context = errorHandler.createExecutionContext(null, batchSize, attemptNumber);

        // Assert
        assertEquals(5, context.get("batch_size"));
        assertEquals(1, context.get("attempt_number"));
        assertEquals(0, context.get("execution_count"));
        assertNull(context.get("first_execution_id"));
        assertNull(context.get("last_execution_id"));
        assertNull(context.get("execution_ids"));
    }

    @Test
    void testShouldRetry_RetryableError_WithinLimits() {
        // Arrange
        BulkExecutionErrorHandler.ErrorInfo errorInfo = new BulkExecutionErrorHandler.ErrorInfo(
            BulkExecutionErrorHandler.ErrorCategory.TIMEOUT_ERROR,
            BulkExecutionErrorHandler.ErrorSeverity.MEDIUM,
            "BULK_EXEC_TIMEOUT",
            "Timeout error",
            "Detailed timeout error",
            true,
            new HashMap<>()
        );

        // Act & Assert
        assertTrue(errorHandler.shouldRetry(errorInfo, 1, 3));
        assertTrue(errorHandler.shouldRetry(errorInfo, 2, 3));
        assertFalse(errorHandler.shouldRetry(errorInfo, 3, 3));
    }

    @Test
    void testShouldRetry_NonRetryableError() {
        // Arrange
        BulkExecutionErrorHandler.ErrorInfo errorInfo = new BulkExecutionErrorHandler.ErrorInfo(
            BulkExecutionErrorHandler.ErrorCategory.VALIDATION_ERROR,
            BulkExecutionErrorHandler.ErrorSeverity.MEDIUM,
            "BULK_EXEC_VALIDATION",
            "Validation error",
            "Detailed validation error",
            false,
            new HashMap<>()
        );

        // Act & Assert
        assertFalse(errorHandler.shouldRetry(errorInfo, 1, 3));
    }

    @Test
    void testShouldRetry_RateLimitError() {
        // Arrange
        BulkExecutionErrorHandler.ErrorInfo errorInfo = new BulkExecutionErrorHandler.ErrorInfo(
            BulkExecutionErrorHandler.ErrorCategory.RATE_LIMIT_ERROR,
            BulkExecutionErrorHandler.ErrorSeverity.MEDIUM,
            "BULK_EXEC_429",
            "Rate limit error",
            "Detailed rate limit error",
            true,
            new HashMap<>()
        );

        // Act & Assert
        assertTrue(errorHandler.shouldRetry(errorInfo, 1, 3));
        assertTrue(errorHandler.shouldRetry(errorInfo, 2, 3));
        assertFalse(errorHandler.shouldRetry(errorInfo, 3, 3)); // At max attempts, no more retries
    }

    @Test
    void testShouldRetry_ServerError_LimitedRetries() {
        // Arrange
        BulkExecutionErrorHandler.ErrorInfo errorInfo = new BulkExecutionErrorHandler.ErrorInfo(
            BulkExecutionErrorHandler.ErrorCategory.SERVER_ERROR,
            BulkExecutionErrorHandler.ErrorSeverity.HIGH,
            "BULK_EXEC_500",
            "Server error",
            "Detailed server error",
            true,
            new HashMap<>()
        );

        // Act & Assert
        assertTrue(errorHandler.shouldRetry(errorInfo, 0, 5)); // Attempt 0 < 2
        assertTrue(errorHandler.shouldRetry(errorInfo, 1, 5)); // Attempt 1 < 2
        assertFalse(errorHandler.shouldRetry(errorInfo, 2, 5)); // Attempt 2 >= 2, no more retries
    }

    @Test
    void testShouldRetry_AuthenticationError() {
        // Arrange
        BulkExecutionErrorHandler.ErrorInfo errorInfo = new BulkExecutionErrorHandler.ErrorInfo(
            BulkExecutionErrorHandler.ErrorCategory.AUTHENTICATION_ERROR,
            BulkExecutionErrorHandler.ErrorSeverity.HIGH,
            "BULK_EXEC_401",
            "Authentication error",
            "Detailed authentication error",
            false,
            new HashMap<>()
        );

        // Act & Assert
        assertFalse(errorHandler.shouldRetry(errorInfo, 1, 3));
    }

    @Test
    void testShouldRetry_NullErrorInfo() {
        // Act & Assert
        assertFalse(errorHandler.shouldRetry(null, 1, 3));
    }

    @Test
    void testShouldRetry_ExceededMaxAttempts() {
        // Arrange
        BulkExecutionErrorHandler.ErrorInfo errorInfo = new BulkExecutionErrorHandler.ErrorInfo(
            BulkExecutionErrorHandler.ErrorCategory.TIMEOUT_ERROR,
            BulkExecutionErrorHandler.ErrorSeverity.MEDIUM,
            "BULK_EXEC_TIMEOUT",
            "Timeout error",
            "Detailed timeout error",
            true,
            new HashMap<>()
        );

        // Act & Assert
        assertFalse(errorHandler.shouldRetry(errorInfo, 5, 3));
    }

    @Test
    void testLogError_WithExecutionIds() {
        // Arrange
        List<Integer> executionIds = Arrays.asList(1, 2, 3);
        BulkExecutionErrorHandler.ErrorInfo errorInfo = new BulkExecutionErrorHandler.ErrorInfo(
            BulkExecutionErrorHandler.ErrorCategory.TIMEOUT_ERROR,
            BulkExecutionErrorHandler.ErrorSeverity.MEDIUM,
            "BULK_EXEC_TIMEOUT",
            "Timeout error",
            "Detailed timeout error",
            true,
            Map.of("additional_context", "test_value")
        );

        // Act - This should not throw any exceptions
        assertDoesNotThrow(() -> errorHandler.logError(errorInfo, executionIds, 3));
    }

    @Test
    void testLogError_NullErrorInfo() {
        // Arrange
        List<Integer> executionIds = Arrays.asList(1, 2, 3);

        // Act - This should not throw any exceptions
        assertDoesNotThrow(() -> errorHandler.logError(null, executionIds, 3));
    }

    @Test
    void testLogError_NullExecutionIds() {
        // Arrange
        BulkExecutionErrorHandler.ErrorInfo errorInfo = new BulkExecutionErrorHandler.ErrorInfo(
            BulkExecutionErrorHandler.ErrorCategory.TIMEOUT_ERROR,
            BulkExecutionErrorHandler.ErrorSeverity.MEDIUM,
            "BULK_EXEC_TIMEOUT",
            "Timeout error",
            "Detailed timeout error",
            true,
            new HashMap<>()
        );

        // Act - This should not throw any exceptions
        assertDoesNotThrow(() -> errorHandler.logError(errorInfo, null, 3));
    }
}