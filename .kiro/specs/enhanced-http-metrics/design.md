# Design Document

## Overview

This design implements standardized HTTP request metrics for the GlobeCo Trade Service using Spring Boot's Micrometer framework with OpenTelemetry integration. The solution leverages Spring Boot's existing actuator and micrometer dependencies to create three core metrics (counter, histogram, gauge) that track HTTP request patterns and performance. The implementation uses a servlet filter approach to intercept all HTTP requests and record metrics transparently.

## Architecture

### High-Level Architecture

```mermaid
graph TB
    A[HTTP Request] --> B[MetricsFilter]
    B --> C[Spring MVC Controllers]
    C --> D[Business Logic]
    D --> E[HTTP Response]
    E --> B
    B --> F[Micrometer Registry]
    F --> G[OTLP Exporter]
    G --> H[OpenTelemetry Collector]
    
    I[/metrics Endpoint] --> F
    
    subgraph "Metrics Components"
        F --> J[http_requests_total Counter]
        F --> K[http_request_duration_seconds Histogram]
        F --> L[http_requests_in_flight Gauge]
    end
```

### Component Integration

The metrics system integrates with the existing Spring Boot application through:

1. **Servlet Filter**: Intercepts all HTTP requests before they reach controllers
2. **Micrometer Registry**: Leverages existing OTLP registry configuration
3. **Spring Actuator**: Exposes metrics through the `/metrics` endpoint
4. **OpenTelemetry Export**: Uses existing OTLP configuration for metric export

## Components and Interfaces

### 1. HttpMetricsFilter

**Purpose**: Servlet filter that intercepts all HTTP requests to record metrics

**Key Responsibilities**:
- Start timing when request enters the filter
- Increment in-flight gauge at request start
- Record counter and histogram metrics at request completion
- Decrement in-flight gauge when request completes
- Extract and normalize labels (method, path, status)

**Interface**:
```java
@Component
public class HttpMetricsFilter implements Filter {
    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain);
    private String extractRoutePath(HttpServletRequest request);
    private String normalizeHttpMethod(String method);
    private String normalizeStatusCode(int statusCode);
}
```

### 2. HttpMetricsConfiguration

**Purpose**: Configuration class that initializes and registers metrics

**Key Responsibilities**:
- Create and register the three required metrics
- Configure histogram buckets
- Set up metric descriptions and base units
- Integrate with existing MeterRegistry

**Interface**:
```java
@Configuration
public class HttpMetricsConfiguration {
    @Bean
    public Counter httpRequestsTotal(MeterRegistry registry);
    @Bean 
    public Timer httpRequestDuration(MeterRegistry registry);
    @Bean
    public Gauge httpRequestsInFlight(MeterRegistry registry);
}
```

### 3. MetricsController

**Purpose**: REST controller that exposes metrics in Prometheus format

**Key Responsibilities**:
- Provide `/metrics` endpoint for scraping
- Format metrics in Prometheus text format
- Handle errors gracefully without affecting main application

**Interface**:
```java
@RestController
public class MetricsController {
    @GetMapping("/metrics")
    public ResponseEntity<String> getMetrics();
}
```

### 4. RequestMetricsHolder

**Purpose**: Thread-local storage for tracking request-specific metrics data

**Key Responsibilities**:
- Store start time for duration calculation
- Track in-flight request state
- Provide thread-safe access to request metrics data

**Interface**:
```java
public class RequestMetricsHolder {
    private static final ThreadLocal<RequestMetrics> holder = new ThreadLocal<>();
    public static void setStartTime(long startTime);
    public static long getStartTime();
    public static void clear();
}
```

## Data Models

### RequestMetrics

```java
public class RequestMetrics {
    private long startTimeNanos;
    private boolean inFlight;
    
    // getters and setters
}
```

### Metric Definitions

#### HTTP Requests Total Counter
- **Name**: `http_requests_total`
- **Type**: Counter
- **Description**: "Total number of HTTP requests"
- **Labels**: method, path, status
- **Implementation**: Micrometer Counter

#### HTTP Request Duration Histogram  
- **Name**: `http_request_duration_seconds`
- **Type**: Timer (Micrometer's histogram implementation)
- **Description**: "Duration of HTTP requests in seconds"
- **Labels**: method, path, status
- **Buckets**: [0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10]
- **Implementation**: Micrometer Timer

#### HTTP Requests In Flight Gauge
- **Name**: `http_requests_in_flight`
- **Type**: Gauge
- **Description**: "Number of HTTP requests currently being processed"
- **Labels**: None
- **Implementation**: AtomicInteger with Micrometer Gauge

## Error Handling

### Metrics Recording Failures

1. **Graceful Degradation**: If metric recording fails, log the error and continue request processing
2. **Exception Isolation**: Wrap all metrics operations in try-catch blocks
3. **Fallback Behavior**: Ensure request processing continues even if metrics are unavailable

### Filter Exception Handling

```java
try {
    // Record start metrics
    chain.doFilter(request, response);
} catch (Exception e) {
    // Ensure metrics are still recorded for failed requests
    recordMetrics(request, response, e);
    throw e;
} finally {
    // Always clean up thread-local storage
    RequestMetricsHolder.clear();
}
```

### Metrics Endpoint Resilience

- Return HTTP 503 if metrics registry is unavailable
- Provide partial metrics if some collectors fail
- Log errors without exposing internal details

## Testing Strategy

### Unit Tests

1. **HttpMetricsFilter Tests**
   - Verify metrics are recorded for successful requests
   - Test error scenarios and exception handling
   - Validate label extraction and normalization
   - Test thread-local cleanup

2. **HttpMetricsConfiguration Tests**
   - Verify all metrics are properly registered
   - Test histogram bucket configuration
   - Validate metric descriptions and units

3. **MetricsController Tests**
   - Test metrics endpoint response format
   - Verify Prometheus text format compliance
   - Test error handling scenarios

### Integration Tests

1. **End-to-End Metrics Flow**
   - Send HTTP requests to various endpoints
   - Verify metrics are recorded with correct labels
   - Test concurrent request handling
   - Validate OTLP export functionality

2. **Performance Tests**
   - Measure metrics collection overhead
   - Test high-concurrency scenarios
   - Validate memory usage patterns

### Test Data Scenarios

- GET requests to health endpoints
- POST requests with various response codes
- Concurrent requests to test in-flight gauge
- Error scenarios (4xx, 5xx responses)
- Long-running requests for duration testing

## Implementation Considerations

### Performance Optimization

1. **Minimal Overhead**: Use efficient timing mechanisms (System.nanoTime())
2. **Label Caching**: Cache normalized label values where possible
3. **Thread Safety**: Use thread-local storage for request-specific data
4. **Lazy Initialization**: Initialize metrics only when first used

### Path Normalization Strategy

For Spring Boot applications, extract route patterns from:
1. `HandlerMapping` attributes when available
2. Request URI pattern matching for parameterized paths
3. Fallback to actual path with parameter sanitization

### Integration with Existing Configuration

Leverage existing application.properties configuration:
- Use existing `management.otlp.metrics.export.*` settings
- Integrate with current MeterRegistry bean
- Maintain compatibility with existing actuator endpoints

### Security Considerations

1. **Metrics Endpoint Security**: Consider adding authentication if needed
2. **Label Sanitization**: Remove sensitive data from path labels
3. **Resource Limits**: Prevent high-cardinality metrics from consuming excessive memory

## Deployment Integration

### Configuration Updates

No changes required to existing OTLP configuration. The new metrics will automatically be exported through the existing:
```properties
management.otlp.metrics.export.url=http://otel-collector-collector.monitoring.svc.cluster.local:4318/v1/metrics
```

### Monitoring Dashboard Integration

The metrics will be available for:
- Grafana dashboards showing request rates and latencies
- Alerting rules based on error rates and response times
- Service-level objective (SLO) monitoring

### Validation Checklist

After deployment, verify:
- [ ] All three metrics appear in OTLP collector
- [ ] Labels contain expected values
- [ ] Histogram buckets are properly configured
- [ ] In-flight gauge accurately tracks concurrent requests
- [ ] `/metrics` endpoint returns valid Prometheus format
- [ ] No performance degradation in request processing