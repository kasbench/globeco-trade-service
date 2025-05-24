package org.kasbench.globeco_trade_service;

import org.kasbench.globeco_trade_service.service.ExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ExecutionSubmitController {
    private final ExecutionService executionService;

    @Autowired
    public ExecutionSubmitController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/execution/{id}/submit")
    public ResponseEntity<?> submitExecution(@PathVariable Integer id) {
        ExecutionService.SubmitResult result = executionService.submitExecution(id);
        if (result.getStatus() != null && result.getStatus().equals("submitted")) {
            return ResponseEntity.ok(java.util.Map.of("status", "submitted"));
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
    }
} 