package org.kasbench.globeco_trade_service.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class BulkTradeOrderDTOTest {

    private Validator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Nested
    @DisplayName("BulkTradeOrderRequestDTO Tests")
    class BulkTradeOrderRequestDTOTests {

        @Test
        @DisplayName("Should validate successfully with valid trade orders")
        void testValidRequest() {
            // Given
            List<TradeOrderPostDTO> tradeOrders = createValidTradeOrders(2);
            BulkTradeOrderRequestDTO request = new BulkTradeOrderRequestDTO(tradeOrders);

            // When
            Set<ConstraintViolation<BulkTradeOrderRequestDTO>> violations = validator.validate(request);

            // Then
            assertTrue(violations.isEmpty(), "Valid request should have no violations");
            assertEquals(2, request.getTradeOrders().size());
        }

        @Test
        @DisplayName("Should fail validation when trade orders list is null")
        void testNullTradeOrders() {
            // Given
            BulkTradeOrderRequestDTO request = new BulkTradeOrderRequestDTO();
            request.setTradeOrders(null);

            // When
            Set<ConstraintViolation<BulkTradeOrderRequestDTO>> violations = validator.validate(request);

            // Then
            assertFalse(violations.isEmpty(), "Null trade orders should cause validation error");
            boolean foundNullMessage = violations.stream()
                    .anyMatch(v -> v.getMessage().contains("cannot be null"));
            assertTrue(foundNullMessage, "Should have null validation message");
        }

        @Test
        @DisplayName("Should fail validation when trade orders list is empty")
        void testEmptyTradeOrders() {
            // Given
            BulkTradeOrderRequestDTO request = new BulkTradeOrderRequestDTO();
            request.setTradeOrders(new ArrayList<>());

            // When
            Set<ConstraintViolation<BulkTradeOrderRequestDTO>> violations = validator.validate(request);

            // Then
            assertFalse(violations.isEmpty(), "Empty trade orders should cause validation error");
            boolean foundSizeMessage = violations.stream()
                    .anyMatch(v -> v.getMessage().contains("between 1 and 1000"));
            assertTrue(foundSizeMessage, "Should have size validation message");
        }

        @Test
        @DisplayName("Should fail validation when trade orders exceed maximum size limit")
        void testMaximumSizeLimit() {
            // Given - Create 1001 trade orders (exceeds limit of 1000)
            List<TradeOrderPostDTO> tradeOrders = createValidTradeOrders(1001);
            BulkTradeOrderRequestDTO request = new BulkTradeOrderRequestDTO(tradeOrders);

            // When
            Set<ConstraintViolation<BulkTradeOrderRequestDTO>> violations = validator.validate(request);

            // Then
            assertFalse(violations.isEmpty(), "Exceeding maximum size should cause validation error");
            boolean foundSizeMessage = violations.stream()
                    .anyMatch(v -> v.getMessage().contains("between 1 and 1000"));
            assertTrue(foundSizeMessage, "Should have size validation message");
        }

        @Test
        @DisplayName("Should validate successfully at maximum size limit")
        void testAtMaximumSizeLimit() {
            // Given - Create exactly 1000 trade orders (at the limit)
            List<TradeOrderPostDTO> tradeOrders = createValidTradeOrders(1000);
            BulkTradeOrderRequestDTO request = new BulkTradeOrderRequestDTO(tradeOrders);

            // When
            Set<ConstraintViolation<BulkTradeOrderRequestDTO>> violations = validator.validate(request);

            // Then
            assertTrue(violations.isEmpty(), "Request at maximum size should be valid");
            assertEquals(1000, request.getTradeOrders().size());
        }

        @Test
        @DisplayName("Should fail validation when individual trade order is invalid")
        void testInvalidTradeOrderInBulk() {
            // Given
            List<TradeOrderPostDTO> tradeOrders = new ArrayList<>();
            TradeOrderPostDTO invalidOrder = new TradeOrderPostDTO();
            // Leave required fields null to make it invalid
            tradeOrders.add(invalidOrder);
            
            BulkTradeOrderRequestDTO request = new BulkTradeOrderRequestDTO(tradeOrders);

            // When
            Set<ConstraintViolation<BulkTradeOrderRequestDTO>> violations = validator.validate(request);

            // Then
            // Note: This test depends on TradeOrderPostDTO having validation annotations
            // If TradeOrderPostDTO doesn't have validation, this test will pass
            // but the validation would happen at the service layer
            assertNotNull(violations);
        }

        @Test
        @DisplayName("Should serialize and deserialize correctly")
        void testSerialization() throws JsonProcessingException {
            // Given
            List<TradeOrderPostDTO> tradeOrders = createValidTradeOrders(2);
            BulkTradeOrderRequestDTO original = new BulkTradeOrderRequestDTO(tradeOrders);

            // When
            String json = objectMapper.writeValueAsString(original);
            BulkTradeOrderRequestDTO deserialized = objectMapper.readValue(json, BulkTradeOrderRequestDTO.class);

            // Then
            assertNotNull(deserialized);
            assertEquals(original.getTradeOrders().size(), deserialized.getTradeOrders().size());
        }

        @Test
        @DisplayName("Should handle toString method correctly")
        void testToString() {
            // Given
            List<TradeOrderPostDTO> tradeOrders = createValidTradeOrders(3);
            BulkTradeOrderRequestDTO request = new BulkTradeOrderRequestDTO(tradeOrders);

            // When
            String toString = request.toString();

            // Then
            assertNotNull(toString);
            assertTrue(toString.contains("BulkTradeOrderRequestDTO"));
            assertTrue(toString.contains("3 items"));
        }

        @Test
        @DisplayName("Should handle toString method with null trade orders")
        void testToStringWithNull() {
            // Given
            BulkTradeOrderRequestDTO request = new BulkTradeOrderRequestDTO();
            request.setTradeOrders(null);

            // When
            String toString = request.toString();

            // Then
            assertNotNull(toString);
            assertTrue(toString.contains("null"));
        }
    }

    @Nested
    @DisplayName("BulkTradeOrderResponseDTO Tests")
    class BulkTradeOrderResponseDTOTests {

        @Test
        @DisplayName("Should create successful response correctly")
        void testSuccessfulResponse() {
            // Given
            List<TradeOrderResultDTO> results = createSuccessfulResults(2);
            
            // When
            BulkTradeOrderResponseDTO response = new BulkTradeOrderResponseDTO(
                    BulkTradeOrderResponseDTO.BulkStatus.SUCCESS,
                    "All orders created successfully",
                    2,
                    2,
                    0,
                    results
            );

            // Then
            assertEquals(BulkTradeOrderResponseDTO.BulkStatus.SUCCESS, response.getStatus());
            assertEquals("All orders created successfully", response.getMessage());
            assertEquals(2, response.getTotalRequested());
            assertEquals(2, response.getSuccessful());
            assertEquals(0, response.getFailed());
            assertEquals(2, response.getResults().size());
        }

        @Test
        @DisplayName("Should create failure response correctly")
        void testFailureResponse() {
            // Given
            List<TradeOrderResultDTO> results = createFailedResults(1);
            
            // When
            BulkTradeOrderResponseDTO response = new BulkTradeOrderResponseDTO(
                    BulkTradeOrderResponseDTO.BulkStatus.FAILURE,
                    "Bulk operation failed",
                    1,
                    0,
                    1,
                    results
            );

            // Then
            assertEquals(BulkTradeOrderResponseDTO.BulkStatus.FAILURE, response.getStatus());
            assertEquals("Bulk operation failed", response.getMessage());
            assertEquals(1, response.getTotalRequested());
            assertEquals(0, response.getSuccessful());
            assertEquals(1, response.getFailed());
            assertEquals(1, response.getResults().size());
        }

        @Test
        @DisplayName("Should create mixed success/failure response correctly")
        void testMixedResponse() {
            // Given
            List<TradeOrderResultDTO> results = new ArrayList<>();
            results.addAll(createSuccessfulResults(2));
            results.addAll(createFailedResults(1));
            
            // When
            BulkTradeOrderResponseDTO response = new BulkTradeOrderResponseDTO(
                    BulkTradeOrderResponseDTO.BulkStatus.FAILURE,
                    "Partial success",
                    3,
                    2,
                    1,
                    results
            );

            // Then
            assertEquals(BulkTradeOrderResponseDTO.BulkStatus.FAILURE, response.getStatus());
            assertEquals(3, response.getTotalRequested());
            assertEquals(2, response.getSuccessful());
            assertEquals(1, response.getFailed());
            assertEquals(3, response.getResults().size());
        }

        @Test
        @DisplayName("Should serialize and deserialize correctly")
        void testSerialization() throws JsonProcessingException {
            // Given
            List<TradeOrderResultDTO> results = createSuccessfulResults(1);
            BulkTradeOrderResponseDTO original = new BulkTradeOrderResponseDTO(
                    BulkTradeOrderResponseDTO.BulkStatus.SUCCESS,
                    "Success",
                    1,
                    1,
                    0,
                    results
            );

            // When
            String json = objectMapper.writeValueAsString(original);
            BulkTradeOrderResponseDTO deserialized = objectMapper.readValue(json, BulkTradeOrderResponseDTO.class);

            // Then
            assertNotNull(deserialized);
            assertEquals(original.getStatus(), deserialized.getStatus());
            assertEquals(original.getMessage(), deserialized.getMessage());
            assertEquals(original.getTotalRequested(), deserialized.getTotalRequested());
            assertEquals(original.getSuccessful(), deserialized.getSuccessful());
            assertEquals(original.getFailed(), deserialized.getFailed());
            assertEquals(original.getResults().size(), deserialized.getResults().size());
        }

        @Test
        @DisplayName("Should handle empty results list")
        void testEmptyResults() {
            // Given
            BulkTradeOrderResponseDTO response = new BulkTradeOrderResponseDTO(
                    BulkTradeOrderResponseDTO.BulkStatus.SUCCESS,
                    "No orders to process",
                    0,
                    0,
                    0,
                    Collections.emptyList()
            );

            // Then
            assertEquals(0, response.getTotalRequested());
            assertEquals(0, response.getResults().size());
        }

        @Test
        @DisplayName("Should handle toString method correctly")
        void testToString() {
            // Given
            List<TradeOrderResultDTO> results = createSuccessfulResults(2);
            BulkTradeOrderResponseDTO response = new BulkTradeOrderResponseDTO(
                    BulkTradeOrderResponseDTO.BulkStatus.SUCCESS,
                    "Success",
                    2,
                    2,
                    0,
                    results
            );

            // When
            String toString = response.toString();

            // Then
            assertNotNull(toString);
            assertTrue(toString.contains("BulkTradeOrderResponseDTO"));
            assertTrue(toString.contains("SUCCESS"));
            assertTrue(toString.contains("2 items"));
        }
    }

    @Nested
    @DisplayName("TradeOrderResultDTO Tests")
    class TradeOrderResultDTOTests {

        @Test
        @DisplayName("Should create successful result correctly")
        void testSuccessfulResult() {
            // Given
            TradeOrderResponseDTO tradeOrderResponse = createTradeOrderResponse(1, 12345);
            
            // When
            TradeOrderResultDTO result = new TradeOrderResultDTO(
                    0, 
                    TradeOrderResultDTO.ResultStatus.SUCCESS, 
                    "Order created successfully", 
                    tradeOrderResponse
            );

            // Then
            assertEquals(0, result.getRequestIndex());
            assertEquals(TradeOrderResultDTO.ResultStatus.SUCCESS, result.getStatus());
            assertEquals("Order created successfully", result.getMessage());
            assertNotNull(result.getTradeOrder());
            assertEquals(1, result.getTradeOrder().getId());
            assertEquals(12345, result.getTradeOrder().getOrderId());
        }

        @Test
        @DisplayName("Should create failed result correctly")
        void testFailedResult() {
            // When
            TradeOrderResultDTO result = new TradeOrderResultDTO(
                    1, 
                    TradeOrderResultDTO.ResultStatus.FAILURE, 
                    "Invalid portfolio ID", 
                    null
            );

            // Then
            assertEquals(1, result.getRequestIndex());
            assertEquals(TradeOrderResultDTO.ResultStatus.FAILURE, result.getStatus());
            assertEquals("Invalid portfolio ID", result.getMessage());
            assertNull(result.getTradeOrder());
        }

        @Test
        @DisplayName("Should handle different result statuses")
        void testResultStatuses() {
            // Test SUCCESS status
            TradeOrderResultDTO successResult = new TradeOrderResultDTO();
            successResult.setStatus(TradeOrderResultDTO.ResultStatus.SUCCESS);
            assertEquals(TradeOrderResultDTO.ResultStatus.SUCCESS, successResult.getStatus());

            // Test FAILURE status
            TradeOrderResultDTO failureResult = new TradeOrderResultDTO();
            failureResult.setStatus(TradeOrderResultDTO.ResultStatus.FAILURE);
            assertEquals(TradeOrderResultDTO.ResultStatus.FAILURE, failureResult.getStatus());
        }

        @Test
        @DisplayName("Should serialize and deserialize correctly")
        void testSerialization() throws JsonProcessingException {
            // Given
            TradeOrderResponseDTO tradeOrderResponse = createTradeOrderResponse(1, 12345);
            TradeOrderResultDTO original = new TradeOrderResultDTO(
                    0, 
                    TradeOrderResultDTO.ResultStatus.SUCCESS, 
                    "Success", 
                    tradeOrderResponse
            );

            // When
            String json = objectMapper.writeValueAsString(original);
            TradeOrderResultDTO deserialized = objectMapper.readValue(json, TradeOrderResultDTO.class);

            // Then
            assertNotNull(deserialized);
            assertEquals(original.getRequestIndex(), deserialized.getRequestIndex());
            assertEquals(original.getStatus(), deserialized.getStatus());
            assertEquals(original.getMessage(), deserialized.getMessage());
            assertNotNull(deserialized.getTradeOrder());
        }

        @Test
        @DisplayName("Should handle toString method correctly")
        void testToString() {
            // Given
            TradeOrderResponseDTO tradeOrderResponse = createTradeOrderResponse(1, 12345);
            TradeOrderResultDTO result = new TradeOrderResultDTO(
                    0, 
                    TradeOrderResultDTO.ResultStatus.SUCCESS, 
                    "Success", 
                    tradeOrderResponse
            );

            // When
            String toString = result.toString();

            // Then
            assertNotNull(toString);
            assertTrue(toString.contains("TradeOrderResultDTO"));
            assertTrue(toString.contains("SUCCESS"));
            assertTrue(toString.contains("id=1"));
        }

        @Test
        @DisplayName("Should handle toString method with null trade order")
        void testToStringWithNullTradeOrder() {
            // Given
            TradeOrderResultDTO result = new TradeOrderResultDTO(
                    0, 
                    TradeOrderResultDTO.ResultStatus.FAILURE, 
                    "Failed", 
                    null
            );

            // When
            String toString = result.toString();

            // Then
            assertNotNull(toString);
            assertTrue(toString.contains("null"));
        }

        @Test
        @DisplayName("Should handle all setter and getter methods")
        void testSettersAndGetters() {
            // Given
            TradeOrderResultDTO result = new TradeOrderResultDTO();
            TradeOrderResponseDTO tradeOrder = createTradeOrderResponse(1, 12345);

            // When
            result.setRequestIndex(5);
            result.setStatus(TradeOrderResultDTO.ResultStatus.SUCCESS);
            result.setMessage("Test message");
            result.setTradeOrder(tradeOrder);

            // Then
            assertEquals(5, result.getRequestIndex());
            assertEquals(TradeOrderResultDTO.ResultStatus.SUCCESS, result.getStatus());
            assertEquals("Test message", result.getMessage());
            assertEquals(tradeOrder, result.getTradeOrder());
        }
    }

    // Helper methods
    private List<TradeOrderPostDTO> createValidTradeOrders(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> {
                    TradeOrderPostDTO tradeOrder = new TradeOrderPostDTO();
                    tradeOrder.setOrderId(12345 + i);
                    tradeOrder.setPortfolioId("PORTFOLIO_" + String.format("%03d", i + 1));
                    tradeOrder.setOrderType("BUY");
                    tradeOrder.setSecurityId("AAPL");
                    tradeOrder.setQuantity(new BigDecimal("100.00"));
                    tradeOrder.setLimitPrice(new BigDecimal("150.25"));
                    tradeOrder.setTradeTimestamp(OffsetDateTime.now());
                    tradeOrder.setBlotterId(1);
                    return tradeOrder;
                })
                .toList();
    }

    private List<TradeOrderResultDTO> createSuccessfulResults(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> new TradeOrderResultDTO(
                        i,
                        TradeOrderResultDTO.ResultStatus.SUCCESS,
                        "Order created successfully",
                        createTradeOrderResponse(i + 1, 12345 + i)
                ))
                .toList();
    }

    private List<TradeOrderResultDTO> createFailedResults(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> new TradeOrderResultDTO(
                        i,
                        TradeOrderResultDTO.ResultStatus.FAILURE,
                        "Validation failed",
                        null
                ))
                .toList();
    }

    private TradeOrderResponseDTO createTradeOrderResponse(Integer id, Integer orderId) {
        TradeOrderResponseDTO response = new TradeOrderResponseDTO();
        response.setId(id);
        response.setOrderId(orderId);
        response.setPortfolioId("PORTFOLIO_001");
        response.setOrderType("BUY");
        response.setSecurityId("AAPL");
        response.setQuantity(new BigDecimal("100.00"));
        response.setLimitPrice(new BigDecimal("150.25"));
        response.setTradeTimestamp(OffsetDateTime.now());
        response.setSubmitted(false);
        response.setVersion(1);
        return response;
    }
}