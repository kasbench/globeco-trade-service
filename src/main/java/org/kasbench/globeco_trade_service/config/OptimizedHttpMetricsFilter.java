package org.kasbench.globeco_trade_service.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Optimized servlet filter that intercepts all HTTP requests to record metrics asynchronously.
 * This implementation addresses performance concerns by:
 * - Recording metrics asynchronously to avoid blocking requests
 * - Caching path normalization results for better performance
 * - Pre-configuring metric builders to reduce runtime overhead
 * 
 * Records three types of metrics:
 * - http_requests_total (Counter) - total number of requests with labels
 * - http_request_duration (Timer) - request duration histogram with labels
 * - http_requests_in_flight (Gauge) - current number of requests being processed
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4
 */
@Component
public class OptimizedHttpMetricsFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(OptimizedHttpMetricsFilter.class);

    private final MeterRegistry meterRegistry;
    private final AtomicInteger inFlightRequestsCounter;
    private final TaskExecutor metricsRecordingExecutor;
    
    // Pre-configured builders to reduce runtime overhead
    private final Timer.Builder timerBuilder;
    private final Counter.Builder counterBuilder;
    
    // Cache for path normalization to improve performance
    private final ConcurrentHashMap<String, String> pathNormalizationCache = new ConcurrentHashMap<>();
    
    // Cache size limit to prevent memory leaks
    private static final int MAX_CACHE_SIZE = 1000;

    @Autowired
    public OptimizedHttpMetricsFilter(MeterRegistry meterRegistry,
                                    AtomicInteger inFlightRequestsCounter,
                                    @Qualifier("metricsRecordingExecutor") TaskExecutor metricsRecordingExecutor) {
        this.meterRegistry = meterRegistry;
        this.inFlightRequestsCounter = inFlightRequestsCounter;
        this.metricsRecordingExecutor = metricsRecordingExecutor;
        
        // Pre-configure timer builder with histogram configuration
        this.timerBuilder = Timer.builder("http_request_duration")
                .description("Duration of HTTP requests")
                .serviceLevelObjectives(
                        Duration.ofMillis(5),
                        Duration.ofMillis(10),
                        Duration.ofMillis(25),
                        Duration.ofMillis(50),
                        Duration.ofMillis(100),
                        Duration.ofMillis(250),
                        Duration.ofMillis(500),
                        Duration.ofMillis(1_000),
                        Duration.ofMillis(2_000),
                        Duration.ofMillis(5_000),
                        Duration.ofMillis(10_000))
                .maximumExpectedValue(Duration.ofMillis(20_000))
                .publishPercentileHistogram(false);
        
        // Pre-configure counter builder
        this.counterBuilder = Counter.builder("http_requests_total")
                .description("Total number of HTTP requests");
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
        inFlightRequestsCounter.incrementAndGet();

        try {
            // Process the request
            chain.doFilter(request, response);
        } finally {
            // Record metrics asynchronously to avoid blocking the request
            CompletableFuture.runAsync(() -> 
                recordMetricsAsync(httpRequest, httpResponse, startTime), 
                metricsRecordingExecutor);
            
            // Always decrement in-flight counter synchronously
            inFlightRequestsCounter.decrementAndGet();
        }
    }

    /**
     * Records the HTTP request metrics asynchronously after request completion.
     * This method runs in a separate thread pool to avoid blocking request processing.
     * 
     * Requirements: 5.1, 5.2
     */
    private void recordMetricsAsync(HttpServletRequest request, HttpServletResponse response, long startTime) {
        try {
            // Extract labels using cached normalization
            String method = normalizeHttpMethod(request.getMethod());
            String path = getCachedNormalizedPath(request);
            String status = normalizeStatusCode(response.getStatus());
            
            long durationNanos = System.nanoTime() - startTime;
            
            // Use pre-configured builders for better performance
            counterBuilder
                .tag("method", method)
                .tag("path", path)
                .tag("status", status)
                .register(meterRegistry)
                .increment();
                
            timerBuilder
                .tag("method", method)
                .tag("path", path)
                .tag("status", status)
                .register(meterRegistry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
                
        } catch (Exception e) {
            // Log but don't let metrics recording affect requests
            logger.debug("Failed to record HTTP metrics", e);
        }
    }

    /**
     * Gets normalized path with caching to improve performance.
     * Uses a concurrent hash map to cache normalization results.
     * 
     * Requirements: 5.3
     */
    private String getCachedNormalizedPath(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        if (requestURI == null) {
            return "unknown";
        }
        
        // Check cache first
        String cachedPath = pathNormalizationCache.get(requestURI);
        if (cachedPath != null) {
            return cachedPath;
        }
        
        // Normalize and cache the result
        String normalizedPath = extractRoutePath(request);
        
        // Prevent cache from growing too large
        if (pathNormalizationCache.size() < MAX_CACHE_SIZE) {
            pathNormalizationCache.put(requestURI, normalizedPath);
        }
        
        return normalizedPath;
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
     * Normalizes path parameters in URLs by replacing numeric IDs and UUIDs with placeholders.
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

        // Replace UUIDs (e.g., /api/users/550e8400-e29b-41d4-a716-446655440000 -> /api/users/{uuid})
        path = path.replaceAll("/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
                "/{uuid}");

        return path;
    }

    /**
     * Normalizes HTTP method names to uppercase.
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
     * Normalizes HTTP status codes to string format.
     * 
     * @param statusCode the HTTP status code
     * @return normalized status code as string
     */
    private String normalizeStatusCode(int statusCode) {
        return String.valueOf(statusCode);
    }
}