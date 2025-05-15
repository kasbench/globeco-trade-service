package org.kasbench.globeco_trade_service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    @GetMapping("/liveness")
    public ResponseEntity<Map<String, String>> liveness() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
    @GetMapping("/readiness")
    public ResponseEntity<Map<String, String>> readiness() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
    @GetMapping("/startup")
    public ResponseEntity<Map<String, String>> startup() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
} 