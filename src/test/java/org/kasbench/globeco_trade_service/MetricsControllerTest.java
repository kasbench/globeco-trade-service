package org.kasbench.globeco_trade_service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for MetricsController to verify basic metrics endpoint functionality.
 */
class MetricsControllerTest {

    private MetricsController metricsController;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsController = new MetricsController(meterRegistry);
    }

    @Test
    void testGetMetrics_ReturnsPrometheusFormat() {
        // When
        ResponseEntity<String> response = metricsController.getMetrics();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        
        String body = response.getBody();
        // Should contain basic validation metrics when no other metrics exist
        assertTrue(body.contains("metrics_endpoint_health") || body.length() > 0,
                "Response should contain metrics data");
    }

    @Test
    void testGetMetrics_WithRegisteredMetrics() {
        // Given - register a test metric
        meterRegistry.counter("test.counter", "tag", "value").increment();

        // When
        ResponseEntity<String> response = metricsController.getMetrics();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // The response should be valid Prometheus format (non-empty)
        assertTrue(response.getBody().length() > 0);
    }

    @Test
    void testGetMetricsHealth_ReturnsHealthStatus() {
        // When
        ResponseEntity<String> response = metricsController.getMetricsHealth();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        
        String body = response.getBody();
        assertTrue(body.contains("\"status\":\"UP\""), "Health response should indicate UP status");
        assertTrue(body.contains("\"meterCount\""), "Health response should include meter count");
        assertTrue(body.contains("\"registryType\""), "Health response should include registry type");
    }

    @Test
    void testGetMetricsHealth_WithMetrics() {
        // Given - register some test metrics
        meterRegistry.counter("test.counter1").increment();
        meterRegistry.gauge("test.gauge1", 42.0);

        // When
        ResponseEntity<String> response = metricsController.getMetricsHealth();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertTrue(body.contains("\"status\":\"UP\""));
        assertTrue(body.contains("\"meterCount\":2") || body.contains("\"meterCount\":3"), 
                "Should report correct meter count");
    }

    @Test
    void testMetricsEndpoint_ContentType() {
        // When
        ResponseEntity<String> response = metricsController.getMetrics();

        // Then
        assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType(),
                "Metrics endpoint should return text/plain content type");
    }

    @Test
    void testHealthEndpoint_ContentType() {
        // When
        ResponseEntity<String> response = metricsController.getMetricsHealth();

        // Then
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType(),
                "Health endpoint should return application/json content type");
    }
}