# Performance Optimization Guide

## Executive Summary

This document addresses critical performance issues identified in the Globeco Trade Service, specifically:
1. **Transaction commit delays** causing 4-13 second overhead under load
2. **HTTP metrics filter** optimization recommendations
3. **Database performance** tuning strategies

## Issue Analysis

### Transaction Commit Performance Problem

**Symptoms:**
- Service method completes in ~13 seconds
- Controller shows total time of ~25 seconds
- 4-13 second delay occurs during Spring transaction commit
- Issue is reproducible under load testing

**Root Cause:**
The delay occurs between method completion and transaction commit due to:
- Database connection pool contention
- Database lock contention under concurrent load
- Slow database commit operations
- Potential deadlock resolution delays

## Recommendations

### 1. Database Performance Optimization

#### A. Connection Pool Tuning
```properties
# Increase connection pool size for high concurrency
spring.datasource.hikari.maximum-pool-size=100
spring.datasource.hikari.minimum-idle=25

# Reduce connection timeout to fail fast
spring.datasource.hikari.connection-timeout=10000

# Optimize connection lifecycle
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=900000

# Enable connection leak detection
spring.datasource.hikari.leak-detection-threshold=30000
```

#### B. Database Query Optimization
1. **Add Database Indexes:**
   ```sql
   -- Analyze slow queries and add appropriate indexes
   CREATE INDEX CONCURRENTLY idx_trade_orders_portfolio_security 
   ON trade_orders(portfolio_id, security_id);
   
   CREATE INDEX CONCURRENTLY idx_executions_trade_order_status 
   ON executions(trade_order_id, execution_status_id);
   ```

2. **Query Analysis:**
   - Enable query logging temporarily to identify slow queries
   - Use `EXPLAIN ANALYZE` on problematic queries
   - Consider query plan optimization

#### C. Transaction Optimization
```properties
# Optimize Hibernate batch processing
spring.jpa.properties.hibernate.jdbc.batch_size=100
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

# Reduce transaction isolation if appropriate
spring.jpa.properties.hibernate.connection.isolation=2
```

### 2. Application-Level Optimizations

#### A. Reduce Transaction Scope
**Current Issue:** Large transaction scope increases lock duration

**Solution:** Split operations into smaller transactions
```java
@Service
public class OptimizedTradeOrderService {
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Execution createExecutionOnly(TradeOrder tradeOrder, TradeOrderSubmitDTO dto) {
        // Create execution record only
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW) 
    public void updateTradeOrderOnly(Integer tradeOrderId, BigDecimal quantity) {
        // Update trade order only
    }
    
    // Coordinate without holding long transactions
    public Execution submitTradeOrder(Integer tradeOrderId, TradeOrderSubmitDTO dto, boolean noExecuteSubmit) {
        // 1. Create execution (short transaction)
        Execution execution = createExecutionOnly(tradeOrder, dto);
        
        // 2. Update trade order (short transaction)  
        updateTradeOrderOnly(tradeOrderId, dto.getQuantity());
        
        // 3. Submit to external service (no transaction)
        if (!noExecuteSubmit) {
            submitToExternalService(execution);
        }
        
        return execution;
    }
}
```

#### B. Async Processing for External Calls
```java
@Service
public class AsyncExecutionService {
    
    @Async("executionTaskExecutor")
    public CompletableFuture<Void> submitToExternalServiceAsync(Execution execution) {
        // Move external service calls outside transaction
        return CompletableFuture.runAsync(() -> {
            executionService.submitExecution(execution.getId());
        });
    }
}

@Configuration
public class AsyncConfig {
    @Bean("executionTaskExecutor")
    public TaskExecutor executionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("execution-");
        executor.initialize();
        return executor;
    }
}
```

### 3. Database Infrastructure Recommendations

#### A. PostgreSQL Configuration
```sql
-- Increase shared buffers for better caching
shared_buffers = 256MB

-- Optimize checkpoint behavior
checkpoint_completion_target = 0.9
wal_buffers = 16MB

-- Improve concurrent performance
max_connections = 200
effective_cache_size = 1GB

-- Reduce lock wait time
deadlock_timeout = 1s
lock_timeout = 30s
```

#### B. Monitoring and Alerting
1. **Database Metrics to Monitor:**
   - Active connections
   - Lock wait time
   - Transaction duration
   - Deadlock frequency
   - Query execution time

2. **Application Metrics:**
   - Connection pool utilization
   - Transaction commit time
   - Request queue depth

### 4. HTTP Metrics Filter Optimization

#### A. Current Performance Issues
The HTTP metrics filter creates performance overhead by:
- Creating Timer metrics on every request
- Complex SLO configuration
- Synchronous metrics recording
- String processing for path normalization

#### B. Optimized Implementation

**Step 1: Re-enable the Filter**
```java
// Uncomment in HttpMetricsFilter.java
@Component
public class HttpMetricsFilter implements Filter {

// Uncomment in HttpMetricsConfiguration.java  
@Configuration
public class HttpMetricsConfiguration {
```

**Step 2: Implement Optimized Version**
```java
@Component
public class OptimizedHttpMetricsFilter implements Filter {
    
    private final MeterRegistry meterRegistry;
    private final AtomicInteger inFlightRequestsCounter;
    
    // Pre-create common metrics to avoid runtime creation
    private final Timer.Builder timerBuilder;
    private final Counter.Builder counterBuilder;
    
    // Cache for normalized paths to avoid repeated processing
    private final ConcurrentHashMap<String, String> pathCache = new ConcurrentHashMap<>();
    
    public OptimizedHttpMetricsFilter(MeterRegistry meterRegistry, AtomicInteger inFlightRequestsCounter) {
        this.meterRegistry = meterRegistry;
        this.inFlightRequestsCounter = inFlightRequestsCounter;
        
        // Pre-configure builders
        this.timerBuilder = Timer.builder("http_request_duration")
                .description("Duration of HTTP requests")
                .serviceLevelObjectives(
                    Duration.ofMillis(100),
                    Duration.ofMillis(500), 
                    Duration.ofMillis(1000),
                    Duration.ofMillis(5000)
                )
                .maximumExpectedValue(Duration.ofMillis(30000));
                
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

        long startTime = System.nanoTime();
        inFlightRequestsCounter.incrementAndGet();

        try {
            chain.doFilter(request, response);
        } finally {
            // Record metrics asynchronously to avoid blocking request
            CompletableFuture.runAsync(() -> recordMetricsAsync(httpRequest, httpResponse, startTime));
            inFlightRequestsCounter.decrementAndGet();
        }
    }
    
    private void recordMetricsAsync(HttpServletRequest request, HttpServletResponse response, long startTime) {
        try {
            String method = request.getMethod().toUpperCase();
            String path = getCachedNormalizedPath(request);
            String status = String.valueOf(response.getStatus());
            
            long durationNanos = System.nanoTime() - startTime;
            
            // Use pre-configured builders
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
    
    private String getCachedNormalizedPath(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return pathCache.computeIfAbsent(requestURI, this::normalizePathParameters);
    }
    
    private String normalizePathParameters(String path) {
        if (path == null) return "unknown";
        
        // Simplified normalization - cache results
        return path.replaceAll("/\\d+", "/{id}")
                  .replaceAll("/[0-9a-fA-F-]{36}", "/{uuid}");
    }
}
```

**Step 3: Configuration for Async Metrics**
```java
@Configuration
@EnableAsync
public class MetricsAsyncConfig {
    
    @Bean("metricsTaskExecutor")
    public TaskExecutor metricsTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("metrics-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        executor.initialize();
        return executor;
    }
}
```

### 5. Implementation Priority

#### Phase 1 (Immediate - High Impact)
1. **Database connection pool tuning** - Increase pool size and optimize timeouts
2. **Enable query logging** - Identify slow queries
3. **Add database indexes** - Based on query analysis

#### Phase 2 (Short Term - Medium Impact)  
1. **Optimize HTTP metrics filter** - Implement async recording
2. **Split large transactions** - Reduce transaction scope
3. **Database configuration tuning** - PostgreSQL optimization

#### Phase 3 (Long Term - Architectural)
1. **Async processing** - Move external calls outside transactions  
2. **Event-driven architecture** - Consider eventual consistency
3. **Database sharding/partitioning** - If data volume requires it

### 6. Monitoring and Validation

#### Key Metrics to Track
```properties
# Add to application.properties for monitoring
management.metrics.export.prometheus.enabled=true
management.metrics.distribution.percentiles.http.server.requests=0.5,0.95,0.99
management.metrics.distribution.sla.http.server.requests=100ms,500ms,1s,5s
```

#### Success Criteria
- Transaction commit time < 1 second under load
- HTTP metrics filter overhead < 10ms per request
- Database connection pool utilization < 80%
- Zero connection pool timeouts

### 7. Testing Strategy

1. **Load Testing:** Reproduce the issue with current configuration
2. **Incremental Changes:** Apply optimizations one at a time
3. **Performance Monitoring:** Track metrics before/after each change
4. **Rollback Plan:** Keep previous configurations for quick rollback

## Conclusion

The primary issue is database transaction commit performance under load. The recommended approach is to start with connection pool optimization and database query analysis, then progressively implement application-level optimizations. The HTTP metrics filter should be re-enabled with async processing to minimize request overhead.