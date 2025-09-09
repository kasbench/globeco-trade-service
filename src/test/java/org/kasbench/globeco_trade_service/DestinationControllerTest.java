package org.kasbench.globeco_trade_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.kasbench.globeco_trade_service.dto.DestinationPostDTO;
import org.kasbench.globeco_trade_service.dto.DestinationPutDTO;
import org.kasbench.globeco_trade_service.entity.Destination;
import org.kasbench.globeco_trade_service.service.DestinationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
@Timeout(value = 10, unit = TimeUnit.SECONDS)
public class DestinationControllerTest extends AbstractH2Test {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private DestinationService destinationService;
    @Autowired
    private ObjectMapper objectMapper;

    private Destination destination;

    @BeforeEach
    void setup() {
        destination = new Destination();
        destination.setAbbreviation("ML");
        destination.setDescription("Merrill Lynch");
        destination.setVersion(1);
        destination = destinationService.createDestination(destination);
    }

    @AfterEach
    void tearDown() {
        // Clean up test data
        try {
            if (destination != null && destination.getId() != null) {
                destinationService.deleteDestination(destination.getId(), destination.getVersion());
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    void testGetAllDestinations() throws Exception {
        mockMvc.perform(get("/api/v1/destinations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.abbreviation=='ML' && @.description=='Merrill Lynch')]").exists());
    }

    @Test
    void testGetDestinationByIdFound() throws Exception {
        mockMvc.perform(get("/api/v1/destinations/" + destination.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(destination.getId()))
                .andExpect(jsonPath("$.abbreviation").value("ML"));
    }

    @Test
    void testGetDestinationByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/destinations/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateDestination() throws Exception {
        DestinationPostDTO postDTO = new DestinationPostDTO();
        postDTO.setAbbreviation("ML");
        postDTO.setDescription("Merrill Lynch");
        mockMvc.perform(post("/api/v1/destinations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.abbreviation").value("ML"));
    }

    @Test
    void testUpdateDestinationFound() throws Exception {
        DestinationPutDTO putDTO = new DestinationPutDTO();
        putDTO.setId(destination.getId());
        putDTO.setAbbreviation("ML");
        putDTO.setDescription("Updated");
        putDTO.setVersion(1);
        Destination updated = new Destination();
        updated.setId(destination.getId());
        updated.setAbbreviation("ML");
        updated.setDescription("Updated");
        updated.setVersion(1);
        mockMvc.perform(put("/api/v1/destinations/" + destination.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(putDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated"));
    }

    @Test
    void testUpdateDestinationNotFound() throws Exception {
        DestinationPutDTO putDTO = new DestinationPutDTO();
        putDTO.setId(999999);
        putDTO.setAbbreviation("XX");
        putDTO.setDescription("Not Found");
        putDTO.setVersion(1);
        mockMvc.perform(put("/api/v1/destinations/999999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(putDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteDestinationFound() throws Exception {
        mockMvc.perform(delete("/api/v1/destinations/" + destination.getId() + "?version=" + destination.getVersion()))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteDestinationNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/destinations/999999?version=1"))
                .andExpect(status().isNotFound());
    }
} 