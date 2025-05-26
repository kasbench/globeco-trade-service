package org.kasbench.globeco_trade_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kasbench.globeco_trade_service.dto.ExecutionPostDTO;
import org.kasbench.globeco_trade_service.dto.ExecutionPutDTO;
import org.kasbench.globeco_trade_service.entity.*;
import org.kasbench.globeco_trade_service.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
public class ExecutionControllerTest extends org.kasbench.globeco_trade_service.AbstractPostgresContainerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
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
    @MockBean
    private RestTemplate restTemplate;

    private ExecutionStatus status;
    private Blotter blotter;
    private TradeType tradeType;
    private TradeOrder tradeOrder;
    private Destination destination;

    @BeforeEach
    void setup() {
        status = new ExecutionStatus();
        status.setAbbreviation("NEW");
        status.setDescription("New");
        status = executionStatusRepository.saveAndFlush(status);

        blotter = new Blotter();
        blotter.setAbbreviation("EQ");
        blotter.setName("Equity");
        blotter = blotterRepository.saveAndFlush(blotter);

        tradeType = new TradeType();
        tradeType.setAbbreviation("BUY");
        tradeType.setDescription("Buy");
        tradeType = tradeTypeRepository.saveAndFlush(tradeType);

        tradeOrder = new TradeOrder();
        tradeOrder.setOrderId(ThreadLocalRandom.current().nextInt(1_000_000, Integer.MAX_VALUE));
        tradeOrder.setPortfolioId("PORT1");
        tradeOrder.setOrderType("BUY");
        tradeOrder.setSecurityId("SEC1");
        tradeOrder.setQuantity(new java.math.BigDecimal("100.00"));
        tradeOrder.setLimitPrice(new java.math.BigDecimal("10.00"));
        tradeOrder.setTradeTimestamp(java.time.OffsetDateTime.now());
        tradeOrder.setBlotter(blotter);
        tradeOrder = tradeOrderRepository.saveAndFlush(tradeOrder);

        destination = new Destination();
        destination.setAbbreviation("ML");
        destination.setDescription("Merrill Lynch");
        destination = destinationRepository.saveAndFlush(destination);
    }

    private ExecutionPostDTO buildPostDTO() {
        ExecutionPostDTO dto = new ExecutionPostDTO();
        dto.setExecutionTimestamp(OffsetDateTime.now());
        dto.setExecutionStatusId(status.getId());
        dto.setBlotterId(blotter.getId());
        dto.setTradeTypeId(tradeType.getId());
        dto.setTradeOrderId(tradeOrder.getId());
        dto.setDestinationId(destination.getId());
        dto.setQuantityOrdered(new BigDecimal("10.00"));
        dto.setQuantityPlaced(new BigDecimal("100.00"));
        dto.setQuantityFilled(new BigDecimal("0.00"));
        dto.setLimitPrice(new BigDecimal("10.00"));
        dto.setExecutionServiceId(55555);
        return dto;
    }

    @Test
    void testCreateAndGetExecution() throws Exception {
        ExecutionPostDTO postDTO = buildPostDTO();
        String postJson = objectMapper.writeValueAsString(postDTO);
        // Create
        String response = mockMvc.perform(post("/api/v1/executions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(postJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.executionServiceId").value(55555))
                .andReturn().getResponse().getContentAsString();
        int id = objectMapper.readTree(response).get("id").asInt();
        // Get by id
        mockMvc.perform(get("/api/v1/executions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.executionStatus.abbreviation").value("NEW"))
                .andExpect(jsonPath("$.executionServiceId").value(55555));
    }

    @Test
    void testGetAllExecutions() throws Exception {
        ExecutionPostDTO postDTO = buildPostDTO();
        String postJson = objectMapper.writeValueAsString(postDTO);
        mockMvc.perform(post("/api/v1/executions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(postJson))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/executions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].executionStatus.abbreviation", hasItem("NEW")));
    }

    @Test
    void testUpdateExecution() throws Exception {
        ExecutionPostDTO postDTO = buildPostDTO();
        String postJson = objectMapper.writeValueAsString(postDTO);
        String response = mockMvc.perform(post("/api/v1/executions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(postJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = objectMapper.readTree(response).get("id").asInt();
        int version = objectMapper.readTree(response).get("version").asInt();
        ExecutionPutDTO putDTO = new ExecutionPutDTO();
        putDTO.setId(id);
        putDTO.setExecutionTimestamp(postDTO.getExecutionTimestamp());
        putDTO.setExecutionStatusId(status.getId());
        putDTO.setBlotterId(blotter.getId());
        putDTO.setTradeTypeId(tradeType.getId());
        putDTO.setTradeOrderId(tradeOrder.getId());
        putDTO.setDestinationId(destination.getId());
        putDTO.setQuantityOrdered(new BigDecimal("20.00"));
        putDTO.setQuantityPlaced(new BigDecimal("200.00"));
        putDTO.setQuantityFilled(new BigDecimal("50.00"));
        putDTO.setLimitPrice(new BigDecimal("20.00"));
        putDTO.setVersion(version);
        putDTO.setExecutionServiceId(77777);
        String putJson = objectMapper.writeValueAsString(putDTO);
        mockMvc.perform(put("/api/v1/executions/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(putJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOrdered").value("20.00"))
                .andExpect(jsonPath("$.quantityPlaced").value("200.00"))
                .andExpect(jsonPath("$.quantityFilled").value("50.00"))
                .andExpect(jsonPath("$.limitPrice").value("20.00"))
                .andExpect(jsonPath("$.executionServiceId").value(77777));
    }

    @Test
    void testDeleteExecution() throws Exception {
        ExecutionPostDTO postDTO = buildPostDTO();
        String postJson = objectMapper.writeValueAsString(postDTO);
        String response = mockMvc.perform(post("/api/v1/executions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(postJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = objectMapper.readTree(response).get("id").asInt();
        int version = objectMapper.readTree(response).get("version").asInt();
        mockMvc.perform(delete("/api/v1/executions/" + id)
                .param("version", String.valueOf(version)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/executions/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteExecutionVersionMismatch() throws Exception {
        ExecutionPostDTO postDTO = buildPostDTO();
        String postJson = objectMapper.writeValueAsString(postDTO);
        String response = mockMvc.perform(post("/api/v1/executions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(postJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = objectMapper.readTree(response).get("id").asInt();
        int version = objectMapper.readTree(response).get("version").asInt();
        mockMvc.perform(delete("/api/v1/executions/" + id)
                .param("version", String.valueOf(version + 1)))
                .andExpect(status().isConflict());
    }

    @Test
    void testGetExecutionNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/executions/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSubmitExecutionSuccess() throws Exception {
        ExecutionPostDTO postDTO = buildPostDTO();
        String postJson = objectMapper.writeValueAsString(postDTO);
        String response = mockMvc.perform(post("/api/v1/executions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(postJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = objectMapper.readTree(response).get("id").asInt();
        java.util.Map<String, Object> responseMap = new java.util.HashMap<>();
        responseMap.put("id", 99999);
        when(restTemplate.postForEntity(anyString(), any(), eq(java.util.Map.class)))
            .thenReturn(ResponseEntity.ok(responseMap));
        mockMvc.perform(post("/api/v1/execution/" + id + "/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.executionServiceId").value(99999))
                .andExpect(jsonPath("$.executionStatus.abbreviation").value("SENT"))
                .andExpect(jsonPath("$.quantityPlaced").value("10.00"));
    }

    @Test
    void testSubmitExecutionClientError() throws Exception {
        ExecutionPostDTO postDTO = buildPostDTO();
        String postJson = objectMapper.writeValueAsString(postDTO);
        String response = mockMvc.perform(post("/api/v1/executions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(postJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = objectMapper.readTree(response).get("id").asInt();
        HttpStatusCodeException ex = mock(HttpStatusCodeException.class);
        when(ex.getRawStatusCode()).thenReturn(400);
        when(ex.getResponseBodyAsString()).thenReturn("Bad Request");
        when(restTemplate.postForEntity(anyString(), any(), eq(java.util.Map.class)))
            .thenThrow(ex);
        mockMvc.perform(post("/api/v1/execution/" + id + "/submit"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Client error")));
    }

    @Test
    void testSubmitExecutionServerError() throws Exception {
        ExecutionPostDTO postDTO = buildPostDTO();
        String postJson = objectMapper.writeValueAsString(postDTO);
        String response = mockMvc.perform(post("/api/v1/executions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(postJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = objectMapper.readTree(response).get("id").asInt();
        HttpStatusCodeException ex = mock(HttpStatusCodeException.class);
        when(ex.getRawStatusCode()).thenReturn(500);
        when(restTemplate.postForEntity(anyString(), any(), eq(java.util.Map.class)))
            .thenThrow(ex);
        mockMvc.perform(post("/api/v1/execution/" + id + "/submit"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", containsString("unavailable")));
    }

    @Test
    void testSubmitExecutionNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/execution/-1/submit"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("not found")));
    }
} 