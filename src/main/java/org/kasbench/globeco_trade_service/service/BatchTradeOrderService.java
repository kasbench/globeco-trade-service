package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.dto.BatchSubmitRequestDTO;
import org.kasbench.globeco_trade_service.dto.BatchSubmitResponseDTO;
import org.kasbench.globeco_trade_service.dto.ExecutionResponseDTO;
import org.kasbench.globeco_trade_service.dto.TradeOrderSubmitDTO;
import org.kasbench.globeco_trade_service.entity.Execution;
import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.kasbench.globeco_trade_service.repository.TradeOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class BatchTradeOrderService {
    private static final Logger logger = LoggerFactory.getLogger(BatchTradeOrderService.class);
    private static final int MAX_BATCH_SIZE = 100;
    
    private final TradeOrderRepository tradeOrderRepository;
    private final TradeOrderService tradeOrderService;
    private final ExecutorService executorService;
    
    public BatchTradeOrderService(
            TradeOrderRepository tradeOrderRepository,
            TradeOrderService tradeOrderService) {
        this.tradeOrderRepository = tradeOrderRepository;
        this.tradeOrderService = tradeOrderService;
        this.executorService = Executors.newFixedThreadPool(10);
    }
    
    /**
     * Submit multiple trade orders in batch with parallel processing
     */
    @Transactional
    public BatchSubmitResponseDTO submitTradeOrdersBatch(BatchSubmitRequestDTO request) {
        logger.info("Processing batch submission for {} trade orders", request.getSubmissions().size());
        
        // Validate batch size
        if (request.getSubmissions().size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException(
                String.format("Batch size (%d) exceeds maximum allowed (%d)", 
                    request.getSubmissions().size(), MAX_BATCH_SIZE));
        }
        
        // Validate request structure
        validateBatchRequest(request);
        
        // Process submissions in parallel
        List<CompletableFuture<BatchSubmitResponseDTO.TradeOrderSubmitResultDTO>> futures = new ArrayList<>();
        
        for (int i = 0; i < request.getSubmissions().size(); i++) {
            BatchSubmitRequestDTO.TradeOrderSubmissionDTO submission = request.getSubmissions().get(i);
            int requestIndex = i;
            
            CompletableFuture<BatchSubmitResponseDTO.TradeOrderSubmitResultDTO> future = CompletableFuture
                .supplyAsync(() -> processTradeOrderSubmission(submission, requestIndex), executorService);
            
            futures.add(future);
        }
        
        // Wait for all submissions to complete
        List<BatchSubmitResponseDTO.TradeOrderSubmitResultDTO> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
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
        
        logger.info("Batch submission completed - Status: {}, Successful: {}, Failed: {}", 
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
     * Process individual trade order submission
     */
    private BatchSubmitResponseDTO.TradeOrderSubmitResultDTO processTradeOrderSubmission(
            BatchSubmitRequestDTO.TradeOrderSubmissionDTO submission, 
            int requestIndex) {
        
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
            Execution execution = tradeOrderService.submitTradeOrder(submission.getTradeOrderId(), submitDTO);
            
            // Create execution response DTO
            ExecutionResponseDTO executionResponse = convertToExecutionResponseDTO(execution);
            
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