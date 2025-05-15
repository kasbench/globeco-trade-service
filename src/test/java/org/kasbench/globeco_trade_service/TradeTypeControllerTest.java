package org.kasbench.globeco_trade_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kasbench.globeco_trade_service.dto.TradeTypePostDTO;
import org.kasbench.globeco_trade_service.dto.TradeTypePutDTO;
import org.kasbench.globeco_trade_service.entity.TradeType;
import org.kasbench.globeco_trade_service.service.TradeTypeService;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TradeTypeController.class)
public class TradeTypeControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private TradeTypeService tradeTypeService;
    @Autowired
    private ObjectMapper objectMapper;

    private TradeType tradeType;

    @BeforeEach
    void setup() {
        tradeType = new TradeType();
        tradeType.setId(1);
        tradeType.setAbbreviation("BUY");
        tradeType.setDescription("Buy");
        tradeType.setVersion(1);
    }

    @Test
    void testGetAllTradeTypes() throws Exception {
        Mockito.when(tradeTypeService.getAllTradeTypes()).thenReturn(Arrays.asList(tradeType));
        mockMvc.perform(get("/api/v1/tradeTypes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].abbreviation").value("BUY"));
    }

    @Test
    void testGetTradeTypeByIdFound() throws Exception {
        Mockito.when(tradeTypeService.getTradeTypeById(1)).thenReturn(Optional.of(tradeType));
        mockMvc.perform(get("/api/v1/tradeType/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.abbreviation").value("BUY"));
    }

    @Test
    void testGetTradeTypeByIdNotFound() throws Exception {
        Mockito.when(tradeTypeService.getTradeTypeById(2)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/tradeType/2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateTradeType() throws Exception {
        TradeTypePostDTO dto = new TradeTypePostDTO();
        dto.setAbbreviation("SELL");
        dto.setDescription("Sell");
        TradeType created = new TradeType();
        created.setId(2);
        created.setAbbreviation("SELL");
        created.setDescription("Sell");
        created.setVersion(1);
        Mockito.when(tradeTypeService.createTradeType(any(TradeType.class))).thenReturn(created);
        mockMvc.perform(post("/api/v1/tradeTypes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.abbreviation").value("SELL"));
    }

    @Test
    void testUpdateTradeType() throws Exception {
        TradeTypePutDTO dto = new TradeTypePutDTO();
        dto.setId(1);
        dto.setAbbreviation("BUY");
        dto.setDescription("Buy Updated");
        dto.setVersion(1);
        TradeType updated = new TradeType();
        updated.setId(1);
        updated.setAbbreviation("BUY");
        updated.setDescription("Buy Updated");
        updated.setVersion(1);
        Mockito.when(tradeTypeService.updateTradeType(eq(1), any(TradeType.class))).thenReturn(updated);
        mockMvc.perform(put("/api/v1/tradeType/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Buy Updated"));
    }

    @Test
    void testUpdateTradeTypeNotFound() throws Exception {
        TradeTypePutDTO dto = new TradeTypePutDTO();
        dto.setId(99);
        dto.setAbbreviation("NONE");
        dto.setDescription("None");
        dto.setVersion(1);
        Mockito.when(tradeTypeService.updateTradeType(eq(99), any(TradeType.class))).thenThrow(new IllegalArgumentException("Not found"));
        mockMvc.perform(put("/api/v1/tradeType/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteTradeType() throws Exception {
        Mockito.doNothing().when(tradeTypeService).deleteTradeType(1, 1);
        mockMvc.perform(delete("/api/v1/tradeType/1?version=1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteTradeTypeNotFound() throws Exception {
        Mockito.doThrow(new IllegalArgumentException("Not found")).when(tradeTypeService).deleteTradeType(99, 1);
        mockMvc.perform(delete("/api/v1/tradeType/99?version=1"))
                .andExpect(status().isNotFound());
    }
} 