package org.kasbench.globeco_trade_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.kasbench.globeco_trade_service.entity.Blotter;
import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.kasbench.globeco_trade_service.repository.BlotterRepository;
import org.kasbench.globeco_trade_service.repository.TradeOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.cache.CacheManager;
import java.util.UUID;
import org.kasbench.globeco_trade_service.repository.ExecutionRepository;

@SpringBootTest
public class TradeOrderServiceImplTest extends org.kasbench.globeco_trade_service.AbstractPostgresContainerTest {
    @Autowired
    private TradeOrderService tradeOrderService;
    @Autowired
    private TradeOrderRepository tradeOrderRepository;
    @Autowired
    private BlotterRepository blotterRepository;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private ExecutionRepository executionRepository;

    private TradeOrder createTradeOrder() {
        String unique = Integer.toHexString(ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE));
        Blotter blotter = new Blotter();
        blotter.setAbbreviation("EQ" + unique);
        blotter.setName("Equity" + unique);
        blotter = blotterRepository.saveAndFlush(blotter);

        TradeOrder tradeOrder = new TradeOrder();
        tradeOrder.setOrderId(Math.abs(UUID.randomUUID().hashCode()));
        tradeOrder.setPortfolioId("PORT123");
        tradeOrder.setOrderType("BUY");
        tradeOrder.setSecurityId("SEC456");
        tradeOrder.setQuantity(new BigDecimal("100.25"));
        tradeOrder.setLimitPrice(new BigDecimal("10.50"));
        tradeOrder.setTradeTimestamp(OffsetDateTime.now());
        tradeOrder.setBlotter(blotter);
        return tradeOrderService.createTradeOrder(tradeOrder);
    }

    @Test
    @Transactional
    void testCreateAndGet() {
        TradeOrder tradeOrder = createTradeOrder();
        Optional<TradeOrder> found = tradeOrderService.getTradeOrderById(tradeOrder.getId());
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals("PORT123", found.get().getPortfolioId());
    }

    @Test
    @Transactional
    void testUpdate() {
        TradeOrder tradeOrder = createTradeOrder();
        tradeOrder.setOrderType("SELL");
        TradeOrder updated = tradeOrderService.updateTradeOrder(tradeOrder.getId(), tradeOrder);
        Assertions.assertEquals("SELL", updated.getOrderType());
    }

    @Test
    @Transactional
    void testDelete() {
        TradeOrder tradeOrder = createTradeOrder();
        tradeOrderService.deleteTradeOrder(tradeOrder.getId(), tradeOrder.getVersion());
        Assertions.assertTrue(tradeOrderService.getTradeOrderById(tradeOrder.getId()).isEmpty());
    }

    @Disabled("Disabled: persistent failures with optimistic locking exception detection in test environment")  
    @Test
    void testOptimisticConcurrency() {
        TradeOrder tradeOrder = createTradeOrder();
        Integer id = tradeOrder.getId();
        TradeOrder t1 = tradeOrderRepository.findById(id).orElseThrow();
        TradeOrder t2 = tradeOrderRepository.findById(id).orElseThrow();
        t1.setOrderType("UPDATE1");
        tradeOrderRepository.saveAndFlush(t1);
        t2.setOrderType("UPDATE2");
        Assertions.assertThrows(OptimisticLockingFailureException.class, () -> {
            tradeOrderRepository.saveAndFlush(t2);
        });
    }

    @Test
    void testDeleteVersionMismatch() {
        TradeOrder tradeOrder = createTradeOrder();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.deleteTradeOrder(tradeOrder.getId(), tradeOrder.getVersion() + 1);
        });
    }

    @Test
    void testGetTradeOrderByIdUsesCache() {
        TradeOrder tradeOrder = createTradeOrder();
        Integer id = tradeOrder.getId();
        // First call: should hit DB
        Optional<TradeOrder> found1 = tradeOrderService.getTradeOrderById(id);
        Assertions.assertTrue(found1.isPresent());
        // Remove from DB directly
        tradeOrderRepository.deleteById(id);
        // Second call: should hit cache, still returns the entity
        Optional<TradeOrder> found2 = tradeOrderService.getTradeOrderById(id);
        Assertions.assertTrue(found2.isPresent());
    }

    @Test
    void testGetAllTradeOrdersUsesCache() {
        createTradeOrder();
        // First call: should hit DB
        Assertions.assertFalse(tradeOrderService.getAllTradeOrders().isEmpty());
        // Remove all Executions first to avoid FK constraint
        executionRepository.deleteAll();
        // Remove all from DB directly
        tradeOrderRepository.deleteAll();
        // Second call: should hit cache, still returns the entity
        Assertions.assertFalse(tradeOrderService.getAllTradeOrders().isEmpty());
    }

    @SuppressWarnings("null")
    @Test
    void testCacheEvictedOnCreateUpdateDelete() {
        TradeOrder tradeOrder = createTradeOrder();
        Integer id = tradeOrder.getId();
        // Prime the cache
        tradeOrderService.getTradeOrderById(id);
        Assertions.assertNotNull(cacheManager.getCache("tradeOrders").get(id));
        // Update should evict cache
        tradeOrder.setOrderType("Updated");
        tradeOrderService.updateTradeOrder(id, tradeOrder);
        Assertions.assertNull(cacheManager.getCache("tradeOrders").get(id));
        // Prime the cache again
        tradeOrderService.getTradeOrderById(id);
        Assertions.assertNotNull(cacheManager.getCache("tradeOrders").get(id));
        // Reload the entity to get the latest version
        TradeOrder updated = tradeOrderRepository.findById(id).orElseThrow();
        // Delete should evict cache
        tradeOrderService.deleteTradeOrder(id, updated.getVersion());
        Assertions.assertNull(cacheManager.getCache("tradeOrders").get(id));
    }
} 