package org.kasbench.globeco_trade_service;

import org.kasbench.globeco_trade_service.service.ExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.kasbench.globeco_trade_service.entity.Execution;
import org.kasbench.globeco_trade_service.dto.ExecutionResponseDTO;

@RestController
@RequestMapping("/api/v1")
public class ExecutionSubmitController {
    private final ExecutionService executionService;
    @Autowired
    private ExecutionController executionController;

    @Autowired
    public ExecutionSubmitController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/execution/{id}/submit")
    public ResponseEntity<?> submitExecution(@PathVariable Integer id) {
        long startTime = System.currentTimeMillis();
        try {
            ExecutionService.SubmitResult result = executionService.submitExecution(id);
            if (result.getStatus() != null && result.getStatus().equals("submitted")) {
                var opt = executionService.getExecutionById(id);
                if (opt.isPresent()) {
                    Execution execution = opt.get();
                    ExecutionResponseDTO dto = executionController.toResponseDTO(execution);
                    return ResponseEntity.ok(dto);
                } else {
                    return ResponseEntity.status(404).body(java.util.Map.of("error", "Execution not found after submit"));
                }
            } else if (result.getError() != null) {
                String error = result.getError();
                if (error.contains("not found")) {
                    return ResponseEntity.status(404).body(java.util.Map.of("error", error));
                } else if (error.contains("Client error")) {
                    return ResponseEntity.status(400).body(java.util.Map.of("error", error));
                } else if (error.contains("unavailable")) {
                    return ResponseEntity.status(500).body(java.util.Map.of("error", error));
                } else {
                    return ResponseEntity.status(500).body(java.util.Map.of("error", error));
                }
            } else {
                return ResponseEntity.status(500).body(java.util.Map.of("error", "Unknown error"));
            }
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            org.slf4j.LoggerFactory.getLogger(ExecutionSubmitController.class).info("(Execution Submit Controller) submitExecution method execution time: {} ms", executionTime);
    }
    }
} 