package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.dto.BatchSubmitRequestDTO;
import org.kasbench.globeco_trade_service.dto.BatchSubmitResponseDTO;
import org.kasbench.globeco_trade_service.dto.ExecutionResponseDTO;
import org.kasbench.globeco_trade_service.dto.TradeOrderSubmitDTO;
import org.kasbench.globeco_trade_service.entity.Execution;
import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.kasbench.globeco_trade_service.repository.TradeOrderRepository;
import org.kasbench.globeco_trade_service.repository.ExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

@Service
public class BatchTradeOrderService {
    private static final Logger logger = LoggerFactory.getLogger(BatchTradeOrderService.class);
    private static final int MAX_BATCH_SIZE = 100;
    
    private final TradeOrderRepository tradeOrderRepository;
    private final TradeOrderService tradeOrderService;
    private final ExecutionRepository executionRepository;
    private final ExecutionService executionService;
    
    public BatchTradeOrderService(
            TradeOrderRepository tradeOrderRepository,
            TradeOrderService tradeOrderService,
            ExecutionRepository executionRepository,
            ExecutionService executionService) {
        this.tradeOrderRepository = tradeOrderRepository;
        this.tradeOrderService = tradeOrderService;
        this.executionRepository = executionRepository;
        this.executionService = executionService;
    }
    
    /**
     * Submit multiple trade orders in batch with default behavior (automatically submits to execution service)
     */
    @Transactional
    public BatchSubmitResponseDTO submitTradeOrdersBatch(BatchSubmitRequestDTO request) {
        return submitTradeOrdersBatch(request, false);
    }
    
    /**
     * Submit multiple trade orders in batch with true bulk execution processing
     * @param request The batch submission request
     * @param noExecuteSubmit When false (default), automatically submits to execution service; when true, only creates local executions
     */
    @Transactional
    public BatchSubmitResponseDTO submitTradeOrdersBatch(BatchSubmitRequestDTO request, boolean noExecuteSubmit) {
        logger.debug("Processing batch submission for {} trade orders", request.getSubmissions().size());
        
        // Validate batch size
        if (request.getSubmissions().size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException(
                String.format("Batch size (%d) exceeds maximum allowed (%d)", 
                    request.getSubmissions().size(), MAX_BATCH_SIZE));
        }
        
        // Validate request structure
        validateBatchRequest(request);
        
        // Step 1: Create all executions locally (without submitting to external service)
        List<BatchSubmitResponseDTO.TradeOrderSubmitResultDTO> results = new ArrayList<>();
        List<Integer> executionIds = new ArrayList<>();
        Map<Integer, Integer> executionToRequestIndex = new HashMap<>();
        
        for (int i = 0; i < request.getSubmissions().size(); i++) {
            BatchSubmitRequestDTO.TradeOrderSubmissionDTO submission = request.getSubmissions().get(i);
            int requestIndex = i;
            
            try {
                // Create execution locally (with noExecuteSubmit=true to skip external submission)
                BatchSubmitResponseDTO.TradeOrderSubmitResultDTO result = 
                    processTradeOrderSubmission(submission, requestIndex, true); // Always skip external submission initially
                
                results.add(result);
                
                if (BatchSubmitResponseDTO.TradeOrderSubmitResultDTO.SubmitStatus.SUCCESS.equals(result.getStatus())) {
                    // Extract execution ID from the result
                    Integer executionId = result.getExecution().getId();
                    executionIds.add(executionId);
                    executionToRequestIndex.put(executionId, requestIndex);
                    logger.debug("Created execution {} for trade order {} (request index {})", 
                               executionId, submission.getTradeOrderId(), requestIndex);
                }
            } catch (Exception e) {
                logger.error("Error creating execution for trade order {}: {}", submission.getTradeOrderId(), e.getMessage(), e);
                results.add(new BatchSubmitResponseDTO.TradeOrderSubmitResultDTO(
                    submission.getTradeOrderId(),
                    BatchSubmitResponseDTO.TradeOrderSubmitResultDTO.SubmitStatus.FAILURE,
                    "Failed to create execution: " + e.getMessage(),
                    null,
                    requestIndex
                ));
            }
        }
        
        // Step 2: If noExecuteSubmit is false and we have executions to submit, use bulk submission
        if (!noExecuteSubmit && !executionIds.isEmpty()) {
            logger.debug("Submitting {} executions in bulk to external service", executionIds.size());
            
            try {
                // Use the bulk execution submission service
                ExecutionService.BulkSubmitResult bulkResult = executionService.submitExecutions(executionIds);
                
                logger.debug("Bulk execution submission completed - Total: {}, Successful: {}, Failed: {}", 
                           bulkResult.getTotalRequested(), bulkResult.getSuccessful(), bulkResult.getFailed());
                
                // Update results based on bulk submission outcome
                updateResultsFromBulkSubmission(results, bulkResult, executionToRequestIndex);
                
            } catch (Exception e) {
                logger.error("Bulk execution submission failed: {}", e.getMessage(), e);
                
                // Mark all successfully created executions as failed due to bulk submission failure
                for (Integer executionId : executionIds) {
                    Integer requestIndex = executionToRequestIndex.get(executionId);
                    if (requestIndex != null && requestIndex < results.size()) {
                        BatchSubmitResponseDTO.TradeOrderSubmitResultDTO result = results.get(requestIndex);
                        if (BatchSubmitResponseDTO.TradeOrderSubmitResultDTO.SubmitStatus.SUCCESS.equals(result.getStatus())) {
                            // Update the result to reflect the bulk submission failure
                            results.set(requestIndex, new BatchSubmitResponseDTO.TradeOrderSubmitResultDTO(
                                result.getTradeOrderId(),
                                BatchSubmitResponseDTO.TradeOrderSubmitResultDTO.SubmitStatus.FAILURE,
                                "Bulk execution submission failed: " + e.getMessage(),
                                result.getExecution(), // Keep the execution data
                                requestIndex
                            ));
                        }
                    }
                }
            }
        }
        
        // Calculate summary statistics
        long successful = results.stream()
            .mapToLong(result -> BatchSubmitResponseDTO.TradeOrderSubmitResultDTO.SubmitStatus.SUCCESS.equals(result.getStatus()) ? 1 : 0)
            .sum();
        
        long failed = results.size() - successful;
        
        // Determine overall status
        BatchSubmitResponseDTO.BatchStatus overallStatus;
        String message;
        
        if (successful == results.size()) {
            overallStatus = BatchSubmitResponseDTO.BatchStatus.SUCCESS;
            message = String.format("All %d trade orders submitted successfully", results.size());
        } else if (successful == 0) {
            overallStatus = BatchSubmitResponseDTO.BatchStatus.FAILURE;
            message = String.format("All %d trade orders failed to submit", results.size());
        } else {
            overallStatus = BatchSubmitResponseDTO.BatchStatus.PARTIAL;
            message = String.format("%d of %d trade orders submitted successfully", successful, results.size());
        }
        
        logger.debug("Batch submission completed - Status: {}, Successful: {}, Failed: {}", 
                   overallStatus, successful, failed);
        
        return new BatchSubmitResponseDTO(
            overallStatus,
            message,
            results.size(),
            (int) successful,
            (int) failed,
            results
        );
    }
    
    /**
     * Update results based on bulk execution submission outcome
     */
    private void updateResultsFromBulkSubmission(
            List<BatchSubmitResponseDTO.TradeOrderSubmitResultDTO> results,
            ExecutionService.BulkSubmitResult bulkResult,
            Map<Integer, Integer> executionToRequestIndex) {
        
        // Process individual execution results from bulk submission
        for (ExecutionService.ExecutionSubmitResult executionResult : bulkResult.getResults()) {
            Integer requestIndex = executionToRequestIndex.get(executionResult.getExecutionId());
            
            if (requestIndex != null && requestIndex < results.size()) {
                BatchSubmitResponseDTO.TradeOrderSubmitResultDTO currentResult = results.get(requestIndex);
                
                // Only update if the current result was successful (execution was created)
                if (BatchSubmitResponseDTO.TradeOrderSubmitResultDTO.SubmitStatus.SUCCESS.equals(currentResult.getStatus())) {
                    
                    if ("SUCCESS".equals(executionResult.getStatus()) || "COMPLETED".equals(executionResult.getStatus())) {
                        // Execution submitted successfully - keep the current success result
                        // but potentially update execution data if needed
                        logger.debug("Execution {} submitted successfully to external service", executionResult.getExecutionId());
                        
                        // Re-fetch execution to get updated data (execution service ID, etc.)
                        try {
                            Execution updatedExecution = executionRepository.findByIdWithAllRelations(executionResult.getExecutionId())
                                .orElse(null);
                            if (updatedExecution != null) {
                                ExecutionResponseDTO updatedExecutionResponse = convertToExecutionResponseDTO(updatedExecution);
                                results.set(requestIndex, new BatchSubmitResponseDTO.TradeOrderSubmitResultDTO(
                                    currentResult.getTradeOrderId(),
                                    BatchSubmitResponseDTO.TradeOrderSubmitResultDTO.SubmitStatus.SUCCESS,
                                    "Trade order submitted successfully",
                                    updatedExecutionResponse,
                                    requestIndex
                                ));
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to re-fetch execution {} after successful submission: {}", 
                                       executionResult.getExecutionId(), e.getMessage());
                            // Keep the original result if re-fetch fails
                        }
                        
                    } else {
                        // Execution submission failed - update result to reflect failure
                        logger.warn("Execution {} failed to submit to external service: {}", 
                                   executionResult.getExecutionId(), executionResult.getMessage());
                        
                        results.set(requestIndex, new BatchSubmitResponseDTO.TradeOrderSubmitResultDTO(
                            currentResult.getTradeOrderId(),
                            BatchSubmitResponseDTO.TradeOrderSubmitResultDTO.SubmitStatus.FAILURE,
                            "Execution submission failed: " + executionResult.getMessage(),
                            currentResult.getExecution(), // Keep the execution data
                            requestIndex
                        ));
                    }
                }
            }
        }
    }

    /**
     * Process individual trade order submission
     */
    private BatchSubmitResponseDTO.TradeOrderSubmitResultDTO processTradeOrderSubmission(
            BatchSubmitRequestDTO.TradeOrderSubmissionDTO submission, 
            int requestIndex, 
            boolean noExecuteSubmit) {
        
        try {
            logger.debug("Processing trade order {} submission (index {})", 
                        submission.getTradeOrderId(), requestIndex);
            
            // Find trade order
            Optional<TradeOrder> tradeOrderOpt = tradeOrderRepository.findById(submission.getTradeOrderId());
            if (tradeOrderOpt.isEmpty()) {
                return new BatchSubmitResponseDTO.TradeOrderSubmitResultDTO(
                    submission.getTradeOrderId(),
                    BatchSubmitResponseDTO.TradeOrderSubmitResultDTO.SubmitStatus.FAILURE,
                    "Trade order not found",
                    null,
                    requestIndex
                );
            }
            
            TradeOrder tradeOrder = tradeOrderOpt.get();
            
            // Check if trade order is already submitted
            if (Boolean.TRUE.equals(tradeOrder.getSubmitted())) {
                return new BatchSubmitResponseDTO.TradeOrderSubmitResultDTO(
                    submission.getTradeOrderId(),
                    BatchSubmitResponseDTO.TradeOrderSubmitResultDTO.SubmitStatus.FAILURE,
                    "Trade order is already submitted",
                    null,
                    requestIndex
                );
            }
            
            // Create submission DTO for the existing service
            TradeOrderSubmitDTO submitDTO = new TradeOrderSubmitDTO();
            submitDTO.setQuantity(submission.getQuantity());
            submitDTO.setDestinationId(submission.getDestinationId());
            
            // Submit to trade order service
            Execution execution = tradeOrderService.submitTradeOrder(submission.getTradeOrderId(), submitDTO, noExecuteSubmit);
            
            // Re-fetch execution with all relationships to avoid lazy loading issues
            Execution executionWithRelations = executionRepository.findByIdWithAllRelations(execution.getId())
                .orElseThrow(() -> new RuntimeException("Execution not found after save: " + execution.getId()));
            
            // Create execution response DTO
            ExecutionResponseDTO executionResponse = convertToExecutionResponseDTO(executionWithRelations);
            
            return new BatchSubmitResponseDTO.TradeOrderSubmitResultDTO(
                submission.getTradeOrderId(),
                BatchSubmitResponseDTO.TradeOrderSubmitResultDTO.SubmitStatus.SUCCESS,
                "Trade order submitted successfully",
                executionResponse,
                requestIndex
            );
            
        } catch (Exception e) {
            logger.error("Error submitting trade order {}: {}", submission.getTradeOrderId(), e.getMessage(), e);
            return new BatchSubmitResponseDTO.TradeOrderSubmitResultDTO(
                submission.getTradeOrderId(),
                BatchSubmitResponseDTO.TradeOrderSubmitResultDTO.SubmitStatus.FAILURE,
                "Submission failed: " + e.getMessage(),
                null,
                requestIndex
            );
        }
    }
    
    /**
     * Convert Execution entity to ExecutionResponseDTO
     */
    private ExecutionResponseDTO convertToExecutionResponseDTO(Execution execution) {
        ExecutionResponseDTO dto = new ExecutionResponseDTO();
        dto.setId(execution.getId());
        dto.setExecutionTimestamp(execution.getExecutionTimestamp());
        dto.setQuantityOrdered(execution.getQuantityOrdered());
        dto.setQuantityPlaced(execution.getQuantityPlaced());
        dto.setQuantityFilled(execution.getQuantityFilled());
        dto.setLimitPrice(execution.getLimitPrice());
        dto.setExecutionServiceId(execution.getExecutionServiceId());
        dto.setVersion(execution.getVersion());
        
        // Set entity relationships as DTOs
        if (execution.getExecutionStatus() != null) {
            org.kasbench.globeco_trade_service.dto.ExecutionStatusResponseDTO statusDto = 
                new org.kasbench.globeco_trade_service.dto.ExecutionStatusResponseDTO();
            statusDto.setId(execution.getExecutionStatus().getId());
            statusDto.setAbbreviation(execution.getExecutionStatus().getAbbreviation());
            statusDto.setDescription(execution.getExecutionStatus().getDescription());
            statusDto.setVersion(execution.getExecutionStatus().getVersion());
            dto.setExecutionStatus(statusDto);
        }
        
        if (execution.getBlotter() != null) {
            org.kasbench.globeco_trade_service.dto.BlotterResponseDTO blotterDto = 
                new org.kasbench.globeco_trade_service.dto.BlotterResponseDTO();
            blotterDto.setId(execution.getBlotter().getId());
            blotterDto.setAbbreviation(execution.getBlotter().getAbbreviation());
            blotterDto.setName(execution.getBlotter().getName());
            blotterDto.setVersion(execution.getBlotter().getVersion());
            dto.setBlotter(blotterDto);
        }
        
        if (execution.getTradeType() != null) {
            org.kasbench.globeco_trade_service.dto.TradeTypeResponseDTO tradeTypeDto = 
                new org.kasbench.globeco_trade_service.dto.TradeTypeResponseDTO();
            tradeTypeDto.setId(execution.getTradeType().getId());
            tradeTypeDto.setAbbreviation(execution.getTradeType().getAbbreviation());
            tradeTypeDto.setDescription(execution.getTradeType().getDescription());
            tradeTypeDto.setVersion(execution.getTradeType().getVersion());
            dto.setTradeType(tradeTypeDto);
        }
        
        if (execution.getTradeOrder() != null) {
            org.kasbench.globeco_trade_service.dto.TradeOrderResponseDTO tradeOrderDto = 
                new org.kasbench.globeco_trade_service.dto.TradeOrderResponseDTO();
            tradeOrderDto.setId(execution.getTradeOrder().getId());
            tradeOrderDto.setOrderId(execution.getTradeOrder().getOrderId());
            tradeOrderDto.setPortfolioId(execution.getTradeOrder().getPortfolioId());
            tradeOrderDto.setOrderType(execution.getTradeOrder().getOrderType());
            tradeOrderDto.setSecurityId(execution.getTradeOrder().getSecurityId());
            tradeOrderDto.setQuantity(execution.getTradeOrder().getQuantity());
            tradeOrderDto.setQuantitySent(execution.getTradeOrder().getQuantitySent());
            tradeOrderDto.setLimitPrice(execution.getTradeOrder().getLimitPrice());
            tradeOrderDto.setTradeTimestamp(execution.getTradeOrder().getTradeTimestamp());
            tradeOrderDto.setSubmitted(execution.getTradeOrder().getSubmitted());
            tradeOrderDto.setVersion(execution.getTradeOrder().getVersion());
            dto.setTradeOrder(tradeOrderDto);
        }
        
        if (execution.getDestination() != null) {
            org.kasbench.globeco_trade_service.dto.DestinationResponseDTO destinationDto = 
                new org.kasbench.globeco_trade_service.dto.DestinationResponseDTO();
            destinationDto.setId(execution.getDestination().getId());
            destinationDto.setAbbreviation(execution.getDestination().getAbbreviation());
            destinationDto.setDescription(execution.getDestination().getDescription());
            destinationDto.setVersion(execution.getDestination().getVersion());
            dto.setDestination(destinationDto);
        }
        
        return dto;
    }
    
    /**
     * Validate batch request structure
     */
    private void validateBatchRequest(BatchSubmitRequestDTO request) {
        if (request.getSubmissions() == null || request.getSubmissions().isEmpty()) {
            throw new IllegalArgumentException("Submissions list cannot be empty");
        }
        
        // Check for duplicate trade order IDs
        List<Integer> tradeOrderIds = request.getSubmissions().stream()
            .map(BatchSubmitRequestDTO.TradeOrderSubmissionDTO::getTradeOrderId)
            .collect(Collectors.toList());
        
        long uniqueIds = tradeOrderIds.stream().distinct().count();
        if (uniqueIds != tradeOrderIds.size()) {
            throw new IllegalArgumentException("Duplicate trade order IDs are not allowed");
        }
        
        // Validate individual submissions
        for (BatchSubmitRequestDTO.TradeOrderSubmissionDTO submission : request.getSubmissions()) {
            if (submission.getTradeOrderId() == null) {
                throw new IllegalArgumentException("Submission trade order ID cannot be null");
            }
            
            if (submission.getQuantity() == null || 
                submission.getQuantity().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                    String.format("Submission quantity must be positive for trade order %d", 
                        submission.getTradeOrderId()));
            }
            
            if (submission.getDestinationId() == null) {
                throw new IllegalArgumentException(
                    String.format("Destination ID cannot be null for trade order %d", 
                        submission.getTradeOrderId()));
            }
        }
    }
} 