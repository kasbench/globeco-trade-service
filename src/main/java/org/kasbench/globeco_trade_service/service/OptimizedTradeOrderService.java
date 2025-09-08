package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.kasbench.globeco_trade_service.entity.Execution;
import org.kasbench.globeco_trade_service.entity.ExecutionStatus;
import org.kasbench.globeco_trade_service.entity.TradeType;
import org.kasbench.globeco_trade_service.entity.Destination;
import org.kasbench.globeco_trade_service.repository.TradeOrderRepository;
import org.kasbench.globeco_trade_service.repository.ExecutionRepository;
import org.kasbench.globeco_trade_service.repository.TradeTypeRepository;
import org.kasbench.globeco_trade_service.repository.ExecutionStatusRepository;
import org.kasbench.globeco_trade_service.repository.DestinationRepository;
import org.kasbench.globeco_trade_service.dto.TradeOrderSubmitDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Optimized Trade Order Service with separate transaction methods to reduce transaction scope
 * and improve performance under load. This service implements the transaction scope reduction
 * pattern to minimize lock contention and transaction commit times.
 */
@Service
public class OptimizedTradeOrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OptimizedTradeOrderService.class);
    
    private final TradeOrderRepository tradeOrderRepository;
    private final ExecutionRepository executionRepository;
    private final TradeTypeRepository tradeTypeRepository;
    private final ExecutionStatusRepository executionStatusRepository;
    private final DestinationRepository destinationRepository;
    private final ExecutionService executionService;
    private final RetryTemplate retryTemplate;
    private final TransactionCompensationHandler compensationHandler;
    
    @Autowired
    public OptimizedTradeOrderService(
            TradeOrderRepository tradeOrderRepository,
            ExecutionRepository executionRepository,
            TradeTypeRepository tradeTypeRepository,
            ExecutionStatusRepository executionStatusRepository,
            DestinationRepository destinationRepository,
            ExecutionService executionService,
            @org.springframework.beans.factory.annotation.Qualifier("executionServiceRetryTemplate") RetryTemplate retryTemplate,
            TransactionCompensationHandler compensationHandler) {
        this.tradeOrderRepository = tradeOrderRepository;
        this.executionRepository = executionRepository;
        this.tradeTypeRepository = tradeTypeRepository;
        this.executionStatusRepository = executionStatusRepository;
        this.destinationRepository = destinationRepository;
        this.executionService = executionService;
        this.retryTemplate = retryTemplate;
        this.compensationHandler = compensationHandler;
    }
    
    /**
     * Creates an execution record in a separate short-lived transaction.
     * This method uses REQUIRES_NEW propagation to ensure it runs in its own transaction,
     * reducing the overall transaction scope and lock contention.
     * 
     * @param tradeOrder The trade order for which to create the execution
     * @param dto The submission details
     * @return The created execution record
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Execution createExecutionRecord(TradeOrder tradeOrder, TradeOrderSubmitDTO dto) {
        long startTime = System.currentTimeMillis();
        logger.debug("Creating execution record for trade order {} in separate transaction", tradeOrder.getId());
        
        try {
            // Validate quantity availability
            if (dto.getQuantity() == null) {
                throw new IllegalArgumentException("Quantity must not be null");
            }
            
            BigDecimal available = tradeOrder.getQuantity().subtract(
                tradeOrder.getQuantitySent() == null ? BigDecimal.ZERO : tradeOrder.getQuantitySent());
            if (dto.getQuantity().compareTo(available) > 0) {
                throw new IllegalArgumentException("Requested quantity exceeds available quantity");
            }
            
            // Map order_type to trade_type_id
            String normalizedOrderType = tradeOrder.getOrderType() == null ? null
                    : tradeOrder.getOrderType().trim().toUpperCase();
            
            Integer tradeTypeId = switch (normalizedOrderType) {
                case "BUY" -> 1;
                case "SELL" -> 2;
                case "SHORT" -> 3;
                case "COVER" -> 4;
                case "EXRC" -> 5;
                default -> throw new IllegalArgumentException("Unknown order_type: " + tradeOrder.getOrderType());
            };
            
            // Load required entities
            TradeType tradeType = tradeTypeRepository.findById(tradeTypeId)
                    .orElseThrow(() -> new IllegalArgumentException("TradeType not found: " + tradeTypeId));
            ExecutionStatus status = executionStatusRepository.findById(1)
                    .orElseThrow(() -> new IllegalArgumentException("ExecutionStatus not found: 1"));
            Destination destination = destinationRepository.findById(dto.getDestinationId())
                    .orElseThrow(() -> new IllegalArgumentException("Destination not found: " + dto.getDestinationId()));
            
            // Create execution record
            Execution execution = new Execution();
            execution.setExecutionTimestamp(OffsetDateTime.now());
            execution.setExecutionStatus(status);
            execution.setTradeType(tradeType);
            execution.setTradeOrder(tradeOrder);
            execution.setDestination(destination);
            execution.setQuantityOrdered(dto.getQuantity());
            execution.setQuantityPlaced(BigDecimal.ZERO);
            execution.setQuantityFilled(BigDecimal.ZERO);
            execution.setLimitPrice(tradeOrder.getLimitPrice());
            execution.setExecutionServiceId(null);
            execution.setVersion(1);
            
            if (tradeOrder.getBlotter() != null) {
                execution.setBlotter(tradeOrder.getBlotter());
            }
            
            Execution savedExecution = executionRepository.save(execution);
            
            long endTime = System.currentTimeMillis();
            logger.debug("Execution record created in {}ms for trade order {}", 
                    (endTime - startTime), tradeOrder.getId());
            
            return savedExecution;
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            logger.error("Failed to create execution record in {}ms for trade order {}: {}", 
                    (endTime - startTime), tradeOrder.getId(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * Updates trade order quantities in a separate short-lived transaction.
     * This method uses REQUIRES_NEW propagation to ensure it runs in its own transaction,
     * reducing the overall transaction scope and lock contention.
     * 
     * @param tradeOrderId The ID of the trade order to update
     * @param quantity The quantity to add to the sent amount
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateTradeOrderQuantities(Integer tradeOrderId, BigDecimal quantity) {
        long startTime = System.currentTimeMillis();
        logger.debug("Updating trade order quantities for {} in separate transaction", tradeOrderId);
        
        try {
            TradeOrder tradeOrder = tradeOrderRepository.findById(tradeOrderId)
                    .orElseThrow(() -> new IllegalArgumentException("TradeOrder not found: " + tradeOrderId));
            
            // Update quantities
            BigDecimal newQuantitySent = (tradeOrder.getQuantitySent() == null ? BigDecimal.ZERO
                    : tradeOrder.getQuantitySent()).add(quantity);
            newQuantitySent = newQuantitySent.setScale(tradeOrder.getQuantity().scale(),
                    java.math.RoundingMode.HALF_UP);
            tradeOrder.setQuantitySent(newQuantitySent);
            
            // Only set submitted if fully sent (within 0.01 tolerance)
            if (tradeOrder.getQuantity().subtract(tradeOrder.getQuantitySent()).abs()
                    .compareTo(new BigDecimal("0.01")) <= 0) {
                tradeOrder.setSubmitted(true);
            }
            
            tradeOrderRepository.save(tradeOrder);
            
            long endTime = System.currentTimeMillis();
            logger.debug("Trade order quantities updated in {}ms for trade order {}", 
                    (endTime - startTime), tradeOrderId);
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            logger.error("Failed to update trade order quantities in {}ms for trade order {}: {}", 
                    (endTime - startTime), tradeOrderId, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Coordinates the trade order submission process without holding long transactions.
     * This method orchestrates the separate transaction methods and handles external service calls
     * outside of database transactions to minimize lock contention and transaction commit times.
     * 
     * @param tradeOrderId The ID of the trade order to submit
     * @param dto The submission details
     * @param noExecuteSubmit When false, automatically submits to execution service; when true, only creates local execution
     * @return The created execution record
     */
    public Execution submitTradeOrder(Integer tradeOrderId, TradeOrderSubmitDTO dto, boolean noExecuteSubmit) {
        long methodStartTime = System.currentTimeMillis();
        logger.info("OptimizedTradeOrderService.submitTradeOrder called with tradeOrderId={}, dto={}, noExecuteSubmit={}",
                tradeOrderId, dto, noExecuteSubmit);
        
        try {
            // Step 1: Load trade order (read-only, no transaction needed)
            TradeOrder tradeOrder = tradeOrderRepository.findByIdWithBlotter(tradeOrderId)
                    .orElseThrow(() -> new IllegalArgumentException("TradeOrder not found: " + tradeOrderId));
            
            // Store original values for potential compensation
            BigDecimal originalQuantitySent = tradeOrder.getQuantitySent();
            Boolean originalSubmittedStatus = tradeOrder.getSubmitted();
            
            // Step 2: Create execution record (short transaction)
            Execution savedExecution = createExecutionRecord(tradeOrder, dto);
            
            // Step 3: Update trade order quantities (short transaction)
            updateTradeOrderQuantities(tradeOrderId, dto.getQuantity());
            
            // Step 4: Submit to external service (no transaction)
            if (!noExecuteSubmit) {
                try {
                    final Integer executionId = savedExecution.getId();
                    submitToExternalService(executionId);
                    
                    // Re-fetch execution to get updated status and execution service ID
                    savedExecution = executionRepository.findById(executionId)
                            .orElseThrow(() -> new RuntimeException("Execution not found after submission: " + executionId));
                    
                } catch (Exception executionServiceException) {
                    logger.error("External execution service failure for trade order {}: {}",
                            tradeOrderId, executionServiceException.getMessage());
                    
                    // Use the enhanced TransactionCompensationHandler for async compensation
                    TransactionCompensationHandler.TradeOrderState originalState = 
                            new TransactionCompensationHandler.TradeOrderState(tradeOrderId, originalQuantitySent, originalSubmittedStatus);
                    compensationHandler.compensateFailedSubmission(savedExecution, originalState);
                    
                    // Re-throw appropriate exception
                    if (executionServiceException.getCause() instanceof org.springframework.web.client.HttpClientErrorException) {
                        throw new IllegalArgumentException(
                                "Execution service rejected the request: " + executionServiceException.getMessage());
                    } else {
                        throw new RuntimeException("Failed to submit execution to external service: "
                                + executionServiceException.getMessage());
                    }
                }
            }
            
            long methodEndTime = System.currentTimeMillis();
            logger.info("OptimizedTradeOrderService.submitTradeOrder completed in {}ms for tradeOrderId={}",
                    (methodEndTime - methodStartTime), tradeOrderId);
            
            return savedExecution;
            
        } catch (Exception e) {
            long methodEndTime = System.currentTimeMillis();
            logger.error("OptimizedTradeOrderService.submitTradeOrder failed after {}ms for tradeOrderId={}: {}",
                    (methodEndTime - methodStartTime), tradeOrderId, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Submits execution to external service with retry logic.
     * This method is called outside of any database transaction to avoid blocking commits.
     * 
     * @param executionId The ID of the execution to submit
     */
    private void submitToExternalService(Integer executionId) {
        logger.info("Submitting execution {} to external service", executionId);
        
        ExecutionService.SubmitResult result = retryTemplate.execute(context -> {
            if (context.getRetryCount() > 0) {
                logger.warn("Retrying execution service submission for execution {} (attempt {})",
                        executionId, context.getRetryCount() + 1);
            } else {
                logger.debug("Attempting execution service submission for execution {} (attempt {})",
                        executionId, context.getRetryCount() + 1);
            }
            return executionService.submitExecution(executionId);
        });
        
        if (result.getError() != null || !"submitted".equals(result.getStatus())) {
            throw new RuntimeException("Execution service submission failed: " + result.getError());
        }
        
        logger.info("Execution {} successfully submitted to external service", executionId);
    }
    

}