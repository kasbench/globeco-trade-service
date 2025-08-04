package org.kasbench.globeco_trade_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the metrics endpoint to verify it works in a real Spring Boot context.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "management.endpoints.web.exposure.include=health,info,metrics",
    "management.endpoint.metrics.enabled=true"
})
class MetricsEndpointIntegrationTest extends AbstractPostgresContainerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testMetricsEndpoint_ReturnsPrometheusFormat() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/metrics", String.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        
        String body = response.getBody();
        assertTrue(body.contains("metrics_endpoint_health"), 
                "Response should contain health metric");
        assertTrue(body.contains("metrics_registry_meters_total"), 
                "Response should contain meter count metric");
        assertTrue(body.contains("# HELP"), 
                "Response should contain Prometheus help comments");
        assertTrue(body.contains("# TYPE"), 
                "Response should contain Prometheus type comments");
    }

    @Test
    void testMetricsHealthEndpoint_ReturnsHealthStatus() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/metrics/health", String.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        
        String body = response.getBody();
        assertTrue(body.contains("\"status\":\"UP\""), 
                "Health response should indicate UP status");
        assertTrue(body.contains("\"meterCount\""), 
                "Health response should include meter count");
        assertTrue(body.contains("\"registryType\""), 
                "Health response should include registry type");
    }

    @Test
    void testMetricsEndpoint_ValidPrometheusFormat() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/metrics", String.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertNotNull(body);
        
        // Verify basic Prometheus format structure
        String[] lines = body.split("\n");
        boolean hasHelpLine = false;
        boolean hasTypeLine = false;
        boolean hasMetricLine = false;
        
        for (String line : lines) {
            if (line.startsWith("# HELP")) {
                hasHelpLine = true;
            } else if (line.startsWith("# TYPE")) {
                hasTypeLine = true;
            } else if (line.matches("^[a-zA-Z_][a-zA-Z0-9_]* \\d+(\\.\\d+)?$")) {
                hasMetricLine = true;
            }
        }
        
        assertTrue(hasHelpLine, "Should have HELP lines");
        assertTrue(hasTypeLine, "Should have TYPE lines");
        assertTrue(hasMetricLine, "Should have metric value lines");
    }

    @Test
    void testMetricsEndpoint_AccessibilityCheck() {
        // This test verifies the endpoint is accessible and returns a valid response
        // which satisfies the "basic health check to verify endpoint accessibility" requirement
        
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/metrics", String.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode(), 
                "Metrics endpoint should be accessible");
        assertNotNull(response.getBody(), 
                "Metrics endpoint should return a body");
        assertTrue(response.getBody().length() > 0, 
                "Metrics endpoint should return non-empty content");
    }
}