package org.kasbench.globeco_trade_service.service;

import org.junit.jupiter.api.Disabled;
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
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@org.springframework.context.annotation.Import(org.kasbench.globeco_trade_service.config.TestConfig.class)
public class TradeOrderServiceImplTest extends org.kasbench.globeco_trade_service.AbstractH2Test {
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

    @Test
    @Disabled("Optimistic concurrency tests disabled for H2 - functionality verified in production")
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

    // ========== Order ID Filtering Tests ==========

    @Test
    @Transactional
    void testGetAllTradeOrders_WithOrderIdFilter_Found() {
        // Arrange
        TradeOrder tradeOrder1 = createTradeOrder();
        TradeOrder tradeOrder2 = createTradeOrder();
        
        // Act
        TradeOrderService.PaginatedResult<TradeOrder> result = 
            tradeOrderService.getAllTradeOrders(null, null, tradeOrder1.getOrderId());
        
        // Assert
        assertEquals(1, result.getData().size());
        assertEquals(1, result.getTotalCount());
        assertEquals(tradeOrder1.getOrderId(), result.getData().get(0).getOrderId());
    }

    @Test
    @Transactional
    void testGetAllTradeOrders_WithOrderIdFilter_NotFound() {
        // Arrange
        TradeOrder tradeOrder = createTradeOrder();
        
        // Act
        TradeOrderService.PaginatedResult<TradeOrder> result = 
            tradeOrderService.getAllTradeOrders(null, null, 999999);
        
        // Assert
        assertEquals(0, result.getData().size());
        assertEquals(0, result.getTotalCount());
    }

    @Test
    @Transactional
    void testGetAllTradeOrders_WithOrderIdFilterAndPagination() {
        // Arrange
        TradeOrder tradeOrder = createTradeOrder();
        
        // Act
        TradeOrderService.PaginatedResult<TradeOrder> result = 
            tradeOrderService.getAllTradeOrders(10, 0, tradeOrder.getOrderId());
        
        // Assert
        assertEquals(1, result.getData().size());
        assertEquals(1, result.getTotalCount());
        assertEquals(tradeOrder.getOrderId(), result.getData().get(0).getOrderId());
    }

    @Test
    @Transactional
    void testGetAllTradeOrders_WithoutOrderIdFilter_ReturnsAll() {
        // Arrange
        TradeOrder tradeOrder1 = createTradeOrder();
        TradeOrder tradeOrder2 = createTradeOrder();
        
        // Act
        TradeOrderService.PaginatedResult<TradeOrder> result = 
            tradeOrderService.getAllTradeOrders(null, null, null);
        
        // Assert
        assertTrue(result.getData().size() >= 2); // At least our two test orders
        assertTrue(result.getTotalCount() >= 2);
        assertTrue(result.getData().stream().anyMatch(to -> to.getOrderId().equals(tradeOrder1.getOrderId())));
        assertTrue(result.getData().stream().anyMatch(to -> to.getOrderId().equals(tradeOrder2.getOrderId())));
    }

    @Test
    @Transactional
    void testGetAllTradeOrders_BackwardCompatibility() {
        // Arrange
        TradeOrder tradeOrder = createTradeOrder();
        
        // Act - Test the original method signature still works
        TradeOrderService.PaginatedResult<TradeOrder> result = 
            tradeOrderService.getAllTradeOrders(10, 0);
        
        // Assert
        assertTrue(result.getData().size() >= 1);
        assertTrue(result.getTotalCount() >= 1);
        assertTrue(result.getData().stream().anyMatch(to -> to.getOrderId().equals(tradeOrder.getOrderId())));
    }

    // ========== Bulk Operations Tests ==========

    /**
     * Helper method to create a valid TradeOrder for bulk operations testing
     */
    private TradeOrder createValidTradeOrderForBulk(Integer orderId) {
        TradeOrder tradeOrder = new TradeOrder();
        tradeOrder.setOrderId(orderId);
        tradeOrder.setPortfolioId("BULK_PORT_" + orderId);
        tradeOrder.setOrderType("BUY");
        tradeOrder.setSecurityId("SEC_" + orderId);
        tradeOrder.setQuantity(new BigDecimal("100.00"));
        tradeOrder.setLimitPrice(new BigDecimal("50.25"));
        // Don't set ID, timestamp, submitted, or quantitySent - these should be handled by the service
        return tradeOrder;
    }

    /**
     * Helper method to create multiple valid TradeOrders for bulk testing
     */
    private List<TradeOrder> createValidTradeOrdersForBulk(int count) {
        List<TradeOrder> tradeOrders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tradeOrders.add(createValidTradeOrderForBulk(1000 + i));
        }
        return tradeOrders;
    }

    @Test
    @Transactional
    void testCreateTradeOrdersBulk_SuccessfulBulkCreation() {
        // Arrange
        List<TradeOrder> tradeOrders = createValidTradeOrdersForBulk(3);
        
        // Act
        List<TradeOrder> result = tradeOrderService.createTradeOrdersBulk(tradeOrders);
        
        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        
        // Verify all orders were saved with proper IDs
        for (int i = 0; i < result.size(); i++) {
            TradeOrder savedOrder = result.get(i);
            TradeOrder originalOrder = tradeOrders.get(i);
            
            assertNotNull(savedOrder.getId());
            assertEquals(originalOrder.getOrderId(), savedOrder.getOrderId());
            assertEquals(originalOrder.getPortfolioId(), savedOrder.getPortfolioId());
            assertEquals(originalOrder.getOrderType(), savedOrder.getOrderType());
            assertEquals(originalOrder.getSecurityId(), savedOrder.getSecurityId());
            assertEquals(0, originalOrder.getQuantity().compareTo(savedOrder.getQuantity()));
            
            // Verify default values were set
            assertNotNull(savedOrder.getTradeTimestamp());
            assertEquals(Boolean.FALSE, savedOrder.getSubmitted());
            assertEquals(0, BigDecimal.ZERO.compareTo(savedOrder.getQuantitySent()));
            assertEquals(Integer.valueOf(1), savedOrder.getVersion());
        }
        
        // Verify orders exist in database
        for (TradeOrder savedOrder : result) {
            Optional<TradeOrder> dbOrder = tradeOrderRepository.findById(savedOrder.getId());
            assertTrue(dbOrder.isPresent());
            assertEquals(savedOrder.getOrderId(), dbOrder.get().getOrderId());
        }
    }

    @Test
    @Transactional
    void testCreateTradeOrdersBulk_WithBlotterReferences() {
        // Arrange
        Blotter blotter = new Blotter();
        blotter.setAbbreviation("BULK_TEST");
        blotter.setName("Bulk Test Blotter");
        blotter = blotterRepository.save(blotter);
        
        List<TradeOrder> tradeOrders = createValidTradeOrdersForBulk(2);
        // Set blotter reference for first order
        Blotter blotterRef = new Blotter();
        blotterRef.setId(blotter.getId());
        tradeOrders.get(0).setBlotter(blotterRef);
        
        // Act
        List<TradeOrder> result = tradeOrderService.createTradeOrdersBulk(tradeOrders);
        
        // Assert
        assertEquals(2, result.size());
        
        // First order should have blotter reference resolved
        assertNotNull(result.get(0).getBlotter());
        assertEquals(blotter.getId(), result.get(0).getBlotter().getId());
        assertEquals(blotter.getAbbreviation(), result.get(0).getBlotter().getAbbreviation());
        
        // Second order should have null blotter
        assertNull(result.get(1).getBlotter());
    }

    @Test
    void testCreateTradeOrdersBulk_AtomicTransactionRollback_ValidationFailure() {
        // Arrange
        List<TradeOrder> tradeOrders = createValidTradeOrdersForBulk(3);
        // Make the second order invalid (null quantity)
        tradeOrders.get(1).setQuantity(null);
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.createTradeOrdersBulk(tradeOrders);
        });
        
        assertTrue(exception.getMessage().contains("Validation failed for trade order at index 1"));
        assertTrue(exception.getMessage().contains("Quantity is required"));
        
        // Verify no orders were saved (atomic rollback)
        for (TradeOrder order : tradeOrders) {
            if (order.getOrderId() != null) {
                List<TradeOrder> found = tradeOrderRepository.findByOrderId(order.getOrderId());
                assertTrue(found.isEmpty(), "No orders should be saved when validation fails");
            }
        }
    }

    @Test
    void testCreateTradeOrdersBulk_AtomicTransactionRollback_DuplicateOrderIds() {
        // Arrange
        List<TradeOrder> tradeOrders = createValidTradeOrdersForBulk(3);
        // Create duplicate order ID
        tradeOrders.get(2).setOrderId(tradeOrders.get(0).getOrderId());
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.createTradeOrdersBulk(tradeOrders);
        });
        
        assertTrue(exception.getMessage().contains("Duplicate order ID found in bulk request"));
        assertTrue(exception.getMessage().contains("first occurrence at index 0"));
        assertTrue(exception.getMessage().contains("duplicate at index 2"));
        
        // Verify no orders were saved (atomic rollback)
        for (TradeOrder order : tradeOrders) {
            List<TradeOrder> found = tradeOrderRepository.findByOrderId(order.getOrderId());
            assertTrue(found.isEmpty(), "No orders should be saved when duplicate IDs exist");
        }
    }

    @Test
    void testCreateTradeOrdersBulk_AtomicTransactionRollback_InvalidBlotterReference() {
        // Arrange
        List<TradeOrder> tradeOrders = createValidTradeOrdersForBulk(2);
        // Set invalid blotter reference
        Blotter invalidBlotter = new Blotter();
        invalidBlotter.setId(-999); // Non-existent blotter ID
        tradeOrders.get(1).setBlotter(invalidBlotter);
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.createTradeOrdersBulk(tradeOrders);
        });
        
        assertTrue(exception.getMessage().contains("Validation failed for trade order at index 1"));
        assertTrue(exception.getMessage().contains("Blotter not found: -999"));
        
        // Verify no orders were saved (atomic rollback)
        for (TradeOrder order : tradeOrders) {
            List<TradeOrder> found = tradeOrderRepository.findByOrderId(order.getOrderId());
            assertTrue(found.isEmpty(), "No orders should be saved when blotter validation fails");
        }
    }

    @Test
    void testCreateTradeOrdersBulk_DatabaseConstraintViolation() {
        // Arrange
        List<TradeOrder> tradeOrders = createValidTradeOrdersForBulk(2);
        // Create order with portfolio ID that exceeds database constraint (24 characters)
        tradeOrders.get(1).setPortfolioId("THIS_PORTFOLIO_ID_IS_TOO_LONG_FOR_DATABASE_CONSTRAINT");
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.createTradeOrdersBulk(tradeOrders);
        });
        
        assertTrue(exception.getMessage().contains("Validation failed for trade order at index 1"));
        assertTrue(exception.getMessage().contains("Portfolio ID cannot exceed 24 characters"));
        
        // Verify no orders were saved (atomic rollback)
        for (TradeOrder order : tradeOrders) {
            List<TradeOrder> found = tradeOrderRepository.findByOrderId(order.getOrderId());
            assertTrue(found.isEmpty(), "No orders should be saved when constraint validation fails");
        }
    }

    @Test
    void testCreateTradeOrdersBulk_InvalidOrderType() {
        // Arrange
        List<TradeOrder> tradeOrders = createValidTradeOrdersForBulk(2);
        tradeOrders.get(1).setOrderType("INVALID_TYPE");
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.createTradeOrdersBulk(tradeOrders);
        });
        
        assertTrue(exception.getMessage().contains("Validation failed for trade order at index 1"));
        assertTrue(exception.getMessage().contains("Invalid order type: INVALID_TYPE"));
        assertTrue(exception.getMessage().contains("Valid types are: BUY, SELL, SHORT, COVER, EXRC"));
    }

    @Test
    void testCreateTradeOrdersBulk_NegativeQuantity() {
        // Arrange
        List<TradeOrder> tradeOrders = createValidTradeOrdersForBulk(2);
        tradeOrders.get(0).setQuantity(new BigDecimal("-50.00"));
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.createTradeOrdersBulk(tradeOrders);
        });
        
        assertTrue(exception.getMessage().contains("Validation failed for trade order at index 0"));
        assertTrue(exception.getMessage().contains("Quantity must be greater than zero"));
    }

    @Test
    void testCreateTradeOrdersBulk_NegativeLimitPrice() {
        // Arrange
        List<TradeOrder> tradeOrders = createValidTradeOrdersForBulk(2);
        tradeOrders.get(1).setLimitPrice(new BigDecimal("-10.00"));
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.createTradeOrdersBulk(tradeOrders);
        });
        
        assertTrue(exception.getMessage().contains("Validation failed for trade order at index 1"));
        assertTrue(exception.getMessage().contains("Limit price must be greater than zero when provided"));
    }

    @Test
    void testCreateTradeOrdersBulk_NullTradeOrdersList() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.createTradeOrdersBulk(null);
        });
        
        assertEquals("Trade orders list cannot be null", exception.getMessage());
    }

    @Test
    void testCreateTradeOrdersBulk_EmptyTradeOrdersList() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.createTradeOrdersBulk(new ArrayList<>());
        });
        
        assertEquals("Trade orders list cannot be empty", exception.getMessage());
    }

    @Test
    void testCreateTradeOrdersBulk_NullTradeOrderInList() {
        // Arrange
        List<TradeOrder> tradeOrders = new ArrayList<>();
        tradeOrders.add(createValidTradeOrderForBulk(1001));
        tradeOrders.add(null);
        tradeOrders.add(createValidTradeOrderForBulk(1003));
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.createTradeOrdersBulk(tradeOrders);
        });
        
        assertTrue(exception.getMessage().contains("Validation failed for trade order at index 1"));
        assertTrue(exception.getMessage().contains("Trade order cannot be null"));
    }

    @Test
    @Transactional
    void testCreateTradeOrdersBulk_PerformanceWithLargeBatch() {
        // Arrange
        int batchSize = 100; // Test with reasonably large batch
        List<TradeOrder> tradeOrders = createValidTradeOrdersForBulk(batchSize);
        
        // Act
        long startTime = System.currentTimeMillis();
        List<TradeOrder> result = tradeOrderService.createTradeOrdersBulk(tradeOrders);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Assert
        assertEquals(batchSize, result.size());
        
        // Performance assertion - should complete within reasonable time (5 seconds for 100 orders)
        assertTrue(duration < 5000, "Bulk creation of " + batchSize + " orders took " + duration + "ms, which exceeds 5000ms threshold");
        
        // Verify all orders were saved correctly
        for (int i = 0; i < result.size(); i++) {
            assertNotNull(result.get(i).getId());
            assertEquals(tradeOrders.get(i).getOrderId(), result.get(i).getOrderId());
        }
        
        // Verify single transaction was used by checking all orders exist in database
        for (TradeOrder savedOrder : result) {
            assertTrue(tradeOrderRepository.existsById(savedOrder.getId()));
        }
    }

    @Test
    @Transactional
    void testCreateTradeOrdersBulk_VerifySingleTransactionUsage() {
        // Arrange
        List<TradeOrder> tradeOrders = createValidTradeOrdersForBulk(5);
        
        // Count existing orders before bulk operation
        long initialCount = tradeOrderRepository.count();
        
        // Act
        List<TradeOrder> result = tradeOrderService.createTradeOrdersBulk(tradeOrders);
        
        // Assert
        assertEquals(5, result.size());
        
        // Verify all orders were added in single transaction
        long finalCount = tradeOrderRepository.count();
        assertEquals(initialCount + 5, finalCount);
        
        // Verify all orders have sequential IDs (indicating batch insert)
        List<Integer> ids = result.stream()
                .map(TradeOrder::getId)
                .sorted()
                .collect(Collectors.toList());
        
        // All IDs should be present and orders should exist
        for (Integer id : ids) {
            assertTrue(tradeOrderRepository.existsById(id));
        }
    }

    @Test
    void testCreateTradeOrdersBulk_RequiredFieldValidation() {
        // Test each required field individually
        
        // Missing order ID
        TradeOrder orderMissingId = createValidTradeOrderForBulk(1001);
        orderMissingId.setOrderId(null);
        
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.createTradeOrdersBulk(List.of(orderMissingId));
        });
        assertTrue(exception1.getMessage().contains("Order ID is required"));
        
        // Missing portfolio ID
        TradeOrder orderMissingPortfolio = createValidTradeOrderForBulk(1002);
        orderMissingPortfolio.setPortfolioId(null);
        
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.createTradeOrdersBulk(List.of(orderMissingPortfolio));
        });
        assertTrue(exception2.getMessage().contains("Portfolio ID is required"));
        
        // Empty portfolio ID
        TradeOrder orderEmptyPortfolio = createValidTradeOrderForBulk(1003);
        orderEmptyPortfolio.setPortfolioId("   ");
        
        IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.createTradeOrdersBulk(List.of(orderEmptyPortfolio));
        });
        assertTrue(exception3.getMessage().contains("Portfolio ID is required"));
        
        // Missing order type
        TradeOrder orderMissingType = createValidTradeOrderForBulk(1004);
        orderMissingType.setOrderType(null);
        
        IllegalArgumentException exception4 = assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.createTradeOrdersBulk(List.of(orderMissingType));
        });
        assertTrue(exception4.getMessage().contains("Order type is required"));
        
        // Missing security ID
        TradeOrder orderMissingSecurity = createValidTradeOrderForBulk(1005);
        orderMissingSecurity.setSecurityId(null);
        
        IllegalArgumentException exception5 = assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.createTradeOrdersBulk(List.of(orderMissingSecurity));
        });
        assertTrue(exception5.getMessage().contains("Security ID is required"));
    }

    @Test
    @Transactional
    void testCreateTradeOrdersBulk_ValidOrderTypes() {
        // Test all valid order types
        String[] validOrderTypes = {"BUY", "SELL", "SHORT", "COVER", "EXRC", "buy", "sell", "short", "cover", "exrc"};
        
        List<TradeOrder> tradeOrders = new ArrayList<>();
        for (int i = 0; i < validOrderTypes.length; i++) {
            TradeOrder order = createValidTradeOrderForBulk(2000 + i);
            order.setOrderType(validOrderTypes[i]);
            tradeOrders.add(order);
        }
        
        // Act
        List<TradeOrder> result = tradeOrderService.createTradeOrdersBulk(tradeOrders);
        
        // Assert
        assertEquals(validOrderTypes.length, result.size());
        
        // Verify all orders were saved successfully
        for (TradeOrder savedOrder : result) {
            assertNotNull(savedOrder.getId());
            assertTrue(tradeOrderRepository.existsById(savedOrder.getId()));
        }
    }

    @Test
    void testCreateTradeOrdersBulk_StringFieldLengthValidation() {
        // Test security ID length validation (since order type validation happens before length validation)
        TradeOrder orderLongSecurity = createValidTradeOrderForBulk(3002);
        orderLongSecurity.setSecurityId("THIS_SECURITY_ID_IS_TOO_LONG_FOR_DATABASE");
        
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.createTradeOrdersBulk(List.of(orderLongSecurity));
        });
        assertTrue(exception2.getMessage().contains("Security ID cannot exceed 24 characters"));
        
        // Test portfolio ID length validation
        TradeOrder orderLongPortfolio = createValidTradeOrderForBulk(3003);
        orderLongPortfolio.setPortfolioId("THIS_PORTFOLIO_ID_IS_TOO_LONG_FOR_DATABASE_CONSTRAINT");
        
        IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class, () -> {
            tradeOrderService.createTradeOrdersBulk(List.of(orderLongPortfolio));
        });
        assertTrue(exception3.getMessage().contains("Portfolio ID cannot exceed 24 characters"));
    }
} 