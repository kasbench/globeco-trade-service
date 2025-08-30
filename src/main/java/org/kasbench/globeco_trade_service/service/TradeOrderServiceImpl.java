package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.kasbench.globeco_trade_service.entity.Blotter;
import org.kasbench.globeco_trade_service.entity.Execution;
import org.kasbench.globeco_trade_service.entity.ExecutionStatus;
import org.kasbench.globeco_trade_service.entity.TradeType;
import org.kasbench.globeco_trade_service.entity.Destination;
import org.kasbench.globeco_trade_service.repository.TradeOrderRepository;
import org.kasbench.globeco_trade_service.repository.BlotterRepository;
import org.kasbench.globeco_trade_service.repository.ExecutionRepository;
import org.kasbench.globeco_trade_service.repository.TradeTypeRepository;
import org.kasbench.globeco_trade_service.repository.ExecutionStatusRepository;
import org.kasbench.globeco_trade_service.repository.DestinationRepository;
import org.kasbench.globeco_trade_service.dto.TradeOrderSubmitDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Optional;

@Service
public class TradeOrderServiceImpl implements TradeOrderService {
    private final TradeOrderRepository tradeOrderRepository;
    private final BlotterRepository blotterRepository;
    private final ExecutionRepository executionRepository;
    private final TradeTypeRepository tradeTypeRepository;
    private final ExecutionStatusRepository executionStatusRepository;
    private final DestinationRepository destinationRepository;
    private final ExecutionService executionService;
    private final RetryTemplate retryTemplate;
    private static final Logger logger = LoggerFactory.getLogger(TradeOrderServiceImpl.class);

    @Autowired
    public TradeOrderServiceImpl(TradeOrderRepository tradeOrderRepository, BlotterRepository blotterRepository,
                                 ExecutionRepository executionRepository, TradeTypeRepository tradeTypeRepository,
                                 ExecutionStatusRepository executionStatusRepository, DestinationRepository destinationRepository,
                                 ExecutionService executionService, 
                                 @org.springframework.beans.factory.annotation.Qualifier("executionServiceRetryTemplate") RetryTemplate retryTemplate) {
        this.tradeOrderRepository = tradeOrderRepository;
        this.blotterRepository = blotterRepository;
        this.executionRepository = executionRepository;
        this.tradeTypeRepository = tradeTypeRepository;
        this.executionStatusRepository = executionStatusRepository;
        this.destinationRepository = destinationRepository;
        this.executionService = executionService;
        this.retryTemplate = retryTemplate;
    }

    @Override
    @Cacheable(value = "tradeOrders", cacheManager = "cacheManager")
    public List<TradeOrder> getAllTradeOrders() {
        return tradeOrderRepository.findAll();
    }

    @Override
    @Cacheable(value = "tradeOrders", cacheManager = "cacheManager")
    public PaginatedResult<TradeOrder> getAllTradeOrders(Integer limit, Integer offset) {
        return getAllTradeOrders(limit, offset, null);
    }
    
    @Override
    @Cacheable(value = "tradeOrders", cacheManager = "cacheManager")
    public PaginatedResult<TradeOrder> getAllTradeOrders(Integer limit, Integer offset, Integer orderId) {
        if (orderId != null) {
            // Filter by order_id
            if (limit == null && offset == null) {
                // No pagination requested, return filtered data
                List<TradeOrder> filtered = tradeOrderRepository.findByOrderId(orderId);
                return new PaginatedResult<>(filtered, filtered.size());
            } else {
                // Create pageable for pagination with filtering
                Pageable pageable = createPageable(limit, offset);
                Page<TradeOrder> page = tradeOrderRepository.findByOrderId(orderId, pageable);
                return new PaginatedResult<>(page.getContent(), page.getTotalElements());
            }
        } else {
            // No filtering, use existing logic
            if (limit == null && offset == null) {
                // No pagination requested, return all data
                List<TradeOrder> all = tradeOrderRepository.findAll();
                return new PaginatedResult<>(all, all.size());
            } else {
                // Create pageable for pagination
                Pageable pageable = createPageable(limit, offset);
                Page<TradeOrder> page = tradeOrderRepository.findAll(pageable);
                return new PaginatedResult<>(page.getContent(), page.getTotalElements());
            }
        }
    }
    
    /**
     * Helper method to create Pageable from limit and offset
     */
    private Pageable createPageable(Integer limit, Integer offset) {
        if (limit != null && offset != null) {
            return PageRequest.of(offset / limit, limit);
        } else if (limit != null) {
            // Only limit provided, start from beginning
            return PageRequest.of(0, limit);
        } else {
            // Only offset provided, use default page size of 50
            return PageRequest.of(offset / 50, 50);
        }
    }

    @Override
    @Cacheable(value = "tradeOrders", key = "#id", cacheManager = "cacheManager")
    public Optional<TradeOrder> getTradeOrderById(Integer id) {
        return tradeOrderRepository.findById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "tradeOrders", allEntries = true, cacheManager = "cacheManager")
    public TradeOrder createTradeOrder(TradeOrder tradeOrder) {
        tradeOrder.setId(null); // Ensure ID is not set for new entity
        if (tradeOrder.getBlotter() != null && tradeOrder.getBlotter().getId() != null) {
            Blotter blotter = blotterRepository.findById(tradeOrder.getBlotter().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Blotter not found: " + tradeOrder.getBlotter().getId()));
            tradeOrder.setBlotter(blotter);
        } else {
            tradeOrder.setBlotter(null);
        }
        tradeOrder.setTradeTimestamp(java.time.OffsetDateTime.now());
        if (tradeOrder.getSubmitted() == null) {
            tradeOrder.setSubmitted(false);
        }
        if (tradeOrder.getQuantitySent() == null) {
            tradeOrder.setQuantitySent(java.math.BigDecimal.ZERO);
        }
        return tradeOrderRepository.save(tradeOrder);
    }

    @Override
    @Transactional
    @CacheEvict(value = "tradeOrders", allEntries = true, cacheManager = "cacheManager")
    public TradeOrder updateTradeOrder(Integer id, TradeOrder tradeOrder) {
        TradeOrder existing = tradeOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TradeOrder not found: " + id));
        existing.setOrderId(tradeOrder.getOrderId());
        existing.setPortfolioId(tradeOrder.getPortfolioId());
        existing.setOrderType(tradeOrder.getOrderType());
        existing.setSecurityId(tradeOrder.getSecurityId());
        existing.setQuantity(tradeOrder.getQuantity());
        existing.setLimitPrice(tradeOrder.getLimitPrice());
        existing.setTradeTimestamp(tradeOrder.getTradeTimestamp());
        existing.setSubmitted(tradeOrder.getSubmitted());
        if (tradeOrder.getBlotter() != null && tradeOrder.getBlotter().getId() != null) {
            Blotter blotter = blotterRepository.findById(tradeOrder.getBlotter().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Blotter not found: " + tradeOrder.getBlotter().getId()));
            existing.setBlotter(blotter);
        } else {
            existing.setBlotter(null);
        }
        if (tradeOrder.getQuantitySent() == null) {
            existing.setQuantitySent(java.math.BigDecimal.ZERO);
        } else {
            existing.setQuantitySent(tradeOrder.getQuantitySent());
        }
        return tradeOrderRepository.save(existing);
    }

    @Override
    @Transactional
    @CacheEvict(value = "tradeOrders", allEntries = true, cacheManager = "cacheManager")
    public void deleteTradeOrder(Integer id, Integer version) {
        TradeOrder existing = tradeOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TradeOrder not found: " + id));
        if (!existing.getVersion().equals(version)) {
            throw new IllegalArgumentException("Version mismatch for tradeOrder: " + id);
        }
        tradeOrderRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Execution submitTradeOrder(Integer tradeOrderId, TradeOrderSubmitDTO dto, boolean noExecuteSubmit) {
        logger.info("TradeOrderServiceImpl.submitTradeOrder called with tradeOrderId={}, dto={}, noExecuteSubmit={}", 
                   tradeOrderId, dto, noExecuteSubmit);
        
        // Store original values for potential rollback
        java.math.BigDecimal originalQuantitySent = null;
        Boolean originalSubmittedStatus = null;
        Execution savedExecution = null;
        
        try {
            TradeOrder tradeOrder = tradeOrderRepository.findByIdWithBlotter(tradeOrderId)
                    .orElseThrow(() -> new IllegalArgumentException("TradeOrder not found: " + tradeOrderId));
            
            // Store original values for rollback
            originalQuantitySent = tradeOrder.getQuantitySent();
            originalSubmittedStatus = tradeOrder.getSubmitted();
            
            if (dto.getQuantity() == null) {
                throw new IllegalArgumentException("Quantity must not be null");
            }
            java.math.BigDecimal available = tradeOrder.getQuantity().subtract(
                tradeOrder.getQuantitySent() == null ? java.math.BigDecimal.ZERO : tradeOrder.getQuantitySent()
            );
            if (dto.getQuantity().compareTo(available) > 0) {
                throw new IllegalArgumentException("Requested quantity exceeds available quantity");
            }
            
            // Normalize orderType before switch
            String normalizedOrderType = tradeOrder.getOrderType() == null ? null : tradeOrder.getOrderType().trim().toUpperCase();
            // Map order_type to trade_type_id
            Integer tradeTypeId = switch (normalizedOrderType) {
                case "BUY" -> 1;
                case "SELL" -> 2;
                case "SHORT" -> 3;
                case "COVER" -> 4;
                case "EXRC" -> 5;
                default -> throw new IllegalArgumentException("Unknown order_type: " + tradeOrder.getOrderType());
            };
            
            TradeType tradeType = tradeTypeRepository.findById(tradeTypeId)
                    .orElseThrow(() -> new IllegalArgumentException("TradeType not found: " + tradeTypeId));
            ExecutionStatus status = executionStatusRepository.findById(1)
                    .orElseThrow(() -> new IllegalArgumentException("ExecutionStatus not found: 1"));
            Destination destination = destinationRepository.findById(dto.getDestinationId())
                    .orElseThrow(() -> new IllegalArgumentException("Destination not found: " + dto.getDestinationId()));
            
            // Create and save execution
            Execution execution = new Execution();
            execution.setExecutionTimestamp(java.time.OffsetDateTime.now());
            execution.setExecutionStatus(status);
            execution.setTradeType(tradeType);
            execution.setTradeOrder(tradeOrder);
            execution.setDestination(destination);
            execution.setQuantityOrdered(dto.getQuantity());
            execution.setQuantityPlaced(java.math.BigDecimal.ZERO);
            execution.setQuantityFilled(java.math.BigDecimal.ZERO);
            execution.setLimitPrice(tradeOrder.getLimitPrice());
            execution.setExecutionServiceId(null);
            execution.setVersion(1);
            if (tradeOrder.getBlotter() != null) {
                execution.setBlotter(tradeOrder.getBlotter());
            }
            
            savedExecution = executionRepository.save(execution);
            
            // Update trade order quantities
            java.math.BigDecimal newQuantitySent = (tradeOrder.getQuantitySent() == null ? java.math.BigDecimal.ZERO : tradeOrder.getQuantitySent()).add(dto.getQuantity());
            newQuantitySent = newQuantitySent.setScale(tradeOrder.getQuantity().scale(), java.math.RoundingMode.HALF_UP);
            tradeOrder.setQuantitySent(newQuantitySent);
            
            // Only set submitted if fully sent (within 0.001)
            if (tradeOrder.getQuantity().subtract(tradeOrder.getQuantitySent()).abs().compareTo(new java.math.BigDecimal("0.01")) <= 0) {
                tradeOrder.setSubmitted(true);
            }
            
            tradeOrderRepository.save(tradeOrder);
            
            // If noExecuteSubmit is false (default), automatically submit to execution service
            if (!noExecuteSubmit) {
                final Integer executionId = savedExecution.getId();
                logger.info("Automatically submitting execution {} to external service", executionId);
                try {
                    // Use retry template for external service call with enhanced logging
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
                    
                    // Re-fetch the execution to get updated status and execution service ID
                    savedExecution = executionRepository.findById(executionId)
                            .orElseThrow(() -> new RuntimeException("Execution not found after submission: " + executionId));
                    
                } catch (Exception executionServiceException) {
                    logger.error("External execution service failure for trade order {}: {}", 
                               tradeOrderId, executionServiceException.getMessage());
                    logger.error("Failed to submit execution {} to external service after retries, rolling back: {}", 
                               executionId, executionServiceException.getMessage());
                    
                    // Compensating transaction: rollback the trade order and execution
                    performCompensatingTransaction(savedExecution, tradeOrder, originalQuantitySent, originalSubmittedStatus);
                    
                    // Determine appropriate exception type based on cause
                    if (executionServiceException.getCause() instanceof HttpClientErrorException) {
                        throw new IllegalArgumentException("Execution service rejected the request: " + executionServiceException.getMessage());
                    } else {
                        throw new RuntimeException("Failed to submit execution to external service: " + executionServiceException.getMessage());
                    }
                }
            }
            
            return savedExecution;
            
        } catch (Exception e) {
            logger.error("Exception in TradeOrderServiceImpl.submitTradeOrder: {}: {}", e.getClass().getName(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Perform compensating transaction to rollback changes when execution service fails
     */
    private void performCompensatingTransaction(Execution execution, TradeOrder tradeOrder, 
                                              java.math.BigDecimal originalQuantitySent, Boolean originalSubmittedStatus) {
        try {
            logger.info("Performing compensating transaction for execution {} and trade order {}", 
                       execution.getId(), tradeOrder.getId());
            
            // Delete the execution record
            executionRepository.deleteById(execution.getId());
            logger.debug("Deleted execution record {}", execution.getId());
            
            // Restore trade order state
            tradeOrder.setQuantitySent(originalQuantitySent);
            tradeOrder.setSubmitted(originalSubmittedStatus);
            tradeOrderRepository.save(tradeOrder);
            logger.debug("Restored trade order {} to original state", tradeOrder.getId());
            
        } catch (Exception rollbackException) {
            logger.error("CRITICAL: Failed to perform compensating transaction for execution {} and trade order {}: {}", 
                       execution.getId(), tradeOrder.getId(), rollbackException.getMessage(), rollbackException);
            // Note: In a production system, this should trigger an alert or be handled by a dead letter queue
        }
    }
} 