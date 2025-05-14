package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.entity.*;
import org.kasbench.globeco_trade_service.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ExecutionServiceImpl implements ExecutionService {
    private final ExecutionRepository executionRepository;
    private final ExecutionStatusRepository executionStatusRepository;
    private final BlotterRepository blotterRepository;
    private final TradeTypeRepository tradeTypeRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final DestinationRepository destinationRepository;

    @Autowired
    public ExecutionServiceImpl(
            ExecutionRepository executionRepository,
            ExecutionStatusRepository executionStatusRepository,
            BlotterRepository blotterRepository,
            TradeTypeRepository tradeTypeRepository,
            TradeOrderRepository tradeOrderRepository,
            DestinationRepository destinationRepository) {
        this.executionRepository = executionRepository;
        this.executionStatusRepository = executionStatusRepository;
        this.blotterRepository = blotterRepository;
        this.tradeTypeRepository = tradeTypeRepository;
        this.tradeOrderRepository = tradeOrderRepository;
        this.destinationRepository = destinationRepository;
    }

    @Override
    @Cacheable(value = "executions", cacheManager = "cacheManager")
    public List<Execution> getAllExecutions() {
        return executionRepository.findAll();
    }

    @Override
    @Cacheable(value = "executions", key = "#id", cacheManager = "cacheManager")
    public Optional<Execution> getExecutionById(Integer id) {
        return executionRepository.findById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "executions", allEntries = true, cacheManager = "cacheManager")
    public Execution createExecution(Execution execution) {
        execution.setId(null); // Ensure ID is not set for new entity
        resolveRelationships(execution);
        return executionRepository.save(execution);
    }

    @Override
    @Transactional
    @CacheEvict(value = "executions", allEntries = true, cacheManager = "cacheManager")
    public Execution updateExecution(Integer id, Execution execution) {
        Execution existing = executionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + id));
        existing.setExecutionTimestamp(execution.getExecutionTimestamp());
        existing.setQuantityOrdered(execution.getQuantityOrdered());
        existing.setQuantityPlaced(execution.getQuantityPlaced());
        existing.setQuantityFilled(execution.getQuantityFilled());
        existing.setLimitPrice(execution.getLimitPrice());
        resolveRelationshipsForUpdate(existing, execution);
        return executionRepository.save(existing);
    }

    @Override
    @Transactional
    @CacheEvict(value = "executions", allEntries = true, cacheManager = "cacheManager")
    public void deleteExecution(Integer id, Integer version) {
        Execution existing = executionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + id));
        if (!existing.getVersion().equals(version)) {
            throw new IllegalArgumentException("Version mismatch for execution: " + id);
        }
        executionRepository.deleteById(id);
    }

    private void resolveRelationships(Execution execution) {
        if (execution.getExecutionStatus() != null && execution.getExecutionStatus().getId() != null) {
            ExecutionStatus status = executionStatusRepository.findById(execution.getExecutionStatus().getId())
                    .orElseThrow(() -> new IllegalArgumentException("ExecutionStatus not found: " + execution.getExecutionStatus().getId()));
            execution.setExecutionStatus(status);
        } else {
            throw new IllegalArgumentException("ExecutionStatus is required");
        }
        if (execution.getBlotter() != null && execution.getBlotter().getId() != null) {
            Blotter blotter = blotterRepository.findById(execution.getBlotter().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Blotter not found: " + execution.getBlotter().getId()));
            execution.setBlotter(blotter);
        } else {
            execution.setBlotter(null);
        }
        if (execution.getTradeType() != null && execution.getTradeType().getId() != null) {
            TradeType tradeType = tradeTypeRepository.findById(execution.getTradeType().getId())
                    .orElseThrow(() -> new IllegalArgumentException("TradeType not found: " + execution.getTradeType().getId()));
            execution.setTradeType(tradeType);
        } else {
            execution.setTradeType(null);
        }
        if (execution.getTradeOrder() != null && execution.getTradeOrder().getId() != null) {
            TradeOrder tradeOrder = tradeOrderRepository.findById(execution.getTradeOrder().getId())
                    .orElseThrow(() -> new IllegalArgumentException("TradeOrder not found: " + execution.getTradeOrder().getId()));
            execution.setTradeOrder(tradeOrder);
        } else {
            throw new IllegalArgumentException("TradeOrder is required");
        }
        if (execution.getDestination() != null && execution.getDestination().getId() != null) {
            Destination destination = destinationRepository.findById(execution.getDestination().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Destination not found: " + execution.getDestination().getId()));
            execution.setDestination(destination);
        } else {
            throw new IllegalArgumentException("Destination is required");
        }
    }

    private void resolveRelationshipsForUpdate(Execution existing, Execution incoming) {
        if (incoming.getExecutionStatus() != null && incoming.getExecutionStatus().getId() != null) {
            ExecutionStatus status = executionStatusRepository.findById(incoming.getExecutionStatus().getId())
                    .orElseThrow(() -> new IllegalArgumentException("ExecutionStatus not found: " + incoming.getExecutionStatus().getId()));
            existing.setExecutionStatus(status);
        }
        if (incoming.getBlotter() != null && incoming.getBlotter().getId() != null) {
            Blotter blotter = blotterRepository.findById(incoming.getBlotter().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Blotter not found: " + incoming.getBlotter().getId()));
            existing.setBlotter(blotter);
        } else {
            existing.setBlotter(null);
        }
        if (incoming.getTradeType() != null && incoming.getTradeType().getId() != null) {
            TradeType tradeType = tradeTypeRepository.findById(incoming.getTradeType().getId())
                    .orElseThrow(() -> new IllegalArgumentException("TradeType not found: " + incoming.getTradeType().getId()));
            existing.setTradeType(tradeType);
        } else {
            existing.setTradeType(null);
        }
        if (incoming.getTradeOrder() != null && incoming.getTradeOrder().getId() != null) {
            TradeOrder tradeOrder = tradeOrderRepository.findById(incoming.getTradeOrder().getId())
                    .orElseThrow(() -> new IllegalArgumentException("TradeOrder not found: " + incoming.getTradeOrder().getId()));
            existing.setTradeOrder(tradeOrder);
        }
        if (incoming.getDestination() != null && incoming.getDestination().getId() != null) {
            Destination destination = destinationRepository.findById(incoming.getDestination().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Destination not found: " + incoming.getDestination().getId()));
            existing.setDestination(destination);
        }
    }
} 