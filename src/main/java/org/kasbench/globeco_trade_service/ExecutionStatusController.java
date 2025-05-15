package org.kasbench.globeco_trade_service;

import org.kasbench.globeco_trade_service.dto.ExecutionStatusPostDTO;
import org.kasbench.globeco_trade_service.dto.ExecutionStatusPutDTO;
import org.kasbench.globeco_trade_service.dto.ExecutionStatusResponseDTO;
import org.kasbench.globeco_trade_service.entity.ExecutionStatus;
import org.kasbench.globeco_trade_service.service.ExecutionStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/executionStatuses")
public class ExecutionStatusController {
    private final ExecutionStatusService executionStatusService;

    @Autowired
    public ExecutionStatusController(ExecutionStatusService executionStatusService) {
        this.executionStatusService = executionStatusService;
    }

    @GetMapping
    public List<ExecutionStatusResponseDTO> getAllExecutionStatuses() {
        return executionStatusService.getAllExecutionStatuses().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExecutionStatusResponseDTO> getExecutionStatusById(@PathVariable Integer id) {
        Optional<ExecutionStatus> status = executionStatusService.getExecutionStatusById(id);
        return status.map(s -> ResponseEntity.ok(toResponseDTO(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ExecutionStatusResponseDTO> createExecutionStatus(@RequestBody ExecutionStatusPostDTO dto) {
        ExecutionStatus status = fromPostDTO(dto);
        ExecutionStatus created = executionStatusService.createExecutionStatus(status);
        return new ResponseEntity<>(toResponseDTO(created), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExecutionStatusResponseDTO> updateExecutionStatus(@PathVariable Integer id, @RequestBody ExecutionStatusPutDTO dto) {
        ExecutionStatus status = fromPutDTO(dto);
        ExecutionStatus updated = executionStatusService.updateExecutionStatus(id, status);
        return ResponseEntity.ok(toResponseDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExecutionStatus(@PathVariable Integer id, @RequestParam Integer version) {
        executionStatusService.deleteExecutionStatus(id, version);
        return ResponseEntity.noContent().build();
    }

    private ExecutionStatusResponseDTO toResponseDTO(ExecutionStatus status) {
        ExecutionStatusResponseDTO dto = new ExecutionStatusResponseDTO();
        dto.setId(status.getId());
        dto.setAbbreviation(status.getAbbreviation());
        dto.setDescription(status.getDescription());
        dto.setVersion(status.getVersion());
        return dto;
    }

    private ExecutionStatus fromPostDTO(ExecutionStatusPostDTO dto) {
        ExecutionStatus status = new ExecutionStatus();
        status.setAbbreviation(dto.getAbbreviation());
        status.setDescription(dto.getDescription());
        return status;
    }

    private ExecutionStatus fromPutDTO(ExecutionStatusPutDTO dto) {
        ExecutionStatus status = new ExecutionStatus();
        status.setId(dto.getId());
        status.setAbbreviation(dto.getAbbreviation());
        status.setDescription(dto.getDescription());
        status.setVersion(dto.getVersion());
        return status;
    }
} 