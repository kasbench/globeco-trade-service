package org.kasbench.globeco_trade_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kasbench.globeco_trade_service.dto.BlotterPostDTO;
import org.kasbench.globeco_trade_service.dto.BlotterPutDTO;
import org.kasbench.globeco_trade_service.entity.Blotter;
import org.kasbench.globeco_trade_service.repository.BlotterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
public class BlotterControllerTest extends org.kasbench.globeco_trade_service.AbstractPostgresContainerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private BlotterRepository blotterRepository;

    private Blotter blotter;

    @BeforeEach
    void setUp() {
        blotterRepository.deleteAll();
        blotter = new Blotter();
        blotter.setAbbreviation("EQ");
        blotter.setName("Equity");
        blotter = blotterRepository.saveAndFlush(blotter);
    }

    @Test
    void testGetAllBlotters() throws Exception {
        mockMvc.perform(get("/api/v1/blotters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(blotter.getId()))
                .andExpect(jsonPath("$[0].abbreviation").value("EQ"));
    }

    @Test
    void testGetBlotterById() throws Exception {
        mockMvc.perform(get("/api/v1/blotters/" + blotter.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(blotter.getId()))
                .andExpect(jsonPath("$.abbreviation").value("EQ"));
    }

    @Test
    void testCreateBlotter() throws Exception {
        BlotterPostDTO dto = new BlotterPostDTO();
        dto.setAbbreviation("FI");
        dto.setName("Fixed Income");
        mockMvc.perform(post("/api/v1/blotters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.abbreviation").value("FI"))
                .andExpect(jsonPath("$.name").value("Fixed Income"));
    }

    @Test
    void testUpdateBlotter() throws Exception {
        BlotterPutDTO dto = new BlotterPutDTO();
        dto.setId(blotter.getId());
        dto.setAbbreviation("EQ");
        dto.setName("Equity Updated");
        dto.setVersion(blotter.getVersion());
        mockMvc.perform(put("/api/v1/blotters/" + blotter.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Equity Updated"));
    }

    @Test
    void testDeleteBlotter() throws Exception {
        mockMvc.perform(delete("/api/v1/blotters/" + blotter.getId())
                .param("version", String.valueOf(blotter.getVersion())))
                .andExpect(status().isNoContent());
    }

    @Test
    void testGetBlotterByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/blotters/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateBlotterNotFound() throws Exception {
        BlotterPutDTO dto = new BlotterPutDTO();
        dto.setId(99999);
        dto.setAbbreviation("X");
        dto.setName("Not Found");
        dto.setVersion(1);
        mockMvc.perform(put("/api/v1/blotters/99999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteBlotterNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/blotters/99999")
                .param("version", "1"))
                .andExpect(status().isNotFound());
    }
} 