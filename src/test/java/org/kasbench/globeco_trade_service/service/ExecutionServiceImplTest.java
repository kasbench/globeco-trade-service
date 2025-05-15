package org.kasbench.globeco_trade_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kasbench.globeco_trade_service.entity.*;
import org.kasbench.globeco_trade_service.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ExecutionServiceImplTest extends org.kasbench.globeco_trade_service.AbstractPostgresContainerTest {
    @Autowired
    private ExecutionService executionService;
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
    @Autowired
    private CacheManager cacheManager;

    private ExecutionStatus status;
    private Blotter blotter;
    private TradeType tradeType;
    private Destination destination;

    @BeforeEach
    void setup() {
        status = new ExecutionStatus();
        status.setAbbreviation("NEW");
        status.setDescription("New");
        status = executionStatusRepository.save(status);

        blotter = new Blotter();
        blotter.setAbbreviation("EQ");
        blotter.setName("Equity");
        blotter = blotterRepository.save(blotter);

        tradeType = new TradeType();
        tradeType.setAbbreviation("BUY");
        tradeType.setDescription("Buy");
        tradeType = tradeTypeRepository.save(tradeType);

        destination = new Destination();
        destination.setAbbreviation("ML");
        destination.setDescription("Merrill Lynch");
        destination = destinationRepository.save(destination);
    }

    private Execution buildExecution() {
        Execution execution = new Execution();
        execution.setExecutionTimestamp(OffsetDateTime.now());
        ExecutionStatus statusWithId = new ExecutionStatus();
        statusWithId.setId(status.getId());
        execution.setExecutionStatus(statusWithId);
        Blotter blotterWithId = new Blotter();
        blotterWithId.setId(blotter.getId());
        execution.setBlotter(blotterWithId);
        TradeType tradeTypeWithId = new TradeType();
        tradeTypeWithId.setId(tradeType.getId());
        execution.setTradeType(tradeTypeWithId);
        // Create a new TradeOrder with a globally unique orderId for each Execution
        TradeOrder newTradeOrder = new TradeOrder();
        newTradeOrder.setOrderId(ThreadLocalRandom.current().nextInt(1_000_000, Integer.MAX_VALUE));
        newTradeOrder.setPortfolioId("PORT1");
        newTradeOrder.setOrderType("BUY");
        newTradeOrder.setSecurityId("SEC1");
        newTradeOrder.setQuantity(new BigDecimal("100.00"));
        newTradeOrder.setLimitPrice(new BigDecimal("10.00"));
        newTradeOrder.setTradeTimestamp(OffsetDateTime.now());
        newTradeOrder.setBlotter(blotter);
        TradeOrder savedTradeOrder = tradeOrderRepository.save(newTradeOrder);
        TradeOrder tradeOrderWithId = new TradeOrder();
        tradeOrderWithId.setId(savedTradeOrder.getId());
        execution.setTradeOrder(tradeOrderWithId);
        Destination destinationWithId = new Destination();
        destinationWithId.setId(destination.getId());
        execution.setDestination(destinationWithId);
        execution.setQuantityOrdered((short) 10);
        execution.setQuantityPlaced(new BigDecimal("100.00"));
        execution.setQuantityFilled(new BigDecimal("0.00"));
        execution.setLimitPrice(new BigDecimal("10.00"));
        return execution;
    }

    @Test
    // @Disabled("Disabled: persistent failures, will revisit at the end of implementation")
    void testCreateAndGetExecution() {
        Execution execution = buildExecution();
        Execution created = executionService.createExecution(execution);
        assertNotNull(created.getId());
        Optional<Execution> found = executionService.getExecutionById(created.getId());
        assertTrue(found.isPresent());
        assertEquals(created.getId(), found.get().getId());
    }

    @Test
    // @Disabled("Disabled: persistent failures, will revisit at the end of implementation")
    void testUpdateExecution() {
        Execution execution = buildExecution();
        Execution created = executionService.createExecution(execution);
        created.setQuantityFilled(new BigDecimal("50.00"));
        Execution updated = executionService.updateExecution(created.getId(), created);
        assertEquals(new BigDecimal("50.00"), updated.getQuantityFilled());
    }

    @Test
    // @Disabled("Disabled: persistent failures, will revisit at the end of implementation")
    void testDeleteExecution() {
        Execution execution = buildExecution();
        Execution created = executionService.createExecution(execution);
        executionService.deleteExecution(created.getId(), created.getVersion());
        assertFalse(executionService.getExecutionById(created.getId()).isPresent());
    }

    @Test
    // @Disabled("Disabled: persistent failures, will revisit at the end of implementation")
    void testDeleteExecutionVersionMismatch() {
        Execution execution = buildExecution();
        Execution created = executionService.createExecution(execution);
        assertThrows(IllegalArgumentException.class, () -> executionService.deleteExecution(created.getId(), created.getVersion() + 1));
    }

    @SuppressWarnings("null")
    @Test
    // @Disabled("Disabled: persistent failures, will revisit at the end of implementation")
    void testGetAllExecutionsUsesCache() {
        cacheManager.getCache("executions").clear();
        Execution execution = buildExecution();
        executionService.createExecution(execution);
        // First call populates cache
        executionService.getAllExecutions();
        // Second call should hit cache
        executionService.getAllExecutions();
        assertNotNull(cacheManager.getCache("executions").get(SimpleKey.EMPTY));
    }

    @SuppressWarnings("null")
    @Test
    // @Disabled("Disabled: persistent failures, will revisit at the end of implementation")
    void testGetExecutionByIdUsesCache() {
        cacheManager.getCache("executions").clear();
        Execution execution = buildExecution();
        Execution created = executionService.createExecution(execution);
        // First call populates cache
        executionService.getExecutionById(created.getId());
        // Second call should hit cache
        executionService.getExecutionById(created.getId());
        assertNotNull(cacheManager.getCache("executions").get(created.getId()));
    }

    @SuppressWarnings("null")
    @Test
    // @Disabled("Disabled: persistent failures, will revisit at the end of implementation")
    void testCacheEvictedOnCreateUpdateDelete() {
        cacheManager.getCache("executions").clear();
        Execution execution = buildExecution();
        executionService.createExecution(execution);
        executionService.getAllExecutions();
        assertNotNull(cacheManager.getCache("executions").get(SimpleKey.EMPTY));
        // Update
        Execution created = executionService.getAllExecutions().get(0);
        created.setQuantityFilled(new BigDecimal("25.00"));
        executionService.updateExecution(created.getId(), created);
        assertNull(cacheManager.getCache("executions").get(SimpleKey.EMPTY));
        // Create again
        executionService.createExecution(buildExecution());
        executionService.getAllExecutions();
        assertNotNull(cacheManager.getCache("executions").get(SimpleKey.EMPTY));
        // Delete
        Execution toDelete = executionService.getAllExecutions().get(0);
        executionService.deleteExecution(toDelete.getId(), toDelete.getVersion());
        assertNull(cacheManager.getCache("executions").get(SimpleKey.EMPTY));
    }
} 