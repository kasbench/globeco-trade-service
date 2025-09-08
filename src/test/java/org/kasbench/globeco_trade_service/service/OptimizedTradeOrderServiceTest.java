package org.kasbench.globeco_trade_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.support.RetryTemplate;

import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.kasbench.globeco_trade_service.entity.Execution;
import org.kasbench.globeco_trade_service.entity.ExecutionStatus;
import org.kasbench.globeco_trade_service.entity.TradeType;
import org.kasbench.globeco_trade_service.entity.Destination;
import org.kasbench.globeco_trade_service.entity.Blotter;
import org.kasbench.globeco_trade_service.repository.TradeOrderRepository;
import org.kasbench.globeco_trade_service.repository.ExecutionRepository;
import org.kasbench.globeco_trade_service.repository.TradeTypeRepository;
import org.kasbench.globeco_trade_service.repository.ExecutionStatusRepository;
import org.kasbench.globeco_trade_service.repository.DestinationRepository;
import org.kasbench.globeco_trade_service.dto.TradeOrderSubmitDTO;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OptimizedTradeOrderServiceTest {

    @Mock
    private TradeOrderRepository tradeOrderRepository;
    
    @Mock
    private ExecutionRepository executionRepository;
    
    @Mock
    private TradeTypeRepository tradeTypeRepository;
    
    @Mock
    private ExecutionStatusRepository executionStatusRepository;
    
    @Mock
    private DestinationRepository destinationRepository;
    
    @Mock
    private ExecutionService executionService;
    
    @Mock
    private RetryTemplate retryTemplate;
    
    @Mock
    private TransactionCompensationHandler compensationHandler;
    
    @InjectMocks
    private OptimizedTradeOrderService optimizedTradeOrderService;
    
    private TradeOrder testTradeOrder;
    private TradeOrderSubmitDTO testSubmitDTO;
    private TradeType testTradeType;
    private ExecutionStatus testExecutionStatus;
    private Destination testDestination;
    private Blotter testBlotter;
    
    @BeforeEach
    void setUp() {
        // Set up test data
        testBlotter = new Blotter();
        testBlotter.setId(1);
        testBlotter.setName("Test Blotter");
        
        testTradeOrder = new TradeOrder();
        testTradeOrder.setId(1);
        testTradeOrder.setOrderId(12345);
        testTradeOrder.setPortfolioId("PORTFOLIO1");
        testTradeOrder.setOrderType("BUY");
        testTradeOrder.setSecurityId("SECURITY1");
        testTradeOrder.setQuantity(new BigDecimal("1000.00"));
        testTradeOrder.setQuantitySent(new BigDecimal("0.00"));
        testTradeOrder.setLimitPrice(new BigDecimal("50.00"));
        testTradeOrder.setSubmitted(false);
        testTradeOrder.setBlotter(testBlotter);
        testTradeOrder.setTradeTimestamp(OffsetDateTime.now());
        
        testSubmitDTO = new TradeOrderSubmitDTO();
        testSubmitDTO.setQuantity(new BigDecimal("500.00"));
        testSubmitDTO.setDestinationId(1);
        
        testTradeType = new TradeType();
        testTradeType.setId(1);
        testTradeType.setAbbreviation("BUY");
        testTradeType.setDescription("Buy Order");
        
        testExecutionStatus = new ExecutionStatus();
        testExecutionStatus.setId(1);
        testExecutionStatus.setAbbreviation("PENDING");
        testExecutionStatus.setDescription("Pending Execution");
        
        testDestination = new Destination();
        testDestination.setId(1);
        testDestination.setAbbreviation("DEST1");
        testDestination.setDescription("Test Destination");
    }
    
    @Test
    void testCreateExecutionRecord_Success() {
        // Arrange
        when(tradeTypeRepository.findById(1)).thenReturn(Optional.of(testTradeType));
        when(executionStatusRepository.findById(1)).thenReturn(Optional.of(testExecutionStatus));
        when(destinationRepository.findById(1)).thenReturn(Optional.of(testDestination));
        
        Execution savedExecution = new Execution();
        savedExecution.setId(1);
        savedExecution.setQuantityOrdered(testSubmitDTO.getQuantity());
        when(executionRepository.save(any(Execution.class))).thenReturn(savedExecution);
        
        // Act
        Execution result = optimizedTradeOrderService.createExecutionRecord(testTradeOrder, testSubmitDTO);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(testSubmitDTO.getQuantity(), result.getQuantityOrdered());
        
        verify(tradeTypeRepository).findById(1);
        verify(executionStatusRepository).findById(1);
        verify(destinationRepository).findById(1);
        verify(executionRepository).save(any(Execution.class));
    }
    
    @Test
    void testCreateExecutionRecord_InsufficientQuantity() {
        // Arrange
        testTradeOrder.setQuantitySent(new BigDecimal("800.00")); // Only 200 available
        testSubmitDTO.setQuantity(new BigDecimal("500.00")); // Requesting more than available
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            optimizedTradeOrderService.createExecutionRecord(testTradeOrder, testSubmitDTO);
        });
        
        assertEquals("Requested quantity exceeds available quantity", exception.getMessage());
        
        // Verify no database operations were performed
        verify(tradeTypeRepository, never()).findById(any());
        verify(executionRepository, never()).save(any());
    }
    
    @Test
    void testCreateExecutionRecord_InvalidOrderType() {
        // Arrange
        testTradeOrder.setOrderType("INVALID");
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            optimizedTradeOrderService.createExecutionRecord(testTradeOrder, testSubmitDTO);
        });
        
        assertEquals("Unknown order_type: INVALID", exception.getMessage());
    }
    
    @Test
    void testUpdateTradeOrderQuantities_Success() {
        // Arrange
        when(tradeOrderRepository.findById(1)).thenReturn(Optional.of(testTradeOrder));
        when(tradeOrderRepository.save(any(TradeOrder.class))).thenReturn(testTradeOrder);
        
        // Act
        optimizedTradeOrderService.updateTradeOrderQuantities(1, new BigDecimal("500.00"));
        
        // Assert
        verify(tradeOrderRepository).findById(1);
        verify(tradeOrderRepository).save(argThat(tradeOrder -> {
            return tradeOrder.getQuantitySent().compareTo(new BigDecimal("500.00")) == 0 &&
                   !tradeOrder.getSubmitted(); // Should not be fully submitted yet
        }));
    }
    
    @Test
    void testUpdateTradeOrderQuantities_FullySubmitted() {
        // Arrange
        when(tradeOrderRepository.findById(1)).thenReturn(Optional.of(testTradeOrder));
        when(tradeOrderRepository.save(any(TradeOrder.class))).thenReturn(testTradeOrder);
        
        // Act - submit the full quantity
        optimizedTradeOrderService.updateTradeOrderQuantities(1, new BigDecimal("1000.00"));
        
        // Assert
        verify(tradeOrderRepository).save(argThat(tradeOrder -> {
            return tradeOrder.getQuantitySent().compareTo(new BigDecimal("1000.00")) == 0 &&
                   tradeOrder.getSubmitted(); // Should be fully submitted
        }));
    }
    
    @Test
    void testSubmitTradeOrder_WithoutExternalSubmission() throws Exception {
        // Arrange
        when(tradeOrderRepository.findByIdWithBlotter(1)).thenReturn(Optional.of(testTradeOrder));
        when(tradeTypeRepository.findById(1)).thenReturn(Optional.of(testTradeType));
        when(executionStatusRepository.findById(1)).thenReturn(Optional.of(testExecutionStatus));
        when(destinationRepository.findById(1)).thenReturn(Optional.of(testDestination));
        
        Execution savedExecution = new Execution();
        savedExecution.setId(1);
        savedExecution.setQuantityOrdered(testSubmitDTO.getQuantity());
        when(executionRepository.save(any(Execution.class))).thenReturn(savedExecution);
        when(tradeOrderRepository.findById(1)).thenReturn(Optional.of(testTradeOrder));
        when(tradeOrderRepository.save(any(TradeOrder.class))).thenReturn(testTradeOrder);
        
        // Act
        Execution result = optimizedTradeOrderService.submitTradeOrder(1, testSubmitDTO, true);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        
        // Verify external service was not called
        verify(executionService, never()).submitExecution(any());
        verify(retryTemplate, never()).execute(any());
    }
    
    @Test
    void testSubmitTradeOrder_WithExternalSubmission_Success() throws Exception {
        // Arrange
        when(tradeOrderRepository.findByIdWithBlotter(1)).thenReturn(Optional.of(testTradeOrder));
        when(tradeTypeRepository.findById(1)).thenReturn(Optional.of(testTradeType));
        when(executionStatusRepository.findById(1)).thenReturn(Optional.of(testExecutionStatus));
        when(destinationRepository.findById(1)).thenReturn(Optional.of(testDestination));
        
        Execution savedExecution = new Execution();
        savedExecution.setId(1);
        savedExecution.setQuantityOrdered(testSubmitDTO.getQuantity());
        when(executionRepository.save(any(Execution.class))).thenReturn(savedExecution);
        when(tradeOrderRepository.findById(1)).thenReturn(Optional.of(testTradeOrder));
        when(tradeOrderRepository.save(any(TradeOrder.class))).thenReturn(testTradeOrder);
        
        // Mock successful external service call
        ExecutionService.SubmitResult successResult = new ExecutionService.SubmitResult("submitted", null);
        when(retryTemplate.execute(any())).thenReturn(successResult);
        when(executionRepository.findById(1)).thenReturn(Optional.of(savedExecution));
        
        // Act
        Execution result = optimizedTradeOrderService.submitTradeOrder(1, testSubmitDTO, false);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        
        // Verify external service was called
        verify(retryTemplate).execute(any());
    }
    
    @Test
    void testSubmitTradeOrder_ExternalSubmissionFailure_PerformsCompensation() throws Exception {
        // Arrange
        when(tradeOrderRepository.findByIdWithBlotter(1)).thenReturn(Optional.of(testTradeOrder));
        when(tradeTypeRepository.findById(1)).thenReturn(Optional.of(testTradeType));
        when(executionStatusRepository.findById(1)).thenReturn(Optional.of(testExecutionStatus));
        when(destinationRepository.findById(1)).thenReturn(Optional.of(testDestination));
        
        Execution savedExecution = new Execution();
        savedExecution.setId(1);
        savedExecution.setQuantityOrdered(testSubmitDTO.getQuantity());
        when(executionRepository.save(any(Execution.class))).thenReturn(savedExecution);
        when(tradeOrderRepository.findById(1)).thenReturn(Optional.of(testTradeOrder));
        when(tradeOrderRepository.save(any(TradeOrder.class))).thenReturn(testTradeOrder);
        
        // Mock failed external service call
        when(retryTemplate.execute(any())).thenThrow(new RuntimeException("External service failed"));
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            optimizedTradeOrderService.submitTradeOrder(1, testSubmitDTO, false);
        });
        
        assertTrue(exception.getMessage().contains("Failed to submit execution to external service"));
        
        // Verify compensation handler was called
        verify(compensationHandler).compensateFailedSubmission(any(Execution.class), any(TransactionCompensationHandler.TradeOrderState.class));
    }
    
    // Note: deleteExecutionRecord and restoreTradeOrderState methods have been moved
    // to TransactionCompensationHandler as part of task 2.2 implementation
}