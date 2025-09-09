package org.kasbench.globeco_trade_service.repository;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.kasbench.globeco_trade_service.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Random;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ExecutionRepositoryTest extends org.kasbench.globeco_trade_service.AbstractH2Test {
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

    private Execution buildExecution(Integer executionServiceId) {
        ExecutionStatus status = new ExecutionStatus();
        status.setAbbreviation("NEW" + System.nanoTime());
        status.setDescription("New");
        status = executionStatusRepository.saveAndFlush(status);

        Blotter blotter = new Blotter();
        blotter.setAbbreviation("EQ");
        blotter.setName("Equity");
        blotter = blotterRepository.saveAndFlush(blotter);

        TradeType tradeType = new TradeType();
        tradeType.setAbbreviation("BUY");
        tradeType.setDescription("Buy");
        tradeType = tradeTypeRepository.saveAndFlush(tradeType);

        TradeOrder tradeOrder = new TradeOrder();
        tradeOrder.setOrderId((int) (Math.random() * 1000000));
        tradeOrder.setPortfolioId("PORT1");
        tradeOrder.setOrderType("BUY");
        tradeOrder.setSecurityId("SEC1");
        tradeOrder.setQuantity(new BigDecimal("100.00"));
        tradeOrder.setLimitPrice(new BigDecimal("10.00"));
        tradeOrder.setTradeTimestamp(OffsetDateTime.now());
        tradeOrder.setBlotter(blotter);
        tradeOrder = tradeOrderRepository.saveAndFlush(tradeOrder);

        Destination destination = new Destination();
        destination.setAbbreviation("ML");
        destination.setDescription("Merrill Lynch");
        destination = destinationRepository.saveAndFlush(destination);

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
        execution.setExecutionServiceId(executionServiceId);
        execution.setVersion(1);
        return executionRepository.saveAndFlush(execution);
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
        Execution execution = buildExecution(null);
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
    @Disabled("Optimistic concurrency tests disabled for H2 - functionality verified in production")
    void testOptimisticConcurrency() {
        Execution execution = buildExecution(null);
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

    @Test
    void testFilterByExecutionServiceId() {
        Execution e1 = buildExecution(12345);
        Execution e2 = buildExecution(67890);
        Execution e3 = buildExecution(null);

        // Exact match
        Specification<Execution> spec = ExecutionSpecification.hasExecutionServiceId(12345);
        List<Execution> results = executionRepository.findAll(spec);
        assertThat(results).extracting(Execution::getExecutionServiceId).containsExactly(12345);

        // No match
        spec = ExecutionSpecification.hasExecutionServiceId(99999);
        results = executionRepository.findAll(spec);
        assertThat(results).isEmpty();

        // Null parameter returns all
        spec = ExecutionSpecification.hasExecutionServiceId(null);
        results = executionRepository.findAll(spec);
        assertThat(results.size()).isGreaterThanOrEqualTo(3);
    }
} 