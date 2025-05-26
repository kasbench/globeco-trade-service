package org.kasbench.globeco_trade_service.repository;

import org.junit.jupiter.api.Test;
import org.kasbench.globeco_trade_service.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Assertions;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ExecutionRepositoryTest extends org.kasbench.globeco_trade_service.AbstractPostgresContainerTest {
    @Autowired
    private ExecutionRepository executionRepository;
    @Autowired
    private ExecutionStatusRepository executionStatusRepository;
    @Autowired
    private BlotterRepository blotterRepository;
    @Autowired
    private TradeTypeRepository tradeTypeRepository;
    @Autowired
    private TradeOrderRepository tradeOrderRepository;
    @Autowired
    private DestinationRepository destinationRepository;

    private Execution buildExecution() {
        ExecutionStatus status = new ExecutionStatus();
        status.setAbbreviation("NEW" + ThreadLocalRandom.current().nextInt(1_000_000));
        status.setDescription("New" + ThreadLocalRandom.current().nextInt(1_000_000));
        status = executionStatusRepository.save(status);

        Blotter blotter = new Blotter();
        blotter.setAbbreviation("EQ" + ThreadLocalRandom.current().nextInt(1_000_000));
        blotter.setName("Equity" + ThreadLocalRandom.current().nextInt(1_000_000));
        blotter = blotterRepository.save(blotter);

        TradeType tradeType = new TradeType();
        tradeType.setAbbreviation("BUY" + ThreadLocalRandom.current().nextInt(1_000_000));
        tradeType.setDescription("Buy" + ThreadLocalRandom.current().nextInt(1_000_000));
        tradeType = tradeTypeRepository.save(tradeType);

        TradeOrder tradeOrder = new TradeOrder();
        tradeOrder.setOrderId(ThreadLocalRandom.current().nextInt(1_000_000, 2_000_000));
        tradeOrder.setPortfolioId(randomAlphaNum(12));
        tradeOrder.setOrderType("BUY");
        tradeOrder.setSecurityId(randomAlphaNum(12));
        tradeOrder.setQuantity(new BigDecimal("100.00"));
        tradeOrder.setLimitPrice(new BigDecimal("10.00"));
        tradeOrder.setTradeTimestamp(OffsetDateTime.now());
        tradeOrder.setBlotter(blotter);
        tradeOrder = tradeOrderRepository.save(tradeOrder);

        Destination destination = new Destination();
        destination.setAbbreviation("ML" + ThreadLocalRandom.current().nextInt(1_000_000));
        destination.setDescription("Merrill Lynch" + ThreadLocalRandom.current().nextInt(1_000_000));
        destination = destinationRepository.save(destination);

        Execution execution = new Execution();
        execution.setExecutionTimestamp(OffsetDateTime.now());
        execution.setExecutionStatus(status);
        execution.setBlotter(blotter);
        execution.setTradeType(tradeType);
        execution.setTradeOrder(tradeOrder);
        execution.setDestination(destination);
        execution.setQuantityOrdered(new BigDecimal("10.00"));
        execution.setQuantityPlaced(new BigDecimal("100.00"));
        execution.setQuantityFilled(new BigDecimal("0.00"));
        execution.setLimitPrice(new BigDecimal("10.00"));
        return execution;
    }

    private static String randomAlphaNum(int len) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Test
    void testCrud() {
        Execution execution = buildExecution();
        Execution saved = executionRepository.save(execution);
        assertNotNull(saved.getId());
        Execution found = executionRepository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals(saved.getId(), found.getId());
        found.setQuantityFilled(new BigDecimal("50.00"));
        executionRepository.save(found);
        Execution updated = executionRepository.findById(found.getId()).orElse(null);
        Assertions.assertEquals(0, updated.getQuantityFilled().compareTo(new BigDecimal("50.00")));
        executionRepository.deleteById(updated.getId());
        assertFalse(executionRepository.findById(updated.getId()).isPresent());
    }

    @Test
    void testOptimisticConcurrency() {
        Execution execution = buildExecution();
        Execution saved = executionRepository.save(execution);
        Execution e1 = executionRepository.findById(saved.getId()).get();
        Execution e2 = executionRepository.findById(saved.getId()).get();
        e1.setQuantityFilled(new BigDecimal("10.00"));
        executionRepository.saveAndFlush(e1);
        e2.setQuantityFilled(new BigDecimal("20.00"));
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.OptimisticLockingFailureException.class, () -> executionRepository.saveAndFlush(e2));
        // Clean up
        executionRepository.deleteById(saved.getId());
    }
} 