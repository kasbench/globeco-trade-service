package org.kasbench.globeco_trade_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kasbench.globeco_trade_service.dto.TradeTypePostDTO;
import org.kasbench.globeco_trade_service.dto.TradeTypePutDTO;
import org.kasbench.globeco_trade_service.entity.TradeType;
import org.kasbench.globeco_trade_service.service.TradeTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
public class TradeTypeControllerTest extends org.kasbench.globeco_trade_service.AbstractPostgresContainerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TradeTypeService tradeTypeService;
    @Autowired
    private ObjectMapper objectMapper;

    private TradeType tradeType;

    @BeforeEach
    void setup() {
        tradeType = new TradeType();
        tradeType.setAbbreviation("BUY");
        tradeType.setDescription("Buy");
        tradeType.setVersion(1);
        tradeType = tradeTypeService.createTradeType(tradeType);
    }

    @Test
    void testGetAllTradeTypes() throws Exception {
        mockMvc.perform(get("/api/v1/tradeTypes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.abbreviation=='BUY' && @.description=='Buy')]").exists());
    }

    @Test
    void testGetTradeTypeByIdFound() throws Exception {
        mockMvc.perform(get("/api/v1/tradeType/" + tradeType.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tradeType.getId()))
                .andExpect(jsonPath("$.abbreviation").value("BUY"));
    }

    @Test
    void testGetTradeTypeByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/tradeType/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateTradeType() throws Exception {
        TradeTypePostDTO dto = new TradeTypePostDTO();
        dto.setAbbreviation("SELL");
        dto.setDescription("Sell");
        mockMvc.perform(post("/api/v1/tradeTypes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.abbreviation").value("SELL"));
    }

    @Test
    void testUpdateTradeType() throws Exception {
        TradeTypePutDTO dto = new TradeTypePutDTO();
        dto.setId(tradeType.getId());
        dto.setAbbreviation("BUY");
        dto.setDescription("Buy Updated");
        dto.setVersion(1);
        mockMvc.perform(put("/api/v1/tradeType/" + tradeType.getId())
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
        mockMvc.perform(put("/api/v1/tradeType/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteTradeType() throws Exception {
        mockMvc.perform(delete("/api/v1/tradeType/" + tradeType.getId() + "?version=" + tradeType.getVersion()))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteTradeTypeNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/tradeType/99?version=1"))
                .andExpect(status().isNotFound());
    }
} 