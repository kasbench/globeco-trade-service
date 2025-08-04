package org.kasbench.globeco_trade_service.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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

    /**
     * Registers the HttpMetricsFilter to intercept all HTTP requests.
     * Sets high priority to ensure metrics are recorded for all requests.
     * 
     * @param httpMetricsFilter the filter instance to register
     * @return the configured FilterRegistrationBean
     */
    @Bean
    public FilterRegistrationBean<HttpMetricsFilter> httpMetricsFilterRegistration(HttpMetricsFilter httpMetricsFilter) {
        FilterRegistrationBean<HttpMetricsFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(httpMetricsFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(1); // High priority to capture all requests
        registration.setName("httpMetricsFilter");
        return registration;
    }

    /**
     * Configures a MeterFilter to apply custom histogram buckets to all HTTP duration timers.
     * This ensures consistent histogram buckets across all timer instances.
     * 
     * @return a MeterFilter that configures histogram buckets
     */
    @Bean
    public MeterFilter httpDurationMeterFilter() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(io.micrometer.core.instrument.Meter.Id id, DistributionStatisticConfig config) {
                if (id.getName().equals("http_request_duration_seconds")) {
                    System.out.println("MeterFilter: Configuring histogram buckets for " + id.getName() + " with tags: " + id.getTags());
                    return DistributionStatisticConfig.builder()
                        .serviceLevelObjectives(0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0)
                        .build()
                        .merge(config);
                }
                return config;
            }
        };
    }

    /**
     * Pre-registers HTTP metrics with sample tags to ensure they appear in metrics endpoints
     * even before any HTTP requests are made. This helps with metrics discovery and monitoring setup.
     * 
     * @param registry the MeterRegistry to register the metrics with
     * @return a string indicating successful initialization
     */
    @Bean
    public String httpMetricsInitializer(MeterRegistry registry) {
        // Pre-register counter with sample tags so it appears in metrics endpoints
        Counter.builder("http_requests_total")
                .description("Total number of HTTP requests")
                .tag("method", "GET")
                .tag("path", "/health")
                .tag("status", "200")
                .register(registry);

        // Don't pre-register timer - let the MeterFilter handle the configuration
                
        return "HTTP metrics pre-registered successfully";
    }
}