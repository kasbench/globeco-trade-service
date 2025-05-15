package org.kasbench.globeco_trade_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kasbench.globeco_trade_service.dto.TradeOrderPostDTO;
import org.kasbench.globeco_trade_service.dto.TradeOrderPutDTO;
import org.kasbench.globeco_trade_service.entity.Blotter;
import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.kasbench.globeco_trade_service.service.TradeOrderService;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TradeOrderController.class)
public class TradeOrderControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradeOrderService tradeOrderService;

    @Autowired
    private ObjectMapper objectMapper;

    private TradeOrder tradeOrder;
    private Blotter blotter;

    @BeforeEach
    void setUp() {
        blotter = new Blotter();
        blotter.setId(1);
        blotter.setAbbreviation("EQ");
        blotter.setName("Equity");
        blotter.setVersion(1);

        tradeOrder = new TradeOrder();
        tradeOrder.setId(1);
        tradeOrder.setOrderId(1001);
        tradeOrder.setPortfolioId("PORTFOLIO1");
        tradeOrder.setOrderType("BUY");
        tradeOrder.setSecurityId("SEC123");
        tradeOrder.setQuantity(new BigDecimal("100.00"));
        tradeOrder.setLimitPrice(new BigDecimal("10.50"));
        tradeOrder.setTradeTimestamp(OffsetDateTime.now());
        tradeOrder.setVersion(1);
        tradeOrder.setBlotter(blotter);
    }

    @Test
    void testGetAllTradeOrders() throws Exception {
        Mockito.when(tradeOrderService.getAllTradeOrders()).thenReturn(Arrays.asList(tradeOrder));
        mockMvc.perform(get("/api/v1/tradeOrders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(tradeOrder.getId()))
                .andExpect(jsonPath("$[0].orderId").value(tradeOrder.getOrderId()))
                .andExpect(jsonPath("$[0].blotter.id").value(blotter.getId()));
    }

    @Test
    void testGetTradeOrderById_Found() throws Exception {
        Mockito.when(tradeOrderService.getTradeOrderById(1)).thenReturn(Optional.of(tradeOrder));
        mockMvc.perform(get("/api/v1/tradeOrders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tradeOrder.getId()))
                .andExpect(jsonPath("$.orderId").value(tradeOrder.getOrderId()));
    }

    @Test
    void testGetTradeOrderById_NotFound() throws Exception {
        Mockito.when(tradeOrderService.getTradeOrderById(2)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/tradeOrders/2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateTradeOrder() throws Exception {
        TradeOrderPostDTO postDTO = new TradeOrderPostDTO();
        postDTO.setOrderId(2002);
        postDTO.setPortfolioId("PORTFOLIO2");
        postDTO.setOrderType("SELL");
        postDTO.setSecurityId("SEC456");
        postDTO.setQuantity(new BigDecimal("50.00"));
        postDTO.setLimitPrice(new BigDecimal("20.00"));
        postDTO.setTradeTimestamp(OffsetDateTime.now());
        postDTO.setBlotterId(1);

        TradeOrder created = new TradeOrder();
        created.setId(2);
        created.setOrderId(postDTO.getOrderId());
        created.setPortfolioId(postDTO.getPortfolioId());
        created.setOrderType(postDTO.getOrderType());
        created.setSecurityId(postDTO.getSecurityId());
        created.setQuantity(postDTO.getQuantity());
        created.setLimitPrice(postDTO.getLimitPrice());
        created.setTradeTimestamp(postDTO.getTradeTimestamp());
        created.setVersion(1);
        created.setBlotter(blotter);

        Mockito.when(tradeOrderService.createTradeOrder(any(TradeOrder.class))).thenReturn(created);
        mockMvc.perform(post("/api/v1/tradeOrders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.orderId").value(created.getOrderId()));
    }

    @Test
    void testUpdateTradeOrder_Success() throws Exception {
        TradeOrderPutDTO putDTO = new TradeOrderPutDTO();
        putDTO.setId(1);
        putDTO.setOrderId(1001);
        putDTO.setPortfolioId("PORTFOLIO1");
        putDTO.setOrderType("BUY");
        putDTO.setSecurityId("SEC123");
        putDTO.setQuantity(new BigDecimal("100.00"));
        putDTO.setLimitPrice(new BigDecimal("10.50"));
        putDTO.setTradeTimestamp(tradeOrder.getTradeTimestamp());
        putDTO.setVersion(1);
        putDTO.setBlotterId(1);

        Mockito.when(tradeOrderService.updateTradeOrder(eq(1), any(TradeOrder.class))).thenReturn(tradeOrder);
        mockMvc.perform(put("/api/v1/tradeOrders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(putDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tradeOrder.getId()))
                .andExpect(jsonPath("$.orderId").value(tradeOrder.getOrderId()));
    }

    @Test
    void testUpdateTradeOrder_NotFound() throws Exception {
        TradeOrderPutDTO putDTO = new TradeOrderPutDTO();
        putDTO.setId(99);
        putDTO.setOrderId(9999);
        putDTO.setPortfolioId("PORTFOLIOX");
        putDTO.setOrderType("SELL");
        putDTO.setSecurityId("SEC999");
        putDTO.setQuantity(new BigDecimal("1.00"));
        putDTO.setLimitPrice(new BigDecimal("1.00"));
        putDTO.setTradeTimestamp(OffsetDateTime.now());
        putDTO.setVersion(1);
        putDTO.setBlotterId(1);

        Mockito.when(tradeOrderService.updateTradeOrder(eq(99), any(TradeOrder.class))).thenThrow(new IllegalArgumentException("Not found"));
        mockMvc.perform(put("/api/v1/tradeOrders/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(putDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteTradeOrder_Success() throws Exception {
        Mockito.doNothing().when(tradeOrderService).deleteTradeOrder(1, 1);
        mockMvc.perform(delete("/api/v1/tradeOrders/1")
                        .param("version", "1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteTradeOrder_NotFound() throws Exception {
        Mockito.doThrow(new IllegalArgumentException("Not found")).when(tradeOrderService).deleteTradeOrder(99, 1);
        mockMvc.perform(delete("/api/v1/tradeOrders/99")
                        .param("version", "1"))
                .andExpect(status().isNotFound());
    }
} 