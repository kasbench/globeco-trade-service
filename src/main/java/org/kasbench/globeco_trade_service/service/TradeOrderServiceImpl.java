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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private static final Logger logger = LoggerFactory.getLogger(TradeOrderServiceImpl.class);

    @Autowired
    public TradeOrderServiceImpl(TradeOrderRepository tradeOrderRepository, BlotterRepository blotterRepository,
                                 ExecutionRepository executionRepository, TradeTypeRepository tradeTypeRepository,
                                 ExecutionStatusRepository executionStatusRepository, DestinationRepository destinationRepository) {
        this.tradeOrderRepository = tradeOrderRepository;
        this.blotterRepository = blotterRepository;
        this.executionRepository = executionRepository;
        this.tradeTypeRepository = tradeTypeRepository;
        this.executionStatusRepository = executionStatusRepository;
        this.destinationRepository = destinationRepository;
    }

    @Override
    @Cacheable(value = "tradeOrders", cacheManager = "cacheManager")
    public List<TradeOrder> getAllTradeOrders() {
        return tradeOrderRepository.findAll();
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
    public Execution submitTradeOrder(Integer tradeOrderId, TradeOrderSubmitDTO dto) {
        logger.info("TradeOrderServiceImpl.submitTradeOrder called with tradeOrderId={} and dto={}", tradeOrderId, dto);
        try {
            TradeOrder tradeOrder = tradeOrderRepository.findById(tradeOrderId)
                    .orElseThrow(() -> new IllegalArgumentException("TradeOrder not found: " + tradeOrderId));
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
            Execution saved = executionRepository.save(execution);
            // Increment quantitySent
            java.math.BigDecimal newQuantitySent = (tradeOrder.getQuantitySent() == null ? java.math.BigDecimal.ZERO : tradeOrder.getQuantitySent()).add(dto.getQuantity());
            newQuantitySent = newQuantitySent.setScale(tradeOrder.getQuantity().scale(), java.math.RoundingMode.HALF_UP);
            tradeOrder.setQuantitySent(newQuantitySent);
            // Only set submitted if fully sent (within 0.001)
            if (tradeOrder.getQuantity().subtract(tradeOrder.getQuantitySent()).abs().compareTo(new java.math.BigDecimal("0.01")) <= 0) {
                tradeOrder.setSubmitted(true);
            }
            tradeOrderRepository.save(tradeOrder);
            return saved;
        } catch (Exception e) {
            logger.error("Exception in TradeOrderServiceImpl.submitTradeOrder: {}: {}", e.getClass().getName(), e.getMessage(), e);
            throw e;
        }
    }
} 