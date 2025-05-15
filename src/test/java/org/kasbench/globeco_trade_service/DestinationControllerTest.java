package org.kasbench.globeco_trade_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kasbench.globeco_trade_service.dto.DestinationPostDTO;
import org.kasbench.globeco_trade_service.dto.DestinationPutDTO;
import org.kasbench.globeco_trade_service.entity.Destination;
import org.kasbench.globeco_trade_service.service.DestinationService;
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

@WebMvcTest(DestinationController.class)
public class DestinationControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private DestinationService destinationService;
    @Autowired
    private ObjectMapper objectMapper;

    private Destination destination;

    @BeforeEach
    void setup() {
        destination = new Destination();
        destination.setId(1);
        destination.setAbbreviation("ML");
        destination.setDescription("Merrill Lynch");
        destination.setVersion(1);
    }

    @Test
    void testGetAllDestinations() throws Exception {
        Mockito.when(destinationService.getAllDestinations()).thenReturn(Arrays.asList(destination));
        mockMvc.perform(get("/api/v1/destinations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].abbreviation").value("ML"));
    }

    @Test
    void testGetDestinationByIdFound() throws Exception {
        Mockito.when(destinationService.getDestinationById(1)).thenReturn(Optional.of(destination));
        mockMvc.perform(get("/api/v1/destinations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.abbreviation").value("ML"));
    }

    @Test
    void testGetDestinationByIdNotFound() throws Exception {
        Mockito.when(destinationService.getDestinationById(2)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/destinations/2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateDestination() throws Exception {
        DestinationPostDTO postDTO = new DestinationPostDTO();
        postDTO.setAbbreviation("ML");
        postDTO.setDescription("Merrill Lynch");
        Mockito.when(destinationService.createDestination(any(Destination.class))).thenReturn(destination);
        mockMvc.perform(post("/api/v1/destinations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.abbreviation").value("ML"));
    }

    @Test
    void testUpdateDestinationFound() throws Exception {
        DestinationPutDTO putDTO = new DestinationPutDTO();
        putDTO.setId(1);
        putDTO.setAbbreviation("ML");
        putDTO.setDescription("Updated");
        putDTO.setVersion(1);
        Destination updated = new Destination();
        updated.setId(1);
        updated.setAbbreviation("ML");
        updated.setDescription("Updated");
        updated.setVersion(1);
        Mockito.when(destinationService.updateDestination(eq(1), any(Destination.class))).thenReturn(updated);
        mockMvc.perform(put("/api/v1/destinations/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(putDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated"));
    }

    @Test
    void testUpdateDestinationNotFound() throws Exception {
        DestinationPutDTO putDTO = new DestinationPutDTO();
        putDTO.setId(2);
        putDTO.setAbbreviation("XX");
        putDTO.setDescription("Not Found");
        putDTO.setVersion(1);
        Mockito.when(destinationService.updateDestination(eq(2), any(Destination.class))).thenThrow(new IllegalArgumentException("Not found"));
        mockMvc.perform(put("/api/v1/destinations/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(putDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteDestinationFound() throws Exception {
        Mockito.doNothing().when(destinationService).deleteDestination(1, 1);
        mockMvc.perform(delete("/api/v1/destinations/1?version=1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteDestinationNotFound() throws Exception {
        Mockito.doThrow(new IllegalArgumentException("Not found")).when(destinationService).deleteDestination(2, 1);
        mockMvc.perform(delete("/api/v1/destinations/2?version=1"))
                .andExpect(status().isNotFound());
    }
} 