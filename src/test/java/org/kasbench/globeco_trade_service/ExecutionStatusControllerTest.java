package org.kasbench.globeco_trade_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kasbench.globeco_trade_service.dto.ExecutionStatusPostDTO;
import org.kasbench.globeco_trade_service.dto.ExecutionStatusPutDTO;
import org.kasbench.globeco_trade_service.entity.ExecutionStatus;
import org.kasbench.globeco_trade_service.repository.ExecutionStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
public class ExecutionStatusControllerTest extends AbstractH2Test {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ExecutionStatusRepository repository;
    @Autowired
    private ObjectMapper objectMapper;

    private ExecutionStatus status;

    @BeforeEach
    void setUp() {
        status = new ExecutionStatus();
        status.setAbbreviation("TEST");
        status.setDescription("Test status");
        status = repository.saveAndFlush(status);
    }

    @Test
    void testGetAllExecutionStatuses() throws Exception {
        mockMvc.perform(get("/api/v1/executionStatuses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].abbreviation", org.hamcrest.Matchers.hasItem("TEST")));
    }

    @Test
    void testGetExecutionStatusById_found() throws Exception {
        mockMvc.perform(get("/api/v1/executionStatuses/" + status.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(status.getId()))
                .andExpect(jsonPath("$.abbreviation").value("TEST"));
    }

    @Test
    void testGetExecutionStatusById_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/executionStatuses/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateExecutionStatus() throws Exception {
        ExecutionStatusPostDTO dto = new ExecutionStatusPostDTO();
        dto.setAbbreviation("NEWPOST");
        dto.setDescription("Created via POST");
        mockMvc.perform(post("/api/v1/executionStatuses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.abbreviation").value("NEWPOST"));
    }

    @Test
    void testUpdateExecutionStatus() throws Exception {
        ExecutionStatusPutDTO dto = new ExecutionStatusPutDTO();
        dto.setId(status.getId());
        dto.setAbbreviation("UPDATED");
        dto.setDescription("Updated description");
        dto.setVersion(status.getVersion());
        mockMvc.perform(put("/api/v1/executionStatuses/" + status.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.abbreviation").value("UPDATED"));
    }

    @Test
    void testDeleteExecutionStatus() throws Exception {
        mockMvc.perform(delete("/api/v1/executionStatuses/" + status.getId())
                        .param("version", status.getVersion().toString()))
                .andExpect(status().isNoContent());
        Optional<ExecutionStatus> deleted = repository.findById(status.getId());
        assertThat(deleted).isEmpty();
    }

    @Test
    void testDeleteExecutionStatus_versionMismatch() throws Exception {
        mockMvc.perform(delete("/api/v1/executionStatuses/" + status.getId())
                        .param("version", "999"))
                .andExpect(status().isConflict());
    }
} 