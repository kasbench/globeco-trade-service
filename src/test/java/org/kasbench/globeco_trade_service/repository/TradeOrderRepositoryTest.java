package org.kasbench.globeco_trade_service.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.AfterEach;
import org.kasbench.globeco_trade_service.entity.Blotter;
import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import java.util.Random;

@SpringBootTest
public class TradeOrderRepositoryTest extends org.kasbench.globeco_trade_service.AbstractPostgresContainerTest {
    @Autowired
    private TradeOrderRepository tradeOrderRepository;
    @Autowired
    private BlotterRepository blotterRepository;

    private TradeOrder tradeOrder;
    private Blotter blotter;

    private static String randomAlphaNum(int len) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private TradeOrder createTradeOrder() {
        blotter = new Blotter();
        blotter.setAbbreviation("EQ" + ThreadLocalRandom.current().nextInt(1_000_000));
        blotter.setName("Equity" + ThreadLocalRandom.current().nextInt(1_000_000));
        blotter = blotterRepository.saveAndFlush(blotter);

        tradeOrder = new TradeOrder();
        tradeOrder.setOrderId(ThreadLocalRandom.current().nextInt(1_000_000, 2_000_000));
        tradeOrder.setPortfolioId(randomAlphaNum(12));
        tradeOrder.setOrderType("BUY");
        tradeOrder.setSecurityId(randomAlphaNum(12));
        tradeOrder.setQuantity(new BigDecimal("100.25"));
        tradeOrder.setLimitPrice(new BigDecimal("10.50"));
        tradeOrder.setTradeTimestamp(OffsetDateTime.now());
        tradeOrder.setBlotter(blotter);
        return tradeOrderRepository.saveAndFlush(tradeOrder);
    }

    @AfterEach
    void tearDown() {
        if (tradeOrder != null && tradeOrder.getId() != null) {
            tradeOrderRepository.deleteById(tradeOrder.getId());
        }
        if (blotter != null && blotter.getId() != null) {
            blotterRepository.deleteById(blotter.getId());
        }
    }

    @Test
    void testCrud() {
        tradeOrder = createTradeOrder();
        Integer id = tradeOrder.getId();
        Assertions.assertTrue(tradeOrderRepository.findById(id).isPresent());
        tradeOrder.setOrderType("SELL");
        tradeOrderRepository.saveAndFlush(tradeOrder);
        TradeOrder found = tradeOrderRepository.findById(id).orElseThrow();
        Assertions.assertEquals("SELL", found.getOrderType().trim());
        tradeOrderRepository.deleteById(id);
        Assertions.assertTrue(tradeOrderRepository.findById(id).isEmpty());
        tradeOrder = null; // Prevent double delete in @AfterEach
    }

    // @Disabled("Disabled: persistent failures with optimistic locking exception detection in test environment")
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
} 