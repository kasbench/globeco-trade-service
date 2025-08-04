package org.kasbench.globeco_trade_service.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuration class for HTTP metrics infrastructure.
 * Initializes and registers the three core HTTP metrics:
 * - http_requests_total (Counter)
 * - http_request_duration_seconds (Timer/Histogram)
 * - http_requests_in_flight (Gauge)
 */
@Configuration
public class HttpMetricsConfiguration {

    private final AtomicInteger inFlightRequests = new AtomicInteger(0);

    /**
     * Creates and registers the HTTP requests total counter metric.
     * This metric tracks the total number of HTTP requests with labels for method, path, and status.
     * 
     * @param registry the MeterRegistry to register the metric with
     * @return the configured Counter instance
     */
    @Bean
    public Counter httpRequestsTotal(MeterRegistry registry) {
        return Counter.builder("http_requests_total")
                .description("Total number of HTTP requests")
                .register(registry);
    }

    /**
     * Creates and registers the HTTP request duration timer metric.
     * This metric tracks request durations as a histogram with custom buckets.
     * The buckets are configured according to the requirements: [0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10]
     * 
     * @param registry the MeterRegistry to register the metric with
     * @return the configured Timer instance
     */
    @Bean
    public Timer httpRequestDuration(MeterRegistry registry) {
        return Timer.builder("http_request_duration_seconds")
                .description("Duration of HTTP requests in seconds")
                .publishPercentileHistogram(true)
                .serviceLevelObjectives(
                    Duration.ofMillis(5),    // 0.005 seconds
                    Duration.ofMillis(10),   // 0.01 seconds
                    Duration.ofMillis(25),   // 0.025 seconds
                    Duration.ofMillis(50),   // 0.05 seconds
                    Duration.ofMillis(100),  // 0.1 seconds
                    Duration.ofMillis(250),  // 0.25 seconds
                    Duration.ofMillis(500),  // 0.5 seconds
                    Duration.ofSeconds(1),   // 1 second
                    Duration.ofMillis(2500), // 2.5 seconds
                    Duration.ofSeconds(5),   // 5 seconds
                    Duration.ofSeconds(10)   // 10 seconds
                )
                .register(registry);
    }

    /**
     * Creates and registers the HTTP requests in-flight gauge metric.
     * This metric tracks the current number of HTTP requests being processed.
     * No labels are included as per requirements.
     * 
     * @param registry the MeterRegistry to register the metric with
     * @return the configured Gauge instance
     */
    @Bean
    public Gauge httpRequestsInFlight(MeterRegistry registry) {
        return Gauge.builder("http_requests_in_flight", inFlightRequests, AtomicInteger::get)
                .description("Number of HTTP requests currently being processed")
                .register(registry);
    }

    /**
     * Provides access to the in-flight requests counter for use by the metrics filter.
     * 
     * @return the AtomicInteger tracking in-flight requests
     */
    @Bean
    public AtomicInteger inFlightRequestsCounter() {
        return inFlightRequests;
    }
}