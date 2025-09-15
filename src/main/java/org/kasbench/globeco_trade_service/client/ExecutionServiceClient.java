package org.kasbench.globeco_trade_service.client;

import org.kasbench.globeco_trade_service.dto.ExecutionServiceBatchRequestDTO;
import org.kasbench.globeco_trade_service.dto.ExecutionServiceBatchResponseDTO;
import org.kasbench.globeco_trade_service.dto.ExecutionServiceResultDTO;
import org.kasbench.globeco_trade_service.dto.ExecutionServiceResponseDTO;
import org.kasbench.globeco_trade_service.dto.BatchExecutionResponseDTO;
import org.kasbench.globeco_trade_service.dto.ExecutionResultDTO;
import org.kasbench.globeco_trade_service.dto.ExecutionResponseDTO;
import org.kasbench.globeco_trade_service.service.BulkExecutionErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Client for making bulk execution submission calls to the Execution Service
 * API.
 * Handles batch submissions using the POST /api/v1/executions/batch endpoint
 * with comprehensive error handling and retry logic.
 */
@Component
public class ExecutionServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionServiceClient.class);

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    private final String executionServiceBaseUrl;
    private final BulkExecutionErrorHandler errorHandler;

    public ExecutionServiceClient(
            @Qualifier("executionServiceRestTemplate") RestTemplate restTemplate,
            @Qualifier("executionServiceRetryTemplate") RetryTemplate retryTemplate,
            @Value("${execution.service.base-url:http://globeco-execution-service:8084}") String executionServiceBaseUrl,
            BulkExecutionErrorHandler errorHandler) {
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
        this.executionServiceBaseUrl = executionServiceBaseUrl;
        this.errorHandler = errorHandler;
    }

    /**
     * Submits a batch of executions to the Execution Service API.
     * 
     * @param request The batch execution request containing up to 100 executions
     * @return BatchExecutionResponseDTO containing the results of the batch
     *         submission
     * @throws ExecutionServiceException if the submission fails after all retries
     */
    public BatchExecutionResponseDTO submitBatch(ExecutionServiceBatchRequestDTO request) {
        if (request == null || request.getExecutions() == null || request.getExecutions().isEmpty()) {
            throw new IllegalArgumentException("Batch request cannot be null or empty");
        }

        int batchSize = request.getExecutions().size();
        List<Integer> executionIds = extractExecutionIds(request);

        logger.debug("Starting batch execution submission for {} executions: {}",
                batchSize, executionIds.size() <= 10 ? executionIds : executionIds.subList(0, 5) + "...");

        long startTime = System.currentTimeMillis();
        Map<String, Object> executionContext = errorHandler.createExecutionContext(executionIds, batchSize, 1);

        try {
            BatchExecutionResponseDTO response = retryTemplate.execute(context -> {
                int attemptNumber = context.getRetryCount() + 1;
                logger.debug("Attempting batch execution submission (attempt {}) for executions: {}",
                        attemptNumber, executionIds.size() <= 5 ? executionIds : executionIds.size() + " executions");

                // Update attempt number in context
                Map<String, Object> attemptContext = errorHandler.createExecutionContext(executionIds, batchSize,
                        attemptNumber);
                return executeSubmitBatch(request, batchSize, executionIds, attemptContext);
            });

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Batch execution submission completed successfully in {} ms for {} executions",
                    duration, batchSize);

            logBatchResults(response, batchSize, executionIds);
            return response;

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;

            // Check if this is already an ExecutionServiceException with ErrorInfo
            if (ex instanceof ExecutionServiceException && ((ExecutionServiceException) ex).hasErrorInfo()) {
                // Re-throw the original exception with its ErrorInfo intact
                logger.error("Batch execution submission failed after {} ms for {} executions: {}",
                        duration, batchSize, ex.getMessage());
                throw (ExecutionServiceException) ex;
            }

            // Map exception to detailed error information
            BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(ex, executionContext);

            // Log detailed error information
            errorHandler.logError(errorInfo, executionIds, batchSize);

            // Log performance impact
            logger.error("Batch execution submission failed after {} ms for {} executions. Error: [{}] {}",
                    duration, batchSize, errorInfo.getErrorCode(), errorInfo.getMessage());

            throw new ExecutionServiceException(
                    String.format("Failed to submit batch execution after retries: [%s] %s",
                            errorInfo.getErrorCode(), errorInfo.getMessage()),
                    ex,
                    errorInfo);
        }
    }

    /**
     * Executes the actual batch submission API call.
     */
    private BatchExecutionResponseDTO executeSubmitBatch(ExecutionServiceBatchRequestDTO request, int batchSize,
            List<Integer> executionIds, Map<String, Object> context) {
        String url = executionServiceBaseUrl + "/api/v1/executions/batch";

        long apiCallStartTime = System.currentTimeMillis();

        try {
            logger.debug("Making API call to {} for executions: {}", url,
                    executionIds.size() <= 5 ? executionIds : executionIds.size() + " executions");

            // Log the full payload at INFO level for debugging execution service format
            logger.debug("BULK_EXECUTION_PAYLOAD: Sending batch request to {} with {} executions", url, batchSize);
            if (request.getExecutions() != null) {
                for (int i = 0; i < request.getExecutions().size(); i++) {
                    var execution = request.getExecutions().get(i);
                    logger.debug(
                            "PAYLOAD_EXECUTION[{}]: ExecutionStatus={}, TradeType={}, Destination={}, SecurityId={}, Quantity={}, LimitPrice={}, Version={}",
                            i, execution.getExecutionStatus(), execution.getTradeType(), execution.getDestination(),
                            execution.getSecurityId(), execution.getQuantity(), execution.getLimitPrice(),
                            execution.getVersion());
                }
            }

            ResponseEntity<ExecutionServiceBatchResponseDTO> response = restTemplate.postForEntity(
                    url, request, ExecutionServiceBatchResponseDTO.class);

            long apiCallDuration = System.currentTimeMillis() - apiCallStartTime;
            logger.debug("Execution Service API call completed in {} ms for batch of {} executions",
                    apiCallDuration, batchSize);

            return handleResponse(response, batchSize, executionIds);

        } catch (HttpClientErrorException ex) {
            long apiCallDuration = System.currentTimeMillis() - apiCallStartTime;

            // Map and log client error with context
            BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(ex, context);
            logger.warn("Execution Service API call failed with client error in {} ms: [{}] {} - HTTP {} - {}",
                    apiCallDuration, errorInfo.getErrorCode(), errorInfo.getMessage(),
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());

            return handleClientError(ex, batchSize, executionIds, errorInfo);

        } catch (HttpServerErrorException ex) {
            long apiCallDuration = System.currentTimeMillis() - apiCallStartTime;

            // Map and log server error with context
            BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(ex, context);
            logger.warn("Execution Service API call failed with server error in {} ms: [{}] {} - HTTP {} - {}",
                    apiCallDuration, errorInfo.getErrorCode(), errorInfo.getMessage(),
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());

            // Server errors should be retried by the retry template
            throw ex;

        } catch (ResourceAccessException ex) {
            long apiCallDuration = System.currentTimeMillis() - apiCallStartTime;

            // Map and log network error with context
            BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(ex, context);
            logger.warn("Execution Service API call failed with network error in {} ms: [{}] {} - {}",
                    apiCallDuration, errorInfo.getErrorCode(), errorInfo.getMessage(), ex.getMessage());

            // Network errors should be retried by the retry template
            throw ex;
        } catch (Exception ex) {
            long apiCallDuration = System.currentTimeMillis() - apiCallStartTime;

            // Map and log unexpected errors
            BulkExecutionErrorHandler.ErrorInfo errorInfo = errorHandler.mapException(ex, context);
            logger.error("Execution Service API call failed with unexpected error in {} ms: [{}] {} - {}",
                    apiCallDuration, errorInfo.getErrorCode(), errorInfo.getMessage(), ex.getMessage());

            throw ex;
        }
    }

    /**
     * Handles successful HTTP responses based on status code.
     */
    private BatchExecutionResponseDTO handleResponse(ResponseEntity<ExecutionServiceBatchResponseDTO> response,
            int batchSize, List<Integer> executionIds) {
        HttpStatus statusCode = (HttpStatus) response.getStatusCode();
        ExecutionServiceBatchResponseDTO body = response.getBody();

        if (body == null) {
            String errorMsg = String.format(
                    "Received null response body from Execution Service for batch of %d executions: %s",
                    batchSize, executionIds);
            logger.error(errorMsg);
            throw new ExecutionServiceException(errorMsg);
        }

        // Convert execution service response to internal format
        BatchExecutionResponseDTO internalResponse = convertToInternalResponse(body);

        switch (statusCode) {
            case CREATED: // HTTP 201 - All executions successful
                logger.debug("All {} executions in batch submitted successfully (HTTP 201): {}",
                        batchSize, executionIds.size() <= 10 ? executionIds : executionIds.size() + " executions");
                return internalResponse;

            case MULTI_STATUS: // HTTP 207 - Partial success
                logger.debug(
                        "Batch submission completed with partial success (HTTP 207): {} successful, {} failed for executions: {}",
                        body.getSuccessful(), body.getFailed(),
                        executionIds.size() <= 10 ? executionIds : executionIds.size() + " executions");

                // Log details about failed executions if available
                if (body.getResults() != null && body.getFailed() > 0) {
                    logFailedExecutionDetails(body, executionIds);
                }
                return internalResponse;

            default:
                logger.warn("Unexpected HTTP status code {} for batch submission of executions: {}",
                        statusCode.value(), executionIds);
                return internalResponse;
        }
    }

    /**
     * Handles HTTP 4xx client errors.
     */
    private BatchExecutionResponseDTO handleClientError(HttpClientErrorException ex, int batchSize,
            List<Integer> executionIds,
            BulkExecutionErrorHandler.ErrorInfo errorInfo) {
        HttpStatus statusCode = (HttpStatus) ex.getStatusCode();

        if (statusCode == HttpStatus.BAD_REQUEST) {
            // HTTP 400 - All executions failed due to bad request
            logger.error("All {} executions in batch failed due to bad request (HTTP 400) - Executions: {} - Error: {}",
                    batchSize, executionIds, ex.getResponseBodyAsString());

            // Create a response indicating all executions failed
            BatchExecutionResponseDTO errorResponse = new BatchExecutionResponseDTO();
            errorResponse.setStatus("FAILED");
            errorResponse.setMessage(String.format("[%s] %s - Response: %s",
                    errorInfo.getErrorCode(),
                    errorInfo.getMessage(),
                    ex.getResponseBodyAsString()));
            errorResponse.setTotalRequested(batchSize);
            errorResponse.setSuccessful(0);
            errorResponse.setFailed(batchSize);

            return errorResponse;
        } else {
            // Other 4xx errors should not be retried
            String errorMsg = String.format(
                    "Batch submission failed with client error [%s] %s - HTTP %d - Executions: %s",
                    errorInfo.getErrorCode(), errorInfo.getMessage(),
                    statusCode.value(), executionIds);
            logger.error(errorMsg + " - Response: {}", ex.getResponseBodyAsString());
            throw new ExecutionServiceException(errorMsg, ex, errorInfo);
        }
    }

    /**
     * Logs detailed batch submission results for monitoring and debugging.
     */
    private void logBatchResults(BatchExecutionResponseDTO response, int batchSize, List<Integer> executionIds) {
        if (response == null) {
            logger.warn("Cannot log batch results - response is null for executions: {}", executionIds);
            return;
        }

        logger.debug("Batch execution results - Status: {}, Total: {}, Successful: {}, Failed: {} - Executions: {}",
                response.getStatus(),
                response.getTotalRequested(),
                response.getSuccessful(),
                response.getFailed(),
                executionIds.size() <= 10 ? executionIds : executionIds.size() + " executions");

        if (response.getFailed() != null && response.getFailed() > 0) {
            logger.warn("{} executions failed in batch submission for executions: {}",
                    response.getFailed(), executionIds);

            // Log additional failure details if available
            if (response.getResults() != null) {
                logFailedExecutionDetailsInternal(response, executionIds);
            }
        }

        if (response.getResults() != null) {
            logger.debug("Batch response contains {} individual execution results for executions: {}",
                    response.getResults().size(), executionIds);
        }

        // Structured logging for monitoring
        logger.debug("BULK_EXECUTION_BATCH_METRICS: batch_size={}, successful={}, failed={}, execution_ids_count={}",
                batchSize, response.getSuccessful(), response.getFailed(), executionIds.size());
    }

    /**
     * Converts execution service response format to internal response format.
     */
    private BatchExecutionResponseDTO convertToInternalResponse(ExecutionServiceBatchResponseDTO serviceResponse) {
        BatchExecutionResponseDTO internalResponse = new BatchExecutionResponseDTO();

        internalResponse.setStatus(serviceResponse.getStatus());
        internalResponse.setMessage(serviceResponse.getMessage());
        internalResponse.setTotalRequested(serviceResponse.getTotalRequested());
        internalResponse.setSuccessful(serviceResponse.getSuccessful());
        internalResponse.setFailed(serviceResponse.getFailed());

        // Convert results if present
        if (serviceResponse.getResults() != null) {
            List<ExecutionResultDTO> internalResults = serviceResponse.getResults().stream()
                    .map(this::convertToInternalResult)
                    .collect(Collectors.toList());
            internalResponse.setResults(internalResults);
        }

        return internalResponse;
    }

    /**
     * Converts execution service result to internal result format.
     */
    private ExecutionResultDTO convertToInternalResult(ExecutionServiceResultDTO serviceResult) {
        ExecutionResultDTO internalResult = new ExecutionResultDTO();

        internalResult.setRequestIndex(serviceResult.getRequestIndex());
        internalResult.setStatus(serviceResult.getStatus());
        internalResult.setMessage(serviceResult.getMessage());

        // Convert execution if present
        if (serviceResult.getExecution() != null) {
            ExecutionResponseDTO internalExecution = convertToInternalExecution(serviceResult.getExecution());
            internalResult.setExecution(internalExecution);
        }

        return internalResult;
    }

    /**
     * Converts execution service execution to internal execution format.
     */
    private ExecutionResponseDTO convertToInternalExecution(ExecutionServiceResponseDTO serviceExecution) {
        ExecutionResponseDTO internalExecution = new ExecutionResponseDTO();

        internalExecution.setId(serviceExecution.getId());
        internalExecution.setExecutionTimestamp(serviceExecution.getReceivedTimestamp());
        internalExecution.setQuantityOrdered(serviceExecution.getQuantity());
        internalExecution.setQuantityPlaced(serviceExecution.getQuantity());
        internalExecution.setQuantityFilled(serviceExecution.getQuantityFilled());
        internalExecution.setLimitPrice(serviceExecution.getLimitPrice());
        internalExecution.setVersion(serviceExecution.getVersion());
        internalExecution.setExecutionServiceId(serviceExecution.getId());

        // Note: We're not converting the string fields back to objects here
        // since the ExecutionBatchProcessor will handle the mapping differently

        return internalExecution;
    }

    /**
     * Logs detailed information about failed executions in a batch (execution
     * service format).
     */
    private void logFailedExecutionDetails(ExecutionServiceBatchResponseDTO response, List<Integer> executionIds) {
        if (response.getResults() == null || response.getResults().isEmpty()) {
            return;
        }

        response.getResults().stream()
                .filter(result -> "FAILED".equals(result.getStatus()))
                .forEach(result -> {
                    Integer executionId = null;
                    if (result.getRequestIndex() != null && result.getRequestIndex() < executionIds.size()) {
                        executionId = executionIds.get(result.getRequestIndex());
                    }

                    logger.warn("EXECUTION_FAILURE_DETAIL: execution_id={}, request_index={}, status={}, message={}",
                            executionId, result.getRequestIndex(), result.getStatus(), result.getMessage());
                });
    }

    /**
     * Logs detailed information about failed executions in a batch (internal
     * format).
     */
    private void logFailedExecutionDetailsInternal(BatchExecutionResponseDTO response, List<Integer> executionIds) {
        if (response.getResults() == null || response.getResults().isEmpty()) {
            return;
        }

        response.getResults().stream()
                .filter(result -> "FAILED".equals(result.getStatus()))
                .forEach(result -> {
                    Integer executionId = null;
                    if (result.getRequestIndex() != null && result.getRequestIndex() < executionIds.size()) {
                        executionId = executionIds.get(result.getRequestIndex());
                    }

                    logger.warn("EXECUTION_FAILURE_DETAIL: execution_id={}, request_index={}, status={}, message={}",
                            executionId, result.getRequestIndex(), result.getStatus(), result.getMessage());
                });
    }

    /**
     * Extracts execution IDs from the batch request for logging and error tracking.
     */
    private List<Integer> extractExecutionIds(ExecutionServiceBatchRequestDTO request) {
        if (request == null || request.getExecutions() == null) {
            return List.of();
        }

        return request.getExecutions().stream()
                .map(execution -> execution.getSecurityId()) // Use security ID as identifier
                .filter(securityId -> securityId != null)
                .map(securityId -> securityId.hashCode()) // Convert string to int for logging
                .collect(Collectors.toList());
    }

    /**
     * Custom exception for Execution Service API failures.
     */
    public static class ExecutionServiceException extends RuntimeException {
        private final BulkExecutionErrorHandler.ErrorInfo errorInfo;

        public ExecutionServiceException(String message) {
            super(message);
            this.errorInfo = null;
        }

        public ExecutionServiceException(String message, Throwable cause) {
            super(message, cause);
            this.errorInfo = null;
        }

        public ExecutionServiceException(String message, Throwable cause,
                BulkExecutionErrorHandler.ErrorInfo errorInfo) {
            super(message, cause);
            this.errorInfo = errorInfo;
        }

        public BulkExecutionErrorHandler.ErrorInfo getErrorInfo() {
            return errorInfo;
        }

        public boolean hasErrorInfo() {
            return errorInfo != null;
        }
    }
}