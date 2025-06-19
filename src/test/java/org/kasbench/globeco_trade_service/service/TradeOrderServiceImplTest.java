package org.kasbench.globeco_trade_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.kasbench.globeco_trade_service.entity.Blotter;
import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.kasbench.globeco_trade_service.entity.Execution;
import org.kasbench.globeco_trade_service.entity.ExecutionStatus;
import org.kasbench.globeco_trade_service.entity.Destination;
import org.kasbench.globeco_trade_service.entity.TradeType;
import org.kasbench.globeco_trade_service.dto.TradeOrderSubmitDTO;
import org.kasbench.globeco_trade_service.repository.BlotterRepository;
import org.kasbench.globeco_trade_service.repository.TradeOrderRepository;
import org.kasbench.globeco_trade_service.repository.ExecutionRepository;
import org.kasbench.globeco_trade_service.repository.ExecutionStatusRepository;
import org.kasbench.globeco_trade_service.repository.DestinationRepository;
import org.kasbench.globeco_trade_service.repository.TradeTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.cache.CacheManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class TradeOrderServiceImplTest extends org.kasbench.globeco_trade_service.AbstractPostgresContainerTest {
    @Autowired
    private TradeOrderService tradeOrderService;
    @Autowired
    private TradeOrderRepository tradeOrderRepository;
    @Autowired
    private BlotterRepository blotterRepository;
    @Autowired
    private ExecutionRepository executionRepository;
    @Autowired
    private ExecutionStatusRepository executionStatusRepository;
    @Autowired
    private DestinationRepository destinationRepository;
    @Autowired
    private TradeTypeRepository tradeTypeRepository;
    @Autowired
    private CacheManager cacheManager;
    
    @MockBean
    private ExecutionService executionService;

    private ExecutionStatus executionStatus;
    private Destination destination;
    private TradeType tradeType;

    @BeforeEach
    void setUp() {
        // Reset mock before each test
        reset(executionService);
        
        // Clean up executions from previous tests to avoid accumulation
        executionRepository.deleteAll();
        
        // Create test data
        executionStatus = new ExecutionStatus();
        executionStatus.setAbbreviation("NEW");
        executionStatus.setDescription("New");
        executionStatus = executionStatusRepository.save(executionStatus);

        destination = new Destination();
        destination.setAbbreviation("DEST1");
        destination.setDescription("Test Destination");
        destination = destinationRepository.save(destination);

        tradeType = new TradeType();
        tradeType.setAbbreviation("BUY");
        tradeType.setDescription("Buy");
        tradeType = tradeTypeRepository.save(tradeType);
    }

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

    private TradeOrderSubmitDTO createSubmitDTO() {
        TradeOrderSubmitDTO dto = new TradeOrderSubmitDTO();
        dto.setQuantity(new BigDecimal("50.00"));
        dto.setDestinationId(destination.getId());
        return dto;
    }

    @Test
    @Transactional
    void testCreateAndGet() {
        TradeOrder tradeOrder = createTradeOrder();
        Optional<TradeOrder> found = tradeOrderService.getTradeOrderById(tradeOrder.getId());
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals("PORT123", found.get().getPortfolioId());
        Assertions.assertEquals(false, found.get().getSubmitted());
    }

    @Test
    @Transactional
    void testUpdate() {
        TradeOrder tradeOrder = createTradeOrder();
        tradeOrder.setOrderType("SELL");
        tradeOrder.setSubmitted(true);
        TradeOrder updated = tradeOrderService.updateTradeOrder(tradeOrder.getId(), tradeOrder);
        Assertions.assertEquals("SELL", updated.getOrderType());
        Assertions.assertEquals(true, updated.getSubmitted());
    }

    @Test
    @Transactional
    void testDelete() {
        TradeOrder tradeOrder = createTradeOrder();
        tradeOrderService.deleteTradeOrder(tradeOrder.getId(), tradeOrder.getVersion());
        Assertions.assertTrue(tradeOrderService.getTradeOrderById(tradeOrder.getId()).isEmpty());
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

    // ========== Phase 3 Tests: New Functionality ==========

    @Test
    @Transactional
    void testSubmitTradeOrderWithNoExecuteSubmitTrue_LegacyBehavior() {
        // Arrange
        TradeOrder tradeOrder = createTradeOrder();
        TradeOrderSubmitDTO submitDTO = createSubmitDTO();
        
        // Act
        Execution execution = tradeOrderService.submitTradeOrder(tradeOrder.getId(), submitDTO, true);
        
        // Assert
        assertNotNull(execution);
        assertEquals(new BigDecimal("50.00"), execution.getQuantityOrdered());
        assertEquals(destination.getId(), execution.getDestination().getId());
        assertNull(execution.getExecutionServiceId()); // Should not be submitted to external service
        
        // Verify execution service was not called
        verifyNoInteractions(executionService);
        
        // Verify trade order state
        TradeOrder updatedTradeOrder = tradeOrderRepository.findById(tradeOrder.getId()).orElseThrow();
        assertEquals(new BigDecimal("50.00"), updatedTradeOrder.getQuantitySent());
        assertFalse(updatedTradeOrder.getSubmitted()); // Should not be fully submitted
    }

    @Test
    @Transactional
    void testSubmitTradeOrderWithNoExecuteSubmitFalse_AutoSubmitsToExecutionService() {
        // Arrange
        TradeOrder tradeOrder = createTradeOrder();
        TradeOrderSubmitDTO submitDTO = createSubmitDTO();
        
        ExecutionService.SubmitResult successResult = new ExecutionService.SubmitResult("submitted", null);
        when(executionService.submitExecution(any(Integer.class))).thenReturn(successResult);
        
        // Act
        Execution execution = tradeOrderService.submitTradeOrder(tradeOrder.getId(), submitDTO, false);
        
        // Assert
        assertNotNull(execution);
        assertEquals(new BigDecimal("50.00"), execution.getQuantityOrdered());
        assertEquals(destination.getId(), execution.getDestination().getId());
        
        // Verify execution service was called
        verify(executionService, times(1)).submitExecution(execution.getId());
        
        // Verify trade order state
        TradeOrder updatedTradeOrder = tradeOrderRepository.findById(tradeOrder.getId()).orElseThrow();
        assertEquals(new BigDecimal("50.00"), updatedTradeOrder.getQuantitySent());
    }

    @Test
    @Transactional
    void testSubmitTradeOrderDefaultBehavior_AutoSubmitsToExecutionService() {
        // Arrange
        TradeOrder tradeOrder = createTradeOrder();
        TradeOrderSubmitDTO submitDTO = createSubmitDTO();
        
        ExecutionService.SubmitResult successResult = new ExecutionService.SubmitResult("submitted", null);
        when(executionService.submitExecution(any(Integer.class))).thenReturn(successResult);
        
        // Act - using default method without noExecuteSubmit parameter
        Execution execution = tradeOrderService.submitTradeOrder(tradeOrder.getId(), submitDTO);
        
        // Assert
        assertNotNull(execution);
        
        // Verify execution service was called (default is false for noExecuteSubmit)
        verify(executionService, times(1)).submitExecution(execution.getId());
    }

    @Test
    void testSubmitTradeOrder_CompensatingTransactionOnExecutionServiceFailure() {
        // Arrange
        TradeOrder tradeOrder = createTradeOrder();
        TradeOrderSubmitDTO submitDTO = createSubmitDTO();
        BigDecimal originalQuantitySent = tradeOrder.getQuantitySent();
        Boolean originalSubmitted = tradeOrder.getSubmitted();
        
        ExecutionService.SubmitResult failureResult = new ExecutionService.SubmitResult(null, "Service unavailable");
        when(executionService.submitExecution(any(Integer.class))).thenReturn(failureResult);
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tradeOrderService.submitTradeOrder(tradeOrder.getId(), submitDTO, false);
        });
        
        assertTrue(exception.getMessage().contains("Execution service submission failed"));
        
        // Verify compensating transaction occurred
        TradeOrder compensatedTradeOrder = tradeOrderRepository.findById(tradeOrder.getId()).orElseThrow();
        assertEquals(0, originalQuantitySent.compareTo(compensatedTradeOrder.getQuantitySent()));
        assertEquals(originalSubmitted, compensatedTradeOrder.getSubmitted());
        
        // Verify execution record was deleted (compensating transaction)
        assertEquals(0, executionRepository.findAll().size());
    }

    @Test
    void testSubmitTradeOrder_CompensatingTransactionOnExecutionServiceException() {
        // Arrange
        TradeOrder tradeOrder = createTradeOrder();
        TradeOrderSubmitDTO submitDTO = createSubmitDTO();
        BigDecimal originalQuantitySent = tradeOrder.getQuantitySent();
        Boolean originalSubmitted = tradeOrder.getSubmitted();
        
        when(executionService.submitExecution(any(Integer.class)))
            .thenThrow(new RuntimeException("Network timeout"));
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tradeOrderService.submitTradeOrder(tradeOrder.getId(), submitDTO, false);
        });
        
        assertTrue(exception.getMessage().contains("Failed to submit execution to external service"));
        
        // Verify compensating transaction occurred
        TradeOrder compensatedTradeOrder = tradeOrderRepository.findById(tradeOrder.getId()).orElseThrow();
        assertEquals(0, originalQuantitySent.compareTo(compensatedTradeOrder.getQuantitySent()));
        assertEquals(originalSubmitted, compensatedTradeOrder.getSubmitted());
        
        // Verify execution record was deleted (compensating transaction)
        assertEquals(0, executionRepository.findAll().size());
    }

    @Test
    @Transactional
    void testSubmitTradeOrder_ClientErrorFromExecutionService() {
        // Arrange
        TradeOrder tradeOrder = createTradeOrder();
        TradeOrderSubmitDTO submitDTO = createSubmitDTO();
        
        HttpClientErrorException clientError = mock(HttpClientErrorException.class);
        RuntimeException wrappedException = new RuntimeException("Execution service submission failed: Bad request", clientError);
        
        when(executionService.submitExecution(any(Integer.class))).thenThrow(wrappedException);
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.submitTradeOrder(tradeOrder.getId(), submitDTO, false);
        });
        
        assertTrue(exception.getMessage().contains("Execution service rejected the request"));
        
        // Verify compensating transaction occurred
        assertEquals(0, executionRepository.findAll().size());
    }

    @Test
    @Transactional
    void testSubmitTradeOrder_FullQuantitySubmitted() {
        // Arrange
        TradeOrder tradeOrder = createTradeOrder();
        final BigDecimal originalQuantity = tradeOrder.getQuantity();
        TradeOrderSubmitDTO submitDTO = createSubmitDTO();
        submitDTO.setQuantity(originalQuantity); // Submit full quantity
        
        ExecutionService.SubmitResult successResult = new ExecutionService.SubmitResult("submitted", null);
        when(executionService.submitExecution(any(Integer.class))).thenReturn(successResult);
        
        // Act
        Execution execution = tradeOrderService.submitTradeOrder(tradeOrder.getId(), submitDTO, false);
        
        // Assert
        assertNotNull(execution);
        
        TradeOrder updatedTradeOrder = tradeOrderRepository.findById(tradeOrder.getId()).orElseThrow();
        assertEquals(originalQuantity, updatedTradeOrder.getQuantitySent());
        assertTrue(updatedTradeOrder.getSubmitted()); // Should be marked as fully submitted
    }

    @Test
    @Transactional
    void testSubmitTradeOrder_PartialQuantitySubmitted() {
        // Arrange
        TradeOrder tradeOrder = createTradeOrder();
        TradeOrderSubmitDTO submitDTO = createSubmitDTO();
        submitDTO.setQuantity(new BigDecimal("25.00")); // Submit partial quantity
        
        ExecutionService.SubmitResult successResult = new ExecutionService.SubmitResult("submitted", null);
        when(executionService.submitExecution(any(Integer.class))).thenReturn(successResult);
        
        // Act
        Execution execution = tradeOrderService.submitTradeOrder(tradeOrder.getId(), submitDTO, false);
        
        // Assert
        assertNotNull(execution);
        
        TradeOrder updatedTradeOrder = tradeOrderRepository.findById(tradeOrder.getId()).orElseThrow();
        assertEquals(new BigDecimal("25.00"), updatedTradeOrder.getQuantitySent());
        assertFalse(updatedTradeOrder.getSubmitted()); // Should not be marked as fully submitted
    }

    @Test
    void testSubmitTradeOrder_ExceedsAvailableQuantity() {
        // Arrange
        TradeOrder tradeOrder = createTradeOrder();
        TradeOrderSubmitDTO submitDTO = createSubmitDTO();
        submitDTO.setQuantity(tradeOrder.getQuantity().add(new BigDecimal("50.00"))); // Exceed available
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.submitTradeOrder(tradeOrder.getId(), submitDTO, true);
        });
        
        assertTrue(exception.getMessage().contains("exceeds available quantity"));
        
        // Verify no execution was created
        assertEquals(0, executionRepository.findAll().size());
    }

    @Test
    @Transactional
    void testSubmitTradeOrder_TradeOrderNotFound() {
        // Arrange
        TradeOrderSubmitDTO submitDTO = createSubmitDTO();
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.submitTradeOrder(-1, submitDTO, true);
        });
        
        assertTrue(exception.getMessage().contains("TradeOrder not found"));
    }

    @Test
    @Transactional
    void testSubmitTradeOrder_DestinationNotFound() {
        // Arrange
        TradeOrder tradeOrder = createTradeOrder();
        TradeOrderSubmitDTO submitDTO = createSubmitDTO();
        submitDTO.setDestinationId(-1); // Non-existent destination
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.submitTradeOrder(tradeOrder.getId(), submitDTO, true);
        });
        
        assertTrue(exception.getMessage().contains("Destination not found"));
    }

    @Test
    @Transactional
    void testSubmitTradeOrder_NullQuantity() {
        // Arrange
        TradeOrder tradeOrder = createTradeOrder();
        TradeOrderSubmitDTO submitDTO = createSubmitDTO();
        submitDTO.setQuantity(null);
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.submitTradeOrder(tradeOrder.getId(), submitDTO, true);
        });
        
        assertTrue(exception.getMessage().contains("Quantity must not be null"));
    }

    @Test
    @Transactional
    void testSubmitTradeOrder_UnknownOrderType() {
        // Arrange
        TradeOrder tradeOrder = createTradeOrder();
        tradeOrder.setOrderType("UNKNOWN");
        final TradeOrder savedTradeOrder = tradeOrderRepository.save(tradeOrder);
        
        TradeOrderSubmitDTO submitDTO = createSubmitDTO();
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.submitTradeOrder(savedTradeOrder.getId(), submitDTO, true);
        });
        
        assertTrue(exception.getMessage().contains("Unknown order_type"));
    }
} 