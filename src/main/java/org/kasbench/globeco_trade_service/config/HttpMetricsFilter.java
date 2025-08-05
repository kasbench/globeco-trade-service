package org.kasbench.globeco_trade_service.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servlet filter that intercepts all HTTP requests to record metrics.
 * Records three types of metrics:
 * - http_requests_total (Counter) - total number of requests with labels
 * - http_request_duration_seconds (Timer) - request duration histogram with
 * labels
 * - http_requests_in_flight (Gauge) - current number of requests being
 * processed
 */
@Component
public class HttpMetricsFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(HttpMetricsFilter.class);

    private final MeterRegistry meterRegistry;
    private final AtomicInteger inFlightRequestsCounter;

    /**
     * Thread-local storage for tracking request-specific metrics data
     */
    private static final ThreadLocal<RequestMetrics> requestMetricsHolder = new ThreadLocal<>();

    @Autowired
    public HttpMetricsFilter(MeterRegistry meterRegistry,
            AtomicInteger inFlightRequestsCounter) {
        this.meterRegistry = meterRegistry;
        this.inFlightRequestsCounter = inFlightRequestsCounter;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Start timing and increment in-flight counter
        long startTime = System.nanoTime();
        requestMetricsHolder.set(new RequestMetrics(startTime, true));
        inFlightRequestsCounter.incrementAndGet();

        try {
            // Process the request
            chain.doFilter(request, response);
        } catch (Exception e) {
            // Ensure metrics are recorded even for failed requests
            logger.debug("Exception during request processing, will still record metrics", e);
            throw e;
        } finally {
            try {
                // Record metrics
                recordMetrics(httpRequest, httpResponse, startTime);
            } catch (Exception e) {
                // Log error but don't let metrics recording break the request
                logger.error("Failed to record HTTP metrics", e);
            } finally {
                // Always decrement in-flight counter and clean up thread-local
                inFlightRequestsCounter.decrementAndGet();
                requestMetricsHolder.remove();
            }
        }
    }

    /**
     * Records the HTTP request metrics after request completion
     */
    private void recordMetrics(HttpServletRequest request, HttpServletResponse response, long startTime) {
        try {
            // Extract labels
            String method = normalizeHttpMethod(request.getMethod());
            String path = extractRoutePath(request);
            String status = normalizeStatusCode(response.getStatus());

            // Calculate duration in seconds
            long durationNanos = System.nanoTime() - startTime;

            // Record counter metric using the registry directly
            meterRegistry.counter("http_requests_total",
                    "method", method,
                    "path", path,
                    "status", status)
                    .increment();

            // Record timer metric with explicit histogram configuration
            Timer timer = Timer.builder("http_request_duration_seconds")
                    .description("Duration of HTTP requests in seconds")
                    .serviceLevelObjectives(
                            Duration.ofNanos(5_000_000), // 0.005 seconds
                            Duration.ofNanos(10_000_000), // 0.01 seconds
                            Duration.ofNanos(25_000_000), // 0.025 seconds
                            Duration.ofNanos(50_000_000), // 0.05 seconds
                            Duration.ofNanos(100_000_000), // 0.1 seconds
                            Duration.ofNanos(250_000_000), // 0.25 seconds
                            Duration.ofNanos(500_000_000), // 0.5 seconds
                            Duration.ofSeconds(1), // 1 second
                            Duration.ofNanos(2_500_000_000L), // 2.5 seconds
                            Duration.ofSeconds(5), // 5 seconds
                            Duration.ofSeconds(10) // 10 seconds
                    )
                    .tag("method", method)
                    .tag("path", path)
                    .tag("status", status)
                    .register(meterRegistry);

            timer.record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);

        } catch (Exception e) {
            logger.error("Error recording HTTP metrics", e);
        }
    }

    /**
     * Extracts and normalizes the route path from the HTTP request.
     * Attempts to use route patterns instead of actual URLs with parameters.
     * 
     * @param request the HTTP request
     * @return normalized path string
     */
    private String extractRoutePath(HttpServletRequest request) {
        try {
            // Try to get the route pattern from Spring MVC handler mapping
            Object bestMatchingPattern = request
                    .getAttribute("org.springframework.web.servlet.HandlerMapping.bestMatchingPattern");
            if (bestMatchingPattern != null) {
                return bestMatchingPattern.toString();
            }

            // Try to get the path within handler mapping
            Object pathWithinHandlerMapping = request
                    .getAttribute("org.springframework.web.servlet.HandlerMapping.pathWithinHandlerMapping");
            if (pathWithinHandlerMapping != null) {
                return pathWithinHandlerMapping.toString();
            }

            // Fallback to request URI with basic parameter normalization
            String requestURI = request.getRequestURI();
            if (requestURI != null) {
                return normalizePathParameters(requestURI);
            }

            return "unknown";
        } catch (Exception e) {
            logger.debug("Failed to extract route path, using fallback", e);
            return "unknown";
        }
    }

    /**
     * Normalizes path parameters in URLs by replacing numeric IDs and UUIDs with
     * placeholders
     * 
     * @param path the original path
     * @return normalized path with parameter placeholders
     */
    private String normalizePathParameters(String path) {
        if (path == null) {
            return "unknown";
        }

        // Replace numeric IDs (e.g., /api/users/123 -> /api/users/{id})
        path = path.replaceAll("/\\d+", "/{id}");

        // Replace UUIDs (e.g., /api/users/550e8400-e29b-41d4-a716-446655440000 ->
        // /api/users/{uuid})
        path = path.replaceAll("/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
                "/{uuid}");

        return path;
    }

    /**
     * Normalizes HTTP method names to uppercase
     * 
     * @param method the HTTP method
     * @return normalized method name in uppercase
     */
    private String normalizeHttpMethod(String method) {
        if (method == null) {
            return "UNKNOWN";
        }
        return method.toUpperCase();
    }

    /**
     * Normalizes HTTP status codes to string format
     * 
     * @param statusCode the HTTP status code
     * @return normalized status code as string
     */
    private String normalizeStatusCode(int statusCode) {
        return String.valueOf(statusCode);
    }

    /**
     * Inner class to hold request-specific metrics data in thread-local storage
     */
    private static class RequestMetrics {
        private final long startTimeNanos;
        private final boolean inFlight;

        public RequestMetrics(long startTimeNanos, boolean inFlight) {
            this.startTimeNanos = startTimeNanos;
            this.inFlight = inFlight;
        }

        public long getStartTimeNanos() {
            return startTimeNanos;
        }

        public boolean isInFlight() {
            return inFlight;
        }
    }
}