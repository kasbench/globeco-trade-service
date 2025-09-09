package org.kasbench.globeco_trade_service.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for batch execution DTOs to verify serialization and validation.
 */
public class BatchExecutionDTOTest {
    
    private ObjectMapper objectMapper;
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Test
    void testBatchExecutionRequestDTO_ValidRequest() {
        // Given
        ExecutionPostDTO execution = createValidExecutionPostDTO();
        List<ExecutionPostDTO> executions = List.of(execution);
        BatchExecutionRequestDTO request = new BatchExecutionRequestDTO(executions);
        
        // When
        Set<ConstraintViolation<BatchExecutionRequestDTO>> violations = validator.validate(request);
        
        // Then
        assertTrue(violations.isEmpty(), "Valid request should have no validation errors");
        assertEquals(1, request.getExecutions().size());
    }
    
    @Test
    void testBatchExecutionRequestDTO_NullExecutions() {
        // Given
        BatchExecutionRequestDTO request = new BatchExecutionRequestDTO();
        request.setExecutions(null);
        
        // When
        Set<ConstraintViolation<BatchExecutionRequestDTO>> violations = validator.validate(request);
        
        // Then
        assertFalse(violations.isEmpty(), "Null executions should cause validation error");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("cannot be null")));
    }
    
    @Test
    void testBatchExecutionRequestDTO_EmptyExecutions() {
        // Given
        BatchExecutionRequestDTO request = new BatchExecutionRequestDTO();
        request.setExecutions(new ArrayList<>());
        
        // When
        Set<ConstraintViolation<BatchExecutionRequestDTO>> violations = validator.validate(request);
        
        // Then
        assertFalse(violations.isEmpty(), "Empty executions should cause validation error");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("between 1 and 100")));
    }
    
    @Test
    void testBatchExecutionRequestDTO_TooManyExecutions() {
        // Given
        List<ExecutionPostDTO> executions = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            executions.add(createValidExecutionPostDTO());
        }
        BatchExecutionRequestDTO request = new BatchExecutionRequestDTO(executions);
        
        // When
        Set<ConstraintViolation<BatchExecutionRequestDTO>> violations = validator.validate(request);
        
        // Then
        assertFalse(violations.isEmpty(), "Too many executions should cause validation error");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("between 1 and 100")));
    }
    
    @Test
    void testBatchExecutionResponseDTO_Serialization() throws Exception {
        // Given
        ExecutionResultDTO result = new ExecutionResultDTO(0, "SUCCESS", "Execution created", null);
        List<ExecutionResultDTO> results = List.of(result);
        BatchExecutionResponseDTO response = new BatchExecutionResponseDTO(
            "SUCCESS", "All executions processed", 1, 1, 0, results);
        
        // When
        String json = objectMapper.writeValueAsString(response);
        BatchExecutionResponseDTO deserialized = objectMapper.readValue(json, BatchExecutionResponseDTO.class);
        
        // Then
        assertNotNull(json);
        assertNotNull(deserialized);
        assertEquals("SUCCESS", deserialized.getStatus());
        assertEquals("All executions processed", deserialized.getMessage());
        assertEquals(1, deserialized.getTotalRequested());
        assertEquals(1, deserialized.getSuccessful());
        assertEquals(0, deserialized.getFailed());
        assertEquals(1, deserialized.getResults().size());
    }
    
    @Test
    void testExecutionResultDTO_Serialization() throws Exception {
        // Given
        ExecutionResultDTO result = new ExecutionResultDTO(0, "SUCCESS", "Created successfully", null);
        
        // When
        String json = objectMapper.writeValueAsString(result);
        ExecutionResultDTO deserialized = objectMapper.readValue(json, ExecutionResultDTO.class);
        
        // Then
        assertNotNull(json);
        assertNotNull(deserialized);
        assertEquals(0, deserialized.getRequestIndex());
        assertEquals("SUCCESS", deserialized.getStatus());
        assertEquals("Created successfully", deserialized.getMessage());
    }
    
    @Test
    void testBatchExecutionRequestDTO_ToString() {
        // Given
        ExecutionPostDTO execution = createValidExecutionPostDTO();
        List<ExecutionPostDTO> executions = List.of(execution);
        BatchExecutionRequestDTO request = new BatchExecutionRequestDTO(executions);
        
        // When
        String toString = request.toString();
        
        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("BatchExecutionRequestDTO"));
        assertTrue(toString.contains("1 items"));
    }
    
    private ExecutionPostDTO createValidExecutionPostDTO() {
        ExecutionPostDTO execution = new ExecutionPostDTO();
        execution.setExecutionTimestamp(OffsetDateTime.now());
        execution.setExecutionStatusId(1);
        execution.setBlotterId(1);
        execution.setTradeTypeId(1);
        execution.setTradeOrderId(1);
        execution.setDestinationId(1);
        execution.setQuantityOrdered(BigDecimal.valueOf(100));
        execution.setQuantityPlaced(BigDecimal.valueOf(100));
        execution.setQuantityFilled(BigDecimal.ZERO);
        execution.setLimitPrice(BigDecimal.valueOf(50.00));
        return execution;
    }
}