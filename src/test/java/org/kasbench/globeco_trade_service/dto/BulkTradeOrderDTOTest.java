package org.kasbench.globeco_trade_service.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BulkTradeOrderDTOTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testBulkTradeOrderRequestDTO_ValidRequest() {
        // Create a valid trade order
        TradeOrderPostDTO tradeOrder = new TradeOrderPostDTO();
        tradeOrder.setOrderId(12345);
        tradeOrder.setPortfolioId("PORTFOLIO_001");
        tradeOrder.setOrderType("BUY");
        tradeOrder.setSecurityId("AAPL");
        tradeOrder.setQuantity(new BigDecimal("100.00"));
        tradeOrder.setLimitPrice(new BigDecimal("150.25"));
        tradeOrder.setTradeTimestamp(OffsetDateTime.now());
        tradeOrder.setBlotterId(1);

        List<TradeOrderPostDTO> tradeOrders = new ArrayList<>();
        tradeOrders.add(tradeOrder);

        BulkTradeOrderRequestDTO request = new BulkTradeOrderRequestDTO(tradeOrders);

        Set<ConstraintViolation<BulkTradeOrderRequestDTO>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    void testBulkTradeOrderRequestDTO_NullTradeOrders() {
        BulkTradeOrderRequestDTO request = new BulkTradeOrderRequestDTO();
        request.setTradeOrders(null);

        Set<ConstraintViolation<BulkTradeOrderRequestDTO>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null trade orders should cause validation error");
        
        boolean foundNullMessage = violations.stream()
                .anyMatch(v -> v.getMessage().contains("cannot be null"));
        assertTrue(foundNullMessage, "Should have null validation message");
    }

    @Test
    void testBulkTradeOrderRequestDTO_EmptyTradeOrders() {
        BulkTradeOrderRequestDTO request = new BulkTradeOrderRequestDTO();
        request.setTradeOrders(new ArrayList<>());

        Set<ConstraintViolation<BulkTradeOrderRequestDTO>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Empty trade orders should cause validation error");
        
        boolean foundSizeMessage = violations.stream()
                .anyMatch(v -> v.getMessage().contains("between 1 and 1000"));
        assertTrue(foundSizeMessage, "Should have size validation message");
    }

    @Test
    void testTradeOrderResultDTO_Success() {
        TradeOrderResponseDTO tradeOrderResponse = new TradeOrderResponseDTO();
        tradeOrderResponse.setId(1);
        tradeOrderResponse.setOrderId(12345);

        TradeOrderResultDTO result = new TradeOrderResultDTO(
                0, 
                TradeOrderResultDTO.ResultStatus.SUCCESS, 
                "Order created successfully", 
                tradeOrderResponse
        );

        assertEquals(0, result.getRequestIndex());
        assertEquals(TradeOrderResultDTO.ResultStatus.SUCCESS, result.getStatus());
        assertEquals("Order created successfully", result.getMessage());
        assertNotNull(result.getTradeOrder());
        assertEquals(1, result.getTradeOrder().getId());
    }

    @Test
    void testBulkTradeOrderResponseDTO_Success() {
        List<TradeOrderResultDTO> results = new ArrayList<>();
        results.add(new TradeOrderResultDTO(0, TradeOrderResultDTO.ResultStatus.SUCCESS, "Success", null));

        BulkTradeOrderResponseDTO response = new BulkTradeOrderResponseDTO(
                BulkTradeOrderResponseDTO.BulkStatus.SUCCESS,
                "All orders created successfully",
                1,
                1,
                0,
                results
        );

        assertEquals(BulkTradeOrderResponseDTO.BulkStatus.SUCCESS, response.getStatus());
        assertEquals("All orders created successfully", response.getMessage());
        assertEquals(1, response.getTotalRequested());
        assertEquals(1, response.getSuccessful());
        assertEquals(0, response.getFailed());
        assertEquals(1, response.getResults().size());
    }
}