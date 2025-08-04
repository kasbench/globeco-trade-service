package org.kasbench.globeco_trade_service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller that exposes metrics in Prometheus format for scraping.
 * This provides early validation of metrics endpoint functionality.
 */
@RestController
public class MetricsController {

    private static final Logger logger = LoggerFactory.getLogger(MetricsController.class);
    
    private final MeterRegistry meterRegistry;

    @Autowired
    public MetricsController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Exposes metrics in Prometheus text format.
     * This endpoint provides basic validation that metrics can be scraped.
     * 
     * @return ResponseEntity containing metrics in Prometheus format
     */
    @GetMapping(value = "/metrics", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getMetrics() {
        try {
            logger.debug("Metrics endpoint accessed");
            
            // Generate basic Prometheus format metrics
            StringBuilder prometheusMetrics = new StringBuilder();
            prometheusMetrics.append("# HELP metrics_endpoint_health Indicates if metrics endpoint is healthy\n");
            prometheusMetrics.append("# TYPE metrics_endpoint_health gauge\n");
            prometheusMetrics.append("metrics_endpoint_health 1.0\n");
            
            // Add meter count metric
            int meterCount = meterRegistry.getMeters().size();
            prometheusMetrics.append("# HELP metrics_registry_meters_total Total number of meters in registry\n");
            prometheusMetrics.append("# TYPE metrics_registry_meters_total gauge\n");
            prometheusMetrics.append("metrics_registry_meters_total ").append(meterCount).append("\n");
            
            // Add basic JVM metrics if available
            List<Meter> jvmMeters = meterRegistry.getMeters().stream()
                    .filter(meter -> meter.getId().getName().startsWith("jvm"))
                    .limit(5) // Limit to avoid too much output in basic validation
                    .collect(Collectors.toList());
            
            for (Meter meter : jvmMeters) {
                prometheusMetrics.append("# Basic JVM metric: ").append(meter.getId().getName()).append("\n");
            }
            
            String result = prometheusMetrics.toString();
            logger.debug("Successfully returned {} characters of metrics data", result.length());
            
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(result);
                    
        } catch (Exception e) {
            logger.error("Error retrieving metrics", e);
            
            // Return basic error metric instead of failing completely
            String errorMetrics = "# Error retrieving metrics\n" +
                                "# TYPE metrics_endpoint_error gauge\n" +
                                "metrics_endpoint_error 1.0\n";
            
            return ResponseEntity.status(503)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(errorMetrics);
        }
    }

    /**
     * Basic health check for the metrics endpoint.
     * This verifies that the endpoint is accessible and responding.
     * 
     * @return ResponseEntity indicating metrics endpoint health
     */
    @GetMapping(value = "/metrics/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getMetricsHealth() {
        try {
            // Verify that we can access the meter registry
            int meterCount = meterRegistry.getMeters().size();
            
            String healthResponse = String.format(
                "{\"status\":\"UP\",\"details\":{\"meterCount\":%d,\"registryType\":\"%s\"}}",
                meterCount,
                meterRegistry.getClass().getSimpleName()
            );
            
            logger.debug("Metrics health check passed with {} meters", meterCount);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(healthResponse);
                    
        } catch (Exception e) {
            logger.error("Metrics health check failed", e);
            
            String errorResponse = "{\"status\":\"DOWN\",\"error\":\"" + e.getMessage() + "\"}";
            return ResponseEntity.status(503)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);
        }
    }
}