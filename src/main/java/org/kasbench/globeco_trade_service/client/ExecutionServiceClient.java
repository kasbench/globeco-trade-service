package org.kasbench.globeco_trade_service.client;

import org.kasbench.globeco_trade_service.dto.BatchExecutionRequestDTO;
import org.kasbench.globeco_trade_service.dto.BatchExecutionResponseDTO;
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

/**
 * Client for making bulk execution submission calls to the Execution Service API.
 * Handles batch submissions using the POST /api/v1/executions/batch endpoint
 * with comprehensive error handling and retry logic.
 */
@Component
public class ExecutionServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(ExecutionServiceClient.class);
    
    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    private final String executionServiceBaseUrl;
    
    public ExecutionServiceClient(
            @Qualifier("executionServiceRestTemplate") RestTemplate restTemplate,
            @Qualifier("executionServiceRetryTemplate") RetryTemplate retryTemplate,
            @Value("${execution.service.base-url:http://globeco-execution-service:8084}") String executionServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
        this.executionServiceBaseUrl = executionServiceBaseUrl;
    }
    
    /**
     * Submits a batch of executions to the Execution Service API.
     * 
     * @param request The batch execution request containing up to 100 executions
     * @return BatchExecutionResponseDTO containing the results of the batch submission
     * @throws ExecutionServiceException if the submission fails after all retries
     */
    public BatchExecutionResponseDTO submitBatch(BatchExecutionRequestDTO request) {
        if (request == null || request.getExecutions() == null || request.getExecutions().isEmpty()) {
            throw new IllegalArgumentException("Batch request cannot be null or empty");
        }
        
        int batchSize = request.getExecutions().size();
        logger.info("Starting batch execution submission for {} executions", batchSize);
        
        long startTime = System.currentTimeMillis();
        
        try {
            BatchExecutionResponseDTO response = retryTemplate.execute(context -> {
                logger.debug("Attempting batch execution submission (attempt {})", context.getRetryCount() + 1);
                return executeSubmitBatch(request, batchSize);
            });
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Batch execution submission completed successfully in {} ms for {} executions", 
                       duration, batchSize);
            
            logBatchResults(response, batchSize);
            return response;
            
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Batch execution submission failed after {} ms for {} executions: {}", 
                        duration, batchSize, ex.getMessage(), ex);
            throw new ExecutionServiceException("Failed to submit batch execution after retries", ex);
        }
    }
    
    /**
     * Executes the actual batch submission API call.
     */
    private BatchExecutionResponseDTO executeSubmitBatch(BatchExecutionRequestDTO request, int batchSize) {
        String url = executionServiceBaseUrl + "/api/v1/executions/batch";
        
        long apiCallStartTime = System.currentTimeMillis();
        
        try {
            ResponseEntity<BatchExecutionResponseDTO> response = restTemplate.postForEntity(
                url, request, BatchExecutionResponseDTO.class);
            
            long apiCallDuration = System.currentTimeMillis() - apiCallStartTime;
            logger.info("Execution Service API call completed in {} ms for batch of {} executions", 
                       apiCallDuration, batchSize);
            
            return handleResponse(response, batchSize);
            
        } catch (HttpClientErrorException ex) {
            long apiCallDuration = System.currentTimeMillis() - apiCallStartTime;
            logger.warn("Execution Service API call failed with client error in {} ms: HTTP {} - {}", 
                       apiCallDuration, ex.getStatusCode().value(), ex.getResponseBodyAsString());
            
            return handleClientError(ex, batchSize);
            
        } catch (HttpServerErrorException ex) {
            long apiCallDuration = System.currentTimeMillis() - apiCallStartTime;
            logger.warn("Execution Service API call failed with server error in {} ms: HTTP {} - {}", 
                       apiCallDuration, ex.getStatusCode().value(), ex.getResponseBodyAsString());
            
            // Server errors should be retried by the retry template
            throw ex;
            
        } catch (ResourceAccessException ex) {
            long apiCallDuration = System.currentTimeMillis() - apiCallStartTime;
            logger.warn("Execution Service API call failed with network error in {} ms: {}", 
                       apiCallDuration, ex.getMessage());
            
            // Network errors should be retried by the retry template
            throw ex;
        }
    }
    
    /**
     * Handles successful HTTP responses based on status code.
     */
    private BatchExecutionResponseDTO handleResponse(ResponseEntity<BatchExecutionResponseDTO> response, int batchSize) {
        HttpStatus statusCode = (HttpStatus) response.getStatusCode();
        BatchExecutionResponseDTO body = response.getBody();
        
        if (body == null) {
            logger.error("Received null response body from Execution Service for batch of {} executions", batchSize);
            throw new ExecutionServiceException("Received null response from Execution Service");
        }
        
        switch (statusCode) {
            case CREATED: // HTTP 201 - All executions successful
                logger.info("All {} executions in batch submitted successfully (HTTP 201)", batchSize);
                return body;
                
            case MULTI_STATUS: // HTTP 207 - Partial success
                logger.info("Batch submission completed with partial success (HTTP 207): {} successful, {} failed", 
                           body.getSuccessful(), body.getFailed());
                return body;
                
            default:
                logger.warn("Unexpected HTTP status code {} for batch submission", statusCode.value());
                return body;
        }
    }
    
    /**
     * Handles HTTP 4xx client errors.
     */
    private BatchExecutionResponseDTO handleClientError(HttpClientErrorException ex, int batchSize) {
        HttpStatus statusCode = (HttpStatus) ex.getStatusCode();
        
        if (statusCode == HttpStatus.BAD_REQUEST) {
            // HTTP 400 - All executions failed due to bad request
            logger.error("All {} executions in batch failed due to bad request (HTTP 400): {}", 
                        batchSize, ex.getResponseBodyAsString());
            
            // Create a response indicating all executions failed
            BatchExecutionResponseDTO errorResponse = new BatchExecutionResponseDTO();
            errorResponse.setStatus("FAILED");
            errorResponse.setMessage("Bad request: " + ex.getResponseBodyAsString());
            errorResponse.setTotalRequested(batchSize);
            errorResponse.setSuccessful(0);
            errorResponse.setFailed(batchSize);
            
            return errorResponse;
        } else {
            // Other 4xx errors should not be retried
            logger.error("Batch submission failed with client error HTTP {}: {}", 
                        statusCode.value(), ex.getResponseBodyAsString());
            throw new ExecutionServiceException("Client error: HTTP " + statusCode.value(), ex);
        }
    }
    
    /**
     * Logs detailed batch submission results for monitoring and debugging.
     */
    private void logBatchResults(BatchExecutionResponseDTO response, int batchSize) {
        if (response == null) {
            logger.warn("Cannot log batch results - response is null");
            return;
        }
        
        logger.info("Batch execution results - Status: {}, Total: {}, Successful: {}, Failed: {}", 
                   response.getStatus(), 
                   response.getTotalRequested(), 
                   response.getSuccessful(), 
                   response.getFailed());
        
        if (response.getFailed() != null && response.getFailed() > 0) {
            logger.warn("{} executions failed in batch submission", response.getFailed());
        }
        
        if (response.getResults() != null) {
            logger.debug("Batch response contains {} individual execution results", response.getResults().size());
        }
    }
    
    /**
     * Custom exception for Execution Service API failures.
     */
    public static class ExecutionServiceException extends RuntimeException {
        public ExecutionServiceException(String message) {
            super(message);
        }
        
        public ExecutionServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}