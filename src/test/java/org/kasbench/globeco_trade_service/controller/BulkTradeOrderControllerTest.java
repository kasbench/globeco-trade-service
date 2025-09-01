package org.kasbench.globeco_trade_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kasbench.globeco_trade_service.TradeOrderController;
import org.kasbench.globeco_trade_service.dto.*;
import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.kasbench.globeco_trade_service.service.ExecutionService;
import org.kasbench.globeco_trade_service.service.TradeOrderService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.TransactionSystemException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for bulk trade order controller endpoint.
 * Tests the POST /api/v1/tradeOrders/bulk endpoint in isolation by mocking the service layer.
 */
@ExtendWith(MockitoExtension.class)
class BulkTradeOrderControllerTest {

    @Mock
    private TradeOrderService tradeOrderService;

    @Mock
    private ExecutionService executionService;

    @InjectMocks
    private TradeOrderController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Register JSR310 module for OffsetDateTime
    }

    @Test
    void testCreateTradeOrdersBulk_Success() throws Exception {
        // Arrange
        BulkTradeOrderRequestDTO request = createValidBulkRequest();
        List<TradeOrder> createdOrders = createMockCreatedOrders();
        
        when(tradeOrderService.createTradeOrdersBulk(anyList())).thenReturn(createdOrders);

        // Act & Assert
        mockMvc.perform(post("/api/v1/tradeOrders/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("All trade orders created successfully"))
                .andExpect(jsonPath("$.totalRequested").value(2))
                .andExpect(jsonPath("$.successful").value(2))
                .andExpect(jsonPath("$.failed").value(0))
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results.length()").value(2))
                .andExpect(jsonPath("$.results[0].requestIndex").value(0))
                .andExpect(jsonPath("$.results[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.results[0].message").value("Trade order created successfully"))
                .andExpect(jsonPath("$.results[0].tradeOrder.id").value(1))
                .andExpect(jsonPath("$.results[0].tradeOrder.orderId").value(12345))
                .andExpect(jsonPath("$.results[1].requestIndex").value(1))
                .andExpect(jsonPath("$.results[1].status").value("SUCCESS"))
                .andExpect(jsonPath("$.results[1].message").value("Trade order created successfully"))
                .andExpect(jsonPath("$.results[1].tradeOrder.id").value(2))
                .andExpect(jsonPath("$.results[1].tradeOrder.orderId").value(67890));

        verify(tradeOrderService, times(1)).createTradeOrdersBulk(anyList());
    }

    @Test
    void testCreateTradeOrdersBulk_ValidationError() throws Exception {
        // Arrange
        BulkTradeOrderRequestDTO request = createValidBulkRequest();
        
        when(tradeOrderService.createTradeOrdersBulk(anyList()))
                .thenThrow(new IllegalArgumentException("Invalid portfolio ID: INVALID_PORTFOLIO"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/tradeOrders/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("Bulk operation failed due to validation errors: Invalid portfolio ID: INVALID_PORTFOLIO"))
                .andExpect(jsonPath("$.totalRequested").value(2))
                .andExpect(jsonPath("$.successful").value(0))
                .andExpect(jsonPath("$.failed").value(2))
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results.length()").value(2))
                .andExpect(jsonPath("$.results[0].requestIndex").value(0))
                .andExpect(jsonPath("$.results[0].status").value("FAILURE"))
                .andExpect(jsonPath("$.results[0].message").value("Invalid portfolio ID: INVALID_PORTFOLIO"))
                .andExpect(jsonPath("$.results[0].tradeOrder").isEmpty());

        verify(tradeOrderService, times(1)).createTradeOrdersBulk(anyList());
    }

    @Test
    void testCreateTradeOrdersBulk_DatabaseConstraintViolation() throws Exception {
        // Arrange
        BulkTradeOrderRequestDTO request = createValidBulkRequest();
        
        when(tradeOrderService.createTradeOrdersBulk(anyList()))
                .thenThrow(new DataIntegrityViolationException("Duplicate order ID"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/tradeOrders/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("Bulk operation failed due to database constraint violations"))
                .andExpect(jsonPath("$.totalRequested").value(2))
                .andExpect(jsonPath("$.successful").value(0))
                .andExpect(jsonPath("$.failed").value(2))
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results.length()").value(2))
                .andExpect(jsonPath("$.results[0].status").value("FAILURE"))
                .andExpect(jsonPath("$.results[0].message").value("Database constraint violation"));

        verify(tradeOrderService, times(1)).createTradeOrdersBulk(anyList());
    }

    @Test
    void testCreateTradeOrdersBulk_TransactionError() throws Exception {
        // Arrange
        BulkTradeOrderRequestDTO request = createValidBulkRequest();
        
        when(tradeOrderService.createTradeOrdersBulk(anyList()))
                .thenThrow(new TransactionSystemException("Transaction rollback"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/tradeOrders/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("Bulk operation failed due to transaction error"))
                .andExpect(jsonPath("$.totalRequested").value(2))
                .andExpect(jsonPath("$.successful").value(0))
                .andExpect(jsonPath("$.failed").value(2))
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results.length()").value(2))
                .andExpect(jsonPath("$.results[0].status").value("FAILURE"))
                .andExpect(jsonPath("$.results[0].message").value("Transaction failed"));

        verify(tradeOrderService, times(1)).createTradeOrdersBulk(anyList());
    }

    @Test
    void testCreateTradeOrdersBulk_UnexpectedError() throws Exception {
        // Arrange
        BulkTradeOrderRequestDTO request = createValidBulkRequest();
        
        when(tradeOrderService.createTradeOrdersBulk(anyList()))
                .thenThrow(new RuntimeException("Unexpected database error"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/tradeOrders/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("Bulk operation failed due to unexpected error"))
                .andExpect(jsonPath("$.totalRequested").value(2))
                .andExpect(jsonPath("$.successful").value(0))
                .andExpect(jsonPath("$.failed").value(2))
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results.length()").value(2))
                .andExpect(jsonPath("$.results[0].status").value("FAILURE"))
                .andExpect(jsonPath("$.results[0].message").value("Internal server error"));

        verify(tradeOrderService, times(1)).createTradeOrdersBulk(anyList());
    }

    @Test
    void testCreateTradeOrdersBulk_InvalidRequestFormat_NullTradeOrders() throws Exception {
        // Arrange
        String invalidJson = """
            {
                "tradeOrders": null
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/v1/tradeOrders/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(tradeOrderService, never()).createTradeOrdersBulk(anyList());
    }

    @Test
    void testCreateTradeOrdersBulk_InvalidRequestFormat_EmptyTradeOrders() throws Exception {
        // Arrange
        BulkTradeOrderRequestDTO request = new BulkTradeOrderRequestDTO();
        request.setTradeOrders(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(post("/api/v1/tradeOrders/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(tradeOrderService, never()).createTradeOrdersBulk(anyList());
    }

    @Test
    void testCreateTradeOrdersBulk_InvalidRequestFormat_ExceedsMaxSize() throws Exception {
        // Arrange - Create request with 1001 trade orders (exceeds max of 1000)
        BulkTradeOrderRequestDTO request = new BulkTradeOrderRequestDTO();
        List<TradeOrderPostDTO> tradeOrders = Collections.nCopies(1001, createValidTradeOrderPostDTO(12345));
        request.setTradeOrders(tradeOrders);

        // Act & Assert
        mockMvc.perform(post("/api/v1/tradeOrders/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(tradeOrderService, never()).createTradeOrdersBulk(anyList());
    }

    @Test
    void testCreateTradeOrdersBulk_InvalidRequestFormat_InvalidTradeOrderData() throws Exception {
        // Arrange
        String invalidJson = """
            {
                "tradeOrders": [
                    {
                        "orderId": null,
                        "portfolioId": "",
                        "orderType": "INVALID_TYPE",
                        "securityId": null,
                        "quantity": -100.00,
                        "limitPrice": null,
                        "tradeTimestamp": "invalid-date",
                        "blotterId": null
                    }
                ]
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/v1/tradeOrders/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(tradeOrderService, never()).createTradeOrdersBulk(anyList());
    }

    @Test
    void testCreateTradeOrdersBulk_RequestResponseDTOMapping() throws Exception {
        // Arrange
        BulkTradeOrderRequestDTO request = createValidBulkRequest();
        List<TradeOrder> createdOrders = createMockCreatedOrders();
        
        when(tradeOrderService.createTradeOrdersBulk(anyList())).thenReturn(createdOrders);

        // Act & Assert - Verify detailed DTO mapping
        mockMvc.perform(post("/api/v1/tradeOrders/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.results[0].tradeOrder.orderId").value(12345))
                .andExpect(jsonPath("$.results[0].tradeOrder.portfolioId").value("PORTFOLIO_001"))
                .andExpect(jsonPath("$.results[0].tradeOrder.orderType").value("BUY"))
                .andExpect(jsonPath("$.results[0].tradeOrder.securityId").value("AAPL"))
                .andExpect(jsonPath("$.results[0].tradeOrder.quantity").value(100.00))
                .andExpect(jsonPath("$.results[0].tradeOrder.limitPrice").value(150.25))
                .andExpect(jsonPath("$.results[0].tradeOrder.submitted").value(false))
                .andExpect(jsonPath("$.results[1].tradeOrder.orderId").value(67890))
                .andExpect(jsonPath("$.results[1].tradeOrder.portfolioId").value("PORTFOLIO_002"))
                .andExpect(jsonPath("$.results[1].tradeOrder.orderType").value("SELL"))
                .andExpect(jsonPath("$.results[1].tradeOrder.securityId").value("GOOGL"))
                .andExpect(jsonPath("$.results[1].tradeOrder.quantity").value(50.00))
                .andExpect(jsonPath("$.results[1].tradeOrder.limitPrice").value(2500.75))
                .andExpect(jsonPath("$.results[1].tradeOrder.submitted").value(false));

        verify(tradeOrderService, times(1)).createTradeOrdersBulk(anyList());
    }

    @Test
    void testCreateTradeOrdersBulk_SingleTradeOrder() throws Exception {
        // Arrange
        BulkTradeOrderRequestDTO request = new BulkTradeOrderRequestDTO();
        request.setTradeOrders(Arrays.asList(createValidTradeOrderPostDTO(12345)));
        
        TradeOrder createdOrder = createMockTradeOrder(1, 12345, "PORTFOLIO_001", "BUY", "AAPL", 
                                                      new BigDecimal("100.00"), new BigDecimal("150.25"));
        when(tradeOrderService.createTradeOrdersBulk(anyList())).thenReturn(Arrays.asList(createdOrder));

        // Act & Assert
        mockMvc.perform(post("/api/v1/tradeOrders/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.totalRequested").value(1))
                .andExpect(jsonPath("$.successful").value(1))
                .andExpect(jsonPath("$.failed").value(0))
                .andExpect(jsonPath("$.results.length()").value(1))
                .andExpect(jsonPath("$.results[0].requestIndex").value(0))
                .andExpect(jsonPath("$.results[0].status").value("SUCCESS"));

        verify(tradeOrderService, times(1)).createTradeOrdersBulk(anyList());
    }

    @Test
    void testCreateTradeOrdersBulk_MaximumAllowedSize() throws Exception {
        // Arrange - Create request with exactly 1000 trade orders (maximum allowed)
        BulkTradeOrderRequestDTO request = new BulkTradeOrderRequestDTO();
        List<TradeOrderPostDTO> tradeOrders = Collections.nCopies(1000, createValidTradeOrderPostDTO(12345));
        request.setTradeOrders(tradeOrders);
        
        List<TradeOrder> createdOrders = Collections.nCopies(1000, 
                createMockTradeOrder(1, 12345, "PORTFOLIO_001", "BUY", "AAPL", 
                                   new BigDecimal("100.00"), new BigDecimal("150.25")));
        when(tradeOrderService.createTradeOrdersBulk(anyList())).thenReturn(createdOrders);

        // Act & Assert
        mockMvc.perform(post("/api/v1/tradeOrders/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.totalRequested").value(1000))
                .andExpect(jsonPath("$.successful").value(1000))
                .andExpect(jsonPath("$.failed").value(0))
                .andExpect(jsonPath("$.results.length()").value(1000));

        verify(tradeOrderService, times(1)).createTradeOrdersBulk(anyList());
    }

    // Helper methods

    private BulkTradeOrderRequestDTO createValidBulkRequest() {
        BulkTradeOrderRequestDTO request = new BulkTradeOrderRequestDTO();
        request.setTradeOrders(Arrays.asList(
                createValidTradeOrderPostDTO(12345),
                createValidTradeOrderPostDTO(67890)
        ));
        return request;
    }

    private TradeOrderPostDTO createValidTradeOrderPostDTO(Integer orderId) {
        TradeOrderPostDTO dto = new TradeOrderPostDTO();
        dto.setOrderId(orderId);
        dto.setPortfolioId(orderId == 12345 ? "PORTFOLIO_001" : "PORTFOLIO_002");
        dto.setOrderType(orderId == 12345 ? "BUY" : "SELL");
        dto.setSecurityId(orderId == 12345 ? "AAPL" : "GOOGL");
        dto.setQuantity(orderId == 12345 ? new BigDecimal("100.00") : new BigDecimal("50.00"));
        dto.setLimitPrice(orderId == 12345 ? new BigDecimal("150.25") : new BigDecimal("2500.75"));
        dto.setTradeTimestamp(OffsetDateTime.now());
        dto.setBlotterId(1);
        return dto;
    }

    private List<TradeOrder> createMockCreatedOrders() {
        return Arrays.asList(
                createMockTradeOrder(1, 12345, "PORTFOLIO_001", "BUY", "AAPL", 
                                   new BigDecimal("100.00"), new BigDecimal("150.25")),
                createMockTradeOrder(2, 67890, "PORTFOLIO_002", "SELL", "GOOGL", 
                                   new BigDecimal("50.00"), new BigDecimal("2500.75"))
        );
    }

    private TradeOrder createMockTradeOrder(Integer id, Integer orderId, String portfolioId, 
                                          String orderType, String securityId, 
                                          BigDecimal quantity, BigDecimal limitPrice) {
        TradeOrder tradeOrder = new TradeOrder();
        tradeOrder.setId(id);
        tradeOrder.setOrderId(orderId);
        tradeOrder.setPortfolioId(portfolioId);
        tradeOrder.setOrderType(orderType);
        tradeOrder.setSecurityId(securityId);
        tradeOrder.setQuantity(quantity);
        tradeOrder.setLimitPrice(limitPrice);
        tradeOrder.setTradeTimestamp(OffsetDateTime.now());
        tradeOrder.setQuantitySent(BigDecimal.ZERO);
        tradeOrder.setSubmitted(false);
        tradeOrder.setVersion(1);
        return tradeOrder;
    }
}