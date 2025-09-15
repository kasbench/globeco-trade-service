package org.kasbench.globeco_trade_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Component responsible for comprehensive error handling and mapping for bulk execution operations.
 * Provides detailed error classification, meaningful error messages, and proper logging context.
 */
@Component
public class BulkExecutionErrorHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(BulkExecutionErrorHandler.class);
    
    // Error categories for classification
    public enum ErrorCategory {
        NETWORK_ERROR,
        TIMEOUT_ERROR,
        CLIENT_ERROR,
        SERVER_ERROR,
        VALIDATION_ERROR,
        AUTHENTICATION_ERROR,
        AUTHORIZATION_ERROR,
        RATE_LIMIT_ERROR,
        SERVICE_UNAVAILABLE_ERROR,
        UNKNOWN_ERROR
    }
    
    // Error severity levels
    public enum ErrorSeverity {
        LOW,      // Transient errors that can be retried
        MEDIUM,   // Errors that may require investigation
        HIGH,     // Critical errors that need immediate attention
        CRITICAL  // System-level failures
    }
    
    /**
     * Comprehensive error information container
     */
    public static class ErrorInfo {
        private final ErrorCategory category;
        private final ErrorSeverity severity;
        private final String errorCode;
        private final String message;
        private final String detailedMessage;
        private final boolean retryable;
        private final Map<String, Object> context;
        
        public ErrorInfo(ErrorCategory category, ErrorSeverity severity, String errorCode, 
                        String message, String detailedMessage, boolean retryable, 
                        Map<String, Object> context) {
            this.category = category;
            this.severity = severity;
            this.errorCode = errorCode;
            this.message = message;
            this.detailedMessage = detailedMessage;
            this.retryable = retryable;
            this.context = context != null ? new HashMap<>(context) : new HashMap<>();
        }
        
        // Getters
        public ErrorCategory getCategory() { return category; }
        public ErrorSeverity getSeverity() { return severity; }
        public String getErrorCode() { return errorCode; }
        public String getMessage() { return message; }
        public String getDetailedMessage() { return detailedMessage; }
        public boolean isRetryable() { return retryable; }
        public Map<String, Object> getContext() { return new HashMap<>(context); }
    }
    
    /**
     * Maps exceptions to detailed error information with proper classification.
     * 
     * @param exception The exception to analyze
     * @param executionContext Additional context about the execution batch
     * @return Detailed error information
     */
    public ErrorInfo mapException(Exception exception, Map<String, Object> executionContext) {
        if (exception == null) {
            return createUnknownError("Null exception provided", executionContext);
        }
        
        logger.debug("Mapping exception: {} with message: {}", 
                    exception.getClass().getSimpleName(), exception.getMessage());
        
        // HTTP Client Errors (4xx)
        if (exception instanceof HttpClientErrorException) {
            return mapHttpClientError((HttpClientErrorException) exception, executionContext);
        }
        
        // HTTP Server Errors (5xx)
        if (exception instanceof HttpServerErrorException) {
            return mapHttpServerError((HttpServerErrorException) exception, executionContext);
        }
        
        // Network and timeout errors
        if (exception instanceof ResourceAccessException) {
            return mapResourceAccessError((ResourceAccessException) exception, executionContext);
        }
        
        // Timeout errors
        if (exception instanceof TimeoutException) {
            return mapTimeoutError((TimeoutException) exception, executionContext);
        }
        
        // General REST client errors
        if (exception instanceof RestClientException) {
            return mapRestClientError((RestClientException) exception, executionContext);
        }
        
        // Runtime exceptions with specific patterns
        if (exception instanceof RuntimeException) {
            return mapRuntimeError((RuntimeException) exception, executionContext);
        }
        
        // Generic exception mapping
        return mapGenericError(exception, executionContext);
    }
    
    /**
     * Maps HTTP 4xx client errors to detailed error information.
     */
    private ErrorInfo mapHttpClientError(HttpClientErrorException exception, Map<String, Object> context) {
        HttpStatus status = (HttpStatus) exception.getStatusCode();
        String responseBody = exception.getResponseBodyAsString();
        
        context.put("httpStatus", status.value());
        context.put("responseBody", responseBody);
        
        switch (status) {
            case BAD_REQUEST:
                return new ErrorInfo(
                    ErrorCategory.VALIDATION_ERROR,
                    ErrorSeverity.MEDIUM,
                    "BULK_EXEC_400",
                    "Invalid execution data in batch request",
                    String.format("HTTP 400 Bad Request: %s. Response: %s", 
                                exception.getMessage(), responseBody),
                    false, // Not retryable - data validation issue
                    context
                );
                
            case UNAUTHORIZED:
                return new ErrorInfo(
                    ErrorCategory.AUTHENTICATION_ERROR,
                    ErrorSeverity.HIGH,
                    "BULK_EXEC_401",
                    "Authentication failed for execution service",
                    String.format("HTTP 401 Unauthorized: %s. Check service credentials.", 
                                exception.getMessage()),
                    false, // Not retryable - auth issue
                    context
                );
                
            case FORBIDDEN:
                return new ErrorInfo(
                    ErrorCategory.AUTHORIZATION_ERROR,
                    ErrorSeverity.HIGH,
                    "BULK_EXEC_403",
                    "Insufficient permissions for bulk execution submission",
                    String.format("HTTP 403 Forbidden: %s. Check service permissions.", 
                                exception.getMessage()),
                    false, // Not retryable - permission issue
                    context
                );
                
            case NOT_FOUND:
                return new ErrorInfo(
                    ErrorCategory.CLIENT_ERROR,
                    ErrorSeverity.MEDIUM,
                    "BULK_EXEC_404",
                    "Execution service endpoint not found",
                    String.format("HTTP 404 Not Found: %s. Check service URL configuration.", 
                                exception.getMessage()),
                    false, // Not retryable - configuration issue
                    context
                );
                
            case TOO_MANY_REQUESTS:
                return new ErrorInfo(
                    ErrorCategory.RATE_LIMIT_ERROR,
                    ErrorSeverity.MEDIUM,
                    "BULK_EXEC_429",
                    "Rate limit exceeded for execution service",
                    String.format("HTTP 429 Too Many Requests: %s. Retry after delay.", 
                                exception.getMessage()),
                    true, // Retryable with backoff
                    context
                );
                
            default:
                return new ErrorInfo(
                    ErrorCategory.CLIENT_ERROR,
                    ErrorSeverity.MEDIUM,
                    "BULK_EXEC_4XX",
                    String.format("Client error: HTTP %d", status.value()),
                    String.format("HTTP %d %s: %s", status.value(), status.getReasonPhrase(), 
                                exception.getMessage()),
                    false, // Generally not retryable
                    context
                );
        }
    }
    
    /**
     * Maps HTTP 5xx server errors to detailed error information.
     */
    private ErrorInfo mapHttpServerError(HttpServerErrorException exception, Map<String, Object> context) {
        HttpStatus status = (HttpStatus) exception.getStatusCode();
        String responseBody = exception.getResponseBodyAsString();
        
        context.put("httpStatus", status.value());
        context.put("responseBody", responseBody);
        
        switch (status) {
            case INTERNAL_SERVER_ERROR:
                return new ErrorInfo(
                    ErrorCategory.SERVER_ERROR,
                    ErrorSeverity.HIGH,
                    "BULK_EXEC_500",
                    "Internal server error in execution service",
                    String.format("HTTP 500 Internal Server Error: %s. Service may be experiencing issues.", 
                                exception.getMessage()),
                    true, // Retryable - server issue
                    context
                );
                
            case BAD_GATEWAY:
                return new ErrorInfo(
                    ErrorCategory.SERVER_ERROR,
                    ErrorSeverity.HIGH,
                    "BULK_EXEC_502",
                    "Bad gateway error from execution service",
                    String.format("HTTP 502 Bad Gateway: %s. Upstream service issue.", 
                                exception.getMessage()),
                    true, // Retryable - gateway issue
                    context
                );
                
            case SERVICE_UNAVAILABLE:
                return new ErrorInfo(
                    ErrorCategory.SERVICE_UNAVAILABLE_ERROR,
                    ErrorSeverity.HIGH,
                    "BULK_EXEC_503",
                    "Execution service temporarily unavailable",
                    String.format("HTTP 503 Service Unavailable: %s. Service is temporarily down.", 
                                exception.getMessage()),
                    true, // Retryable - temporary unavailability
                    context
                );
                
            case GATEWAY_TIMEOUT:
                return new ErrorInfo(
                    ErrorCategory.TIMEOUT_ERROR,
                    ErrorSeverity.MEDIUM,
                    "BULK_EXEC_504",
                    "Gateway timeout from execution service",
                    String.format("HTTP 504 Gateway Timeout: %s. Upstream service timeout.", 
                                exception.getMessage()),
                    true, // Retryable - timeout issue
                    context
                );
                
            default:
                return new ErrorInfo(
                    ErrorCategory.SERVER_ERROR,
                    ErrorSeverity.HIGH,
                    "BULK_EXEC_5XX",
                    String.format("Server error: HTTP %d", status.value()),
                    String.format("HTTP %d %s: %s", status.value(), status.getReasonPhrase(), 
                                exception.getMessage()),
                    true, // Generally retryable
                    context
                );
        }
    }
    
    /**
     * Maps resource access errors (network, connection, timeout) to detailed error information.
     */
    private ErrorInfo mapResourceAccessError(ResourceAccessException exception, Map<String, Object> context) {
        String message = exception.getMessage();
        String lowerMessage = message != null ? message.toLowerCase() : "";
        
        context.put("exceptionType", "ResourceAccessException");
        context.put("rootCause", exception.getCause() != null ? exception.getCause().getClass().getSimpleName() : "Unknown");
        
        if (lowerMessage.contains("timeout") || lowerMessage.contains("timed out")) {
            return new ErrorInfo(
                ErrorCategory.TIMEOUT_ERROR,
                ErrorSeverity.MEDIUM,
                "BULK_EXEC_TIMEOUT",
                "Connection timeout to execution service",
                String.format("Connection timeout: %s. Service may be slow or unreachable.", message),
                true, // Retryable - timeout issue
                context
            );
        } else if (lowerMessage.contains("connection") || lowerMessage.contains("connect")) {
            return new ErrorInfo(
                ErrorCategory.NETWORK_ERROR,
                ErrorSeverity.MEDIUM,
                "BULK_EXEC_CONN",
                "Connection error to execution service",
                String.format("Connection error: %s. Network or service connectivity issue.", message),
                true, // Retryable - network issue
                context
            );
        } else {
            return new ErrorInfo(
                ErrorCategory.NETWORK_ERROR,
                ErrorSeverity.MEDIUM,
                "BULK_EXEC_RESOURCE",
                "Resource access error to execution service",
                String.format("Resource access error: %s", message),
                true, // Retryable - resource issue
                context
            );
        }
    }
    
    /**
     * Maps timeout exceptions to detailed error information.
     */
    private ErrorInfo mapTimeoutError(TimeoutException exception, Map<String, Object> context) {
        context.put("exceptionType", "TimeoutException");
        
        return new ErrorInfo(
            ErrorCategory.TIMEOUT_ERROR,
            ErrorSeverity.MEDIUM,
            "BULK_EXEC_TIMEOUT",
            "Operation timeout during bulk execution submission",
            String.format("Timeout error: %s. Operation took too long to complete.", 
                        exception.getMessage()),
            true, // Retryable - timeout issue
            context
        );
    }
    
    /**
     * Maps general REST client errors to detailed error information.
     */
    private ErrorInfo mapRestClientError(RestClientException exception, Map<String, Object> context) {
        context.put("exceptionType", "RestClientException");
        
        return new ErrorInfo(
            ErrorCategory.CLIENT_ERROR,
            ErrorSeverity.MEDIUM,
            "BULK_EXEC_REST",
            "REST client error during bulk execution submission",
            String.format("REST client error: %s", exception.getMessage()),
            true, // May be retryable depending on cause
            context
        );
    }
    
    /**
     * Maps runtime exceptions with pattern matching to detailed error information.
     */
    private ErrorInfo mapRuntimeError(RuntimeException exception, Map<String, Object> context) {
        String message = exception.getMessage();
        String lowerMessage = message != null ? message.toLowerCase() : "";
        
        context.put("exceptionType", "RuntimeException");
        context.put("exceptionClass", exception.getClass().getSimpleName());
        
        // Pattern matching for common runtime errors
        if (lowerMessage.contains("validation") || lowerMessage.contains("invalid")) {
            return new ErrorInfo(
                ErrorCategory.VALIDATION_ERROR,
                ErrorSeverity.MEDIUM,
                "BULK_EXEC_VALIDATION",
                "Data validation error during bulk execution",
                String.format("Validation error: %s", message),
                false, // Not retryable - data issue
                context
            );
        } else if (lowerMessage.contains("timeout") || lowerMessage.contains("timed out")) {
            return new ErrorInfo(
                ErrorCategory.TIMEOUT_ERROR,
                ErrorSeverity.MEDIUM,
                "BULK_EXEC_RUNTIME_TIMEOUT",
                "Runtime timeout during bulk execution",
                String.format("Runtime timeout: %s", message),
                true, // Retryable - timeout issue
                context
            );
        } else if (lowerMessage.contains("connection") || lowerMessage.contains("network")) {
            return new ErrorInfo(
                ErrorCategory.NETWORK_ERROR,
                ErrorSeverity.MEDIUM,
                "BULK_EXEC_RUNTIME_NETWORK",
                "Network error during bulk execution",
                String.format("Network error: %s", message),
                true, // Retryable - network issue
                context
            );
        } else {
            return new ErrorInfo(
                ErrorCategory.UNKNOWN_ERROR,
                ErrorSeverity.MEDIUM,
                "BULK_EXEC_RUNTIME",
                "Runtime error during bulk execution",
                String.format("Runtime error: %s", message),
                false, // Unknown - not retryable by default
                context
            );
        }
    }
    
    /**
     * Maps generic exceptions to detailed error information.
     */
    private ErrorInfo mapGenericError(Exception exception, Map<String, Object> context) {
        context.put("exceptionType", exception.getClass().getSimpleName());
        
        return new ErrorInfo(
            ErrorCategory.UNKNOWN_ERROR,
            ErrorSeverity.MEDIUM,
            "BULK_EXEC_GENERIC",
            "Unexpected error during bulk execution submission",
            String.format("Unexpected error: %s", exception.getMessage()),
            false, // Unknown - not retryable by default
            context
        );
    }
    
    /**
     * Creates error info for unknown/null exceptions.
     */
    private ErrorInfo createUnknownError(String reason, Map<String, Object> context) {
        return new ErrorInfo(
            ErrorCategory.UNKNOWN_ERROR,
            ErrorSeverity.MEDIUM,
            "BULK_EXEC_UNKNOWN",
            "Unknown error during bulk execution",
            String.format("Unknown error: %s", reason),
            false, // Unknown - not retryable
            context != null ? context : new HashMap<>()
        );
    }
    
    /**
     * Logs error information with appropriate level and context.
     * 
     * @param errorInfo The error information to log
     * @param executionIds List of execution IDs affected by the error
     * @param batchSize Size of the batch that failed
     */
    public void logError(ErrorInfo errorInfo, java.util.List<Integer> executionIds, int batchSize) {
        if (errorInfo == null) {
            logger.warn("Cannot log error - errorInfo is null");
            return;
        }
        
        String executionContext = String.format("batch_size=%d, execution_ids=%s", 
                                               batchSize, 
                                               executionIds != null ? executionIds.toString() : "null");
        
        String logMessage = String.format("[%s] %s - %s | Context: %s | Error Context: %s", 
                                        errorInfo.getErrorCode(),
                                        errorInfo.getMessage(),
                                        errorInfo.getDetailedMessage(),
                                        executionContext,
                                        errorInfo.getContext());
        
        // Log at appropriate level based on severity
        switch (errorInfo.getSeverity()) {
            case LOW:
                logger.debug(logMessage);
                break;
            case MEDIUM:
                logger.warn(logMessage);
                break;
            case HIGH:
                logger.error(logMessage);
                break;
            case CRITICAL:
                logger.error("CRITICAL ERROR: {}", logMessage);
                break;
            default:
                logger.warn(logMessage);
        }
        
        // Additional structured logging for monitoring systems
        logger.debug("BULK_EXECUTION_ERROR_METRICS: error_code={}, category={}, severity={}, retryable={}, batch_size={}, affected_executions={}", 
                   errorInfo.getErrorCode(),
                   errorInfo.getCategory(),
                   errorInfo.getSeverity(),
                   errorInfo.isRetryable(),
                   batchSize,
                   executionIds != null ? executionIds.size() : 0);
    }
    
    /**
     * Creates execution context map for error tracking.
     * 
     * @param executionIds List of execution IDs in the batch
     * @param batchSize Size of the batch
     * @param attemptNumber Current attempt number
     * @return Context map with execution details
     */
    public Map<String, Object> createExecutionContext(java.util.List<Integer> executionIds, 
                                                     int batchSize, 
                                                     int attemptNumber) {
        Map<String, Object> context = new HashMap<>();
        context.put("batch_size", batchSize);
        context.put("attempt_number", attemptNumber);
        context.put("execution_count", executionIds != null ? executionIds.size() : 0);
        context.put("timestamp", System.currentTimeMillis());
        
        if (executionIds != null && !executionIds.isEmpty()) {
            context.put("first_execution_id", executionIds.get(0));
            context.put("last_execution_id", executionIds.get(executionIds.size() - 1));
            
            // Include all IDs for small batches, sample for large batches
            if (executionIds.size() <= 10) {
                context.put("execution_ids", executionIds);
            } else {
                context.put("execution_ids_sample", executionIds.subList(0, 5));
                context.put("total_execution_ids", executionIds.size());
            }
        }
        
        return context;
    }
    
    /**
     * Determines if an error should trigger immediate failure or allow retries.
     * 
     * @param errorInfo The error information to evaluate
     * @param currentAttempt Current retry attempt number
     * @param maxAttempts Maximum allowed retry attempts
     * @return true if the operation should be retried, false otherwise
     */
    public boolean shouldRetry(ErrorInfo errorInfo, int currentAttempt, int maxAttempts) {
        if (errorInfo == null) {
            return false;
        }
        
        // Check if error is retryable
        if (!errorInfo.isRetryable()) {
            logger.debug("Error {} is not retryable, skipping retry", errorInfo.getErrorCode());
            return false;
        }
        
        // Additional logic based on error category
        switch (errorInfo.getCategory()) {
            case RATE_LIMIT_ERROR:
                // Always retry rate limit errors with backoff (within max attempts)
                return currentAttempt < maxAttempts;
                
            case TIMEOUT_ERROR:
            case NETWORK_ERROR:
            case SERVICE_UNAVAILABLE_ERROR:
                // Retry transient errors (within max attempts)
                return currentAttempt < maxAttempts;
                
            case SERVER_ERROR:
                // Retry server errors but with caution - limit to 2 attempts max
                return currentAttempt < Math.min(maxAttempts, 2);
                
            case VALIDATION_ERROR:
            case AUTHENTICATION_ERROR:
            case AUTHORIZATION_ERROR:
                // Never retry these errors
                return false;
                
            default:
                // Use the retryable flag for unknown categories (within max attempts)
                return errorInfo.isRetryable() && currentAttempt < maxAttempts;
        }
    }
}