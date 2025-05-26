package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.kasbench.globeco_trade_service.entity.Blotter;
import org.kasbench.globeco_trade_service.repository.TradeOrderRepository;
import org.kasbench.globeco_trade_service.repository.BlotterRepository;
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

    @Autowired
    public TradeOrderServiceImpl(TradeOrderRepository tradeOrderRepository, BlotterRepository blotterRepository) {
        this.tradeOrderRepository = tradeOrderRepository;
        this.blotterRepository = blotterRepository;
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
} 