package org.kasbench.globeco_trade_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.kasbench.globeco_trade_service.dto.TradeOrderPostDTO;
import org.kasbench.globeco_trade_service.dto.TradeOrderPutDTO;
import org.kasbench.globeco_trade_service.dto.TradeOrderSubmitDTO;
import org.kasbench.globeco_trade_service.entity.Blotter;
import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.kasbench.globeco_trade_service.entity.ExecutionStatus;
import org.kasbench.globeco_trade_service.entity.TradeType;
import org.kasbench.globeco_trade_service.entity.Destination;
import org.kasbench.globeco_trade_service.service.TradeOrderService;
import org.kasbench.globeco_trade_service.repository.BlotterRepository;
import org.kasbench.globeco_trade_service.repository.ExecutionStatusRepository;
import org.kasbench.globeco_trade_service.repository.TradeTypeRepository;
import org.kasbench.globeco_trade_service.repository.DestinationRepository;
import org.kasbench.globeco_trade_service.repository.TradeOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Random;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
public class TradeOrderControllerTest extends org.kasbench.globeco_trade_service.AbstractPostgresContainerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TradeOrderService tradeOrderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BlotterRepository blotterRepository;

    @Autowired
    private ExecutionStatusRepository executionStatusRepository;

    @Autowired
    private TradeTypeRepository tradeTypeRepository;

    @Autowired
    private DestinationRepository destinationRepository;

    @Autowired
    private TradeOrderRepository tradeOrderRepository;

    private TradeOrder tradeOrder;
    private Blotter blotter;

    private static String randomAlphaNum(int len) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @BeforeEach
    void setUp() {
        // Ensure required ExecutionStatus and TradeType exist
        if (executionStatusRepository.findById(1).isEmpty()) {
            ExecutionStatus status = new ExecutionStatus();
            status.setId(1);
            status.setAbbreviation("NEW");
            status.setDescription("New");
            status.setVersion(1);
            executionStatusRepository.saveAndFlush(status);
        }
        if (tradeTypeRepository.findById(1).isEmpty()) {
            TradeType tradeType = new TradeType();
            tradeType.setId(1);
            tradeType.setAbbreviation("BUY");
            tradeType.setDescription("Buy");
            tradeType.setVersion(1);
            tradeTypeRepository.saveAndFlush(tradeType);
        }
        if (destinationRepository.findById(1).isEmpty()) {
            Destination dest = new Destination();
            dest.setId(1);
            dest.setAbbreviation("DEST1");
            dest.setDescription("Test Destination");
            dest.setVersion(1);
            destinationRepository.saveAndFlush(dest);
        }
        blotter = new Blotter();
        blotter.setAbbreviation("EQ" + ThreadLocalRandom.current().nextInt(1_000_000));
        blotter.setName("Equity" + ThreadLocalRandom.current().nextInt(1_000_000));
        blotter.setVersion(1);
        blotter = blotterRepository.saveAndFlush(blotter);

        tradeOrder = new TradeOrder();
        tradeOrder.setOrderId(ThreadLocalRandom.current().nextInt(1_000_000, 2_000_000));
        tradeOrder.setPortfolioId(randomAlphaNum(12));
        tradeOrder.setOrderType("BUY");
        tradeOrder.setSecurityId(randomAlphaNum(12));
        tradeOrder.setQuantity(new java.math.BigDecimal("100.00"));
        tradeOrder.setLimitPrice(new java.math.BigDecimal("10.50"));
        tradeOrder.setTradeTimestamp(java.time.OffsetDateTime.now());
        tradeOrder.setVersion(1);
        tradeOrder.setBlotter(blotter);
        tradeOrder = tradeOrderService.createTradeOrder(tradeOrder);
    }

    @AfterEach
    void tearDown() {
        if (tradeOrder != null && tradeOrder.getId() != null) {
            try {
                // Reload to get latest version
                TradeOrder latest = tradeOrderService.getTradeOrderById(tradeOrder.getId()).orElse(null);
                if (latest != null) {
                    tradeOrderService.deleteTradeOrder(latest.getId(), latest.getVersion());
                }
            } catch (Exception e) {
                // Ignore if already deleted
            }
        }
        if (blotter != null && blotter.getId() != null) {
            try {
                blotterRepository.deleteById(blotter.getId());
            } catch (Exception e) {
                // Ignore if already deleted
            }
        }
    }

    @Test
    void testGetAllTradeOrders() throws Exception {
        mockMvc.perform(get("/api/v1/tradeOrders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.orderId==%d)]", tradeOrder.getOrderId()).exists())
                .andExpect(jsonPath("$[?(@.blotter.abbreviation=='%s')]", blotter.getAbbreviation()).exists())
                .andExpect(jsonPath("$[?(@.orderId==%d)].submitted", tradeOrder.getOrderId()).value(false));
    }

    @Test
    void testGetTradeOrderById_Found() throws Exception {
        mockMvc.perform(get("/api/v1/tradeOrders/" + tradeOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(tradeOrder.getOrderId()))
                .andExpect(jsonPath("$.submitted").value(false));
    }

    @Test
    void testGetTradeOrderById_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/tradeOrders/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateTradeOrder() throws Exception {
        TradeOrderPostDTO postDTO = new TradeOrderPostDTO();
        int uniqueOrderId = ThreadLocalRandom.current().nextInt(2_000_000, 3_000_000);
        postDTO.setOrderId(uniqueOrderId);
        postDTO.setPortfolioId(randomAlphaNum(12));
        postDTO.setOrderType("SELL");
        postDTO.setSecurityId(randomAlphaNum(12));
        postDTO.setQuantity(new BigDecimal("50.00"));
        postDTO.setLimitPrice(new BigDecimal("20.00"));
        postDTO.setTradeTimestamp(OffsetDateTime.now());
        postDTO.setBlotterId(blotter.getId());

        mockMvc.perform(post("/api/v1/tradeOrders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(uniqueOrderId))
                .andExpect(jsonPath("$.submitted").value(false));
    }

    @Test
    void testUpdateTradeOrder_Success() throws Exception {
        TradeOrderPutDTO putDTO = new TradeOrderPutDTO();
        putDTO.setId(tradeOrder.getId());
        putDTO.setOrderId(tradeOrder.getOrderId());
        putDTO.setPortfolioId(tradeOrder.getPortfolioId());
        putDTO.setOrderType("SELL");
        putDTO.setSecurityId(tradeOrder.getSecurityId());
        putDTO.setQuantity(tradeOrder.getQuantity());
        putDTO.setLimitPrice(tradeOrder.getLimitPrice());
        putDTO.setTradeTimestamp(tradeOrder.getTradeTimestamp());
        putDTO.setVersion(tradeOrder.getVersion());
        putDTO.setBlotterId(blotter.getId());

        mockMvc.perform(put("/api/v1/tradeOrders/" + tradeOrder.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(putDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(tradeOrder.getOrderId()))
                .andExpect(jsonPath("$.submitted").value(false));
    }

    @Test
    void testUpdateTradeOrder_NotFound() throws Exception {
        TradeOrderPutDTO putDTO = new TradeOrderPutDTO();
        putDTO.setId(999999);
        putDTO.setOrderId(ThreadLocalRandom.current().nextInt(3_000_000, 4_000_000));
        putDTO.setPortfolioId(randomAlphaNum(12));
        putDTO.setOrderType("SELL");
        putDTO.setSecurityId(randomAlphaNum(12));
        putDTO.setQuantity(new BigDecimal("1.00"));
        putDTO.setLimitPrice(new BigDecimal("1.00"));
        putDTO.setTradeTimestamp(OffsetDateTime.now());
        putDTO.setVersion(1);
        putDTO.setBlotterId(blotter.getId());

        mockMvc.perform(put("/api/v1/tradeOrders/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(putDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteTradeOrder_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/tradeOrders/" + tradeOrder.getId())
                        .param("version", String.valueOf(tradeOrder.getVersion())))
                .andExpect(status().isNoContent());
        tradeOrder = null; // Prevent double delete in tearDown
    }

    @Test
    void testDeleteTradeOrder_NotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/tradeOrders/999999")
                        .param("version", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSubmitTradeOrder_Success() throws Exception {
        TradeOrderSubmitDTO submitDTO = new TradeOrderSubmitDTO();
        submitDTO.setQuantity(new BigDecimal("10.00"));
        submitDTO.setDestinationId(1);
        mockMvc.perform(post("/api/v1/tradeOrders/" + tradeOrder.getId() + "/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tradeOrder.id").value(tradeOrder.getId()))
                .andExpect(jsonPath("$.quantityOrdered").value("10.00"))
                .andExpect(jsonPath("$.destination.id").value(1));
        // Assert tradeOrder.submitted is false after partial submit
        TradeOrder updated = tradeOrderService.getTradeOrderById(tradeOrder.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertFalse(updated.getSubmitted());
        // Submit the remaining quantity
        TradeOrderSubmitDTO submitDTO2 = new TradeOrderSubmitDTO();
        submitDTO2.setQuantity(new BigDecimal("90.00"));
        submitDTO2.setDestinationId(1);
        mockMvc.perform(post("/api/v1/tradeOrders/" + tradeOrder.getId() + "/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitDTO2)))
                .andExpect(status().isCreated());
        TradeOrder fullySubmitted = tradeOrderRepository.findById(tradeOrder.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(fullySubmitted.getSubmitted());
    }

    @Test
    void testSubmitTradeOrder_NotFound() throws Exception {
        TradeOrderSubmitDTO submitDTO = new TradeOrderSubmitDTO();
        submitDTO.setQuantity(new BigDecimal("10"));
        submitDTO.setDestinationId(1);
        mockMvc.perform(post("/api/v1/tradeOrders/999999/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSubmitTradeOrder_BadRequest() throws Exception {
        // Set up a trade order with an invalid order_type
        tradeOrder.setOrderType("INVALID");
        tradeOrderService.updateTradeOrder(tradeOrder.getId(), tradeOrder);
        TradeOrderSubmitDTO submitDTO = new TradeOrderSubmitDTO();
        submitDTO.setQuantity(new BigDecimal("10"));
        submitDTO.setDestinationId(1);
        mockMvc.perform(post("/api/v1/tradeOrders/" + tradeOrder.getId() + "/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSubmitTradeOrder_MissingDestination() throws Exception {
        TradeOrderSubmitDTO submitDTO = new TradeOrderSubmitDTO();
        submitDTO.setQuantity(new BigDecimal("10"));
        submitDTO.setDestinationId(999999);
        mockMvc.perform(post("/api/v1/tradeOrders/" + tradeOrder.getId() + "/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitDTO)))
                .andExpect(status().isBadRequest());
    }
} 