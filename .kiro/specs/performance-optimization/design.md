# Performance Optimization Design

## Overview

This design document outlines the comprehensive performance optimization strategy for the Globeco Trade Service to address critical performance issues under load, specifically targeting transaction commit delays, database connection pool contention, and inefficient external service integration patterns.

## Architecture

### Current Performance Issues Analysis

Based on the existing codebase analysis, the following performance bottlenecks have been identified:

1. **Transaction Commit Delays**: Large transaction scopes in `TradeOrderServiceImpl.submitTradeOrder()` causing 4-13 second delays
2. **Database Connection Pool**: Current pool size of 80 may be insufficient under high load
3. **External Service Integration**: Synchronous calls within transactions blocking commit operations
4. **HTTP Metrics Filter**: Currently disabled due to performance overhead concerns
5. **Cache Configuration**: Suboptimal TTL and size settings for external service data

### Target Architecture

The optimized architecture will implement the following patterns:

- **Microservice Transaction Pattern**: Split large transactions into smaller, focused operations
- **Async Processing Pattern**: Move external service calls outside transaction boundaries
- **Circuit Breaker Pattern**: Implement resilience for external service failures
- **Cache-Aside Pattern**: Optimize caching strategy for external service data
- **Connection Pool Optimization**: Right-size database connections for concurrent load

## Components and Interfaces

### 1. Database Connection Pool Configuration

#### Enhanced HikariCP Configuration
```yaml
spring:
  datasource:
    hikari:
      # Optimized for high concurrency
      maximum-pool-size: 100          # Increased from 80
      minimum-idle: 25                # Increased from 20
      connection-timeout: 10000       # Reduced from 30000 for fail-fast
      idle-timeout: 300000            # 5 minutes
      max-lifetime: 900000            # 15 minutes
      leak-detection-threshold: 30000 # Reduced from 60000
      
      # Performance optimizations
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true
        rewriteBatchedStatements: true
        cacheResultSetMetadata: true
        elideSetAutoCommits: true
        maintainTimeStats: false
```

#### Connection Pool Monitoring Component
```java
@Component
public class ConnectionPoolMonitor {
    private final HikariDataSource dataSource;
    private final MeterRegistry meterRegistry;
    
    @Scheduled(fixedRate = 30000)
    public void recordConnectionPoolMetrics() {
        HikariPoolMXBean poolBean = dataSource.getHikariPoolMXBean();
        
        Gauge.builder("hikari.connections.active")
            .register(meterRegistry, poolBean, HikariPoolMXBean::getActiveConnections);
        Gauge.builder("hikari.connections.idle")
            .register(meterRegistry, poolBean, HikariPoolMXBean::getIdleConnections);
        Gauge.builder("hikari.connections.total")
            .register(meterRegistry, poolBean, HikariPoolMXBean::getTotalConnections);
    }
}
```

### 2. Transaction Scope Optimization

#### Refactored Trade Order Service Architecture
```java
@Service
public class OptimizedTradeOrderService {
    
    // Separate transaction for execution creation
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Execution createExecutionRecord(TradeOrder tradeOrder, TradeOrderSubmitDTO dto) {
        // Create execution record only - short transaction
    }
    
    // Separate transaction for trade order update
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateTradeOrderQuantities(Integer tradeOrderId, BigDecimal quantity) {
        // Update trade order quantities only - short transaction
    }
    
    // Coordination method without transaction
    public Execution submitTradeOrder(Integer tradeOrderId, TradeOrderSubmitDTO dto, boolean noExecuteSubmit) {
        // 1. Create execution (short transaction)
        // 2. Update trade order (short transaction)
        // 3. Submit to external service (no transaction)
        // 4. Handle compensation if needed
    }
}
```

#### Transaction Compensation Handler
```java
@Component
public class TransactionCompensationHandler {
    
    public void compensateFailedSubmission(Execution execution, TradeOrder originalState) {
        // Implement saga pattern for transaction compensation
        CompletableFuture.runAsync(() -> {
            try {
                // Rollback execution
                executionRepository.deleteById(execution.getId());
                // Restore trade order state
                restoreTradeOrderState(originalState);
            } catch (Exception e) {
                // Send to dead letter queue for manual intervention
                deadLetterQueueService.send(new CompensationFailedEvent(execution, originalState, e));
            }
        });
    }
}
```

### 3. Asynchronous Processing Framework

#### Async Configuration
```java
@Configuration
@EnableAsync
public class AsyncProcessingConfig {
    
    @Bean("executionSubmissionExecutor")
    public TaskExecutor executionSubmissionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("execution-submit-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
    
    @Bean("metricsRecordingExecutor")
    public TaskExecutor metricsRecordingExecutor() {
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

#### Async Execution Service
```java
@Service
public class AsyncExecutionService {
    
    @Async("executionSubmissionExecutor")
    public CompletableFuture<ExecutionService.SubmitResult> submitExecutionAsync(Integer executionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return retryTemplate.execute(context -> 
                    executionService.submitExecution(executionId));
            } catch (Exception e) {
                // Handle failure with compensation
                compensationHandler.compensateFailedSubmission(executionId);
                throw new AsyncExecutionException("Failed to submit execution: " + executionId, e);
            }
        });
    }
}
```

### 4. Enhanced Caching Strategy

#### Optimized Cache Configuration
```java
@Configuration
public class OptimizedCacheConfig {
    
    @Bean("securityCache")
    public Cache<String, SecurityDTO> securityCache() {
        return Caffeine.newBuilder()
            .maximumSize(2000)                          // Increased from 1000
            .expireAfterWrite(Duration.ofMinutes(10))   // Increased from 5
            .refreshAfterWrite(Duration.ofMinutes(8))   // Proactive refresh
            .recordStats()
            .removalListener(this::onSecurityCacheRemoval)
            .buildAsync(this::loadSecurityAsync)
            .synchronous();
    }
    
    @Bean("portfolioCache")
    public Cache<String, PortfolioDTO> portfolioCache() {
        return Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(15))   // Increased from 5
            .refreshAfterWrite(Duration.ofMinutes(12))
            .recordStats()
            .removalListener(this::onPortfolioCacheRemoval)
            .buildAsync(this::loadPortfolioAsync)
            .synchronous();
    }
    
    // Cache warming strategy
    @EventListener(ApplicationReadyEvent.class)
    public void warmupCaches() {
        CompletableFuture.allOf(
            CompletableFuture.runAsync(this::warmupSecurityCache),
            CompletableFuture.runAsync(this::warmupPortfolioCache)
        );
    }
}
```

#### Batch Cache Loading Service
```java
@Service
public class BatchCacheLoadingService {
    
    public CompletableFuture<Map<String, SecurityDTO>> batchLoadSecurities(Set<String> securityIds) {
        return CompletableFuture.supplyAsync(() -> {
            return securityIds.parallelStream()
                .collect(Collectors.toConcurrentMap(
                    id -> id,
                    id -> securityCacheService.getSecurityById(id),
                    (existing, replacement) -> existing
                ));
        });
    }
    
    public CompletableFuture<Map<String, PortfolioDTO>> batchLoadPortfolios(Set<String> portfolioIds) {
        return CompletableFuture.supplyAsync(() -> {
            return portfolioIds.parallelStream()
                .collect(Collectors.toConcurrentMap(
                    id -> id,
                    id -> portfolioCacheService.getPortfolioById(id),
                    (existing, replacement) -> existing
                ));
        });
    }
}
```

### 5. Circuit Breaker Implementation

#### Resilience4j Configuration
```java
@Configuration
public class CircuitBreakerConfig {
    
    @Bean
    public CircuitBreaker securityServiceCircuitBreaker() {
        return CircuitBreaker.ofDefaults("securityService")
            .toBuilder()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .recordExceptions(HttpServerErrorException.class, ResourceAccessException.class)
            .ignoreExceptions(HttpClientErrorException.class)
            .build();
    }
    
    @Bean
    public CircuitBreaker portfolioServiceCircuitBreaker() {
        return CircuitBreaker.ofDefaults("portfolioService")
            .toBuilder()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .recordExceptions(HttpServerErrorException.class, ResourceAccessException.class)
            .ignoreExceptions(HttpClientErrorException.class)
            .build();
    }
}
```

#### Circuit Breaker Service Wrapper
```java
@Service
public class ResilientExternalServiceClient {
    
    private final CircuitBreaker securityServiceCircuitBreaker;
    private final CircuitBreaker portfolioServiceCircuitBreaker;
    
    public SecurityDTO getSecurityWithCircuitBreaker(String securityId) {
        return securityServiceCircuitBreaker.executeSupplier(() -> 
            securityServiceClient.getSecurityById(securityId));
    }
    
    public PortfolioDTO getPortfolioWithCircuitBreaker(String portfolioId) {
        return portfolioServiceCircuitBreaker.executeSupplier(() -> 
            portfolioServiceClient.getPortfolioById(portfolioId));
    }
}
```

### 6. Optimized HTTP Metrics Filter

#### Async HTTP Metrics Filter
```java
@Component
public class OptimizedHttpMetricsFilter implements Filter {
    
    private final MeterRegistry meterRegistry;
    private final AtomicInteger inFlightRequestsCounter;
    private final Timer.Builder timerBuilder;
    private final Counter.Builder counterBuilder;
    private final ConcurrentHashMap<String, String> pathCache = new ConcurrentHashMap<>();
    
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
            // Record metrics asynchronously
            CompletableFuture.runAsync(() -> 
                recordMetricsAsync(httpRequest, httpResponse, startTime), 
                metricsRecordingExecutor);
            inFlightRequestsCounter.decrementAndGet();
        }
    }
    
    private void recordMetricsAsync(HttpServletRequest request, HttpServletResponse response, long startTime) {
        try {
            String method = request.getMethod().toUpperCase();
            String path = getCachedNormalizedPath(request);
            String status = String.valueOf(response.getStatus());
            
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
}
```

### 7. Database Query Optimization

#### Enhanced JPA Configuration
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 100              # Increased from 50
          fetch_size: 50
        query:
          plan_cache_max_size: 2048
          plan_parameter_metadata_max_size: 128
        connection:
          provider_disables_autocommit: true
        order_inserts: true
        order_updates: true
        generate_statistics: false
```

#### Database Index Strategy
```sql
-- Performance-critical indexes for common query patterns
CREATE INDEX CONCURRENTLY idx_trade_orders_portfolio_security_submitted 
ON trade_orders (portfolio_id, security_id, submitted);

CREATE INDEX CONCURRENTLY idx_trade_orders_order_type_timestamp 
ON trade_orders (order_type, trade_timestamp DESC);

CREATE INDEX CONCURRENTLY idx_executions_trade_order_status 
ON executions (trade_order_id, execution_status_id);

-- Covering index for common SELECT operations
CREATE INDEX CONCURRENTLY idx_trade_orders_covering 
ON trade_orders (portfolio_id, security_id) 
INCLUDE (id, order_id, quantity, quantity_sent, submitted);

-- Partial index for active orders
CREATE INDEX CONCURRENTLY idx_trade_orders_active 
ON trade_orders (portfolio_id, security_id, trade_timestamp DESC) 
WHERE submitted = false;
```

#### Query Performance Monitor
```java
@Component
public class QueryPerformanceMonitor {
    
    @EventListener
    public void handleSlowQuery(SlowQueryEvent event) {
        if (event.getDuration().toMillis() > 1000) {
            logger.warn("Slow query detected: duration={}ms, query={}", 
                event.getDuration().toMillis(), event.getQuery());
            
            slowQueryCounter.increment(
                Tags.of(
                    "query_type", event.getQueryType(),
                    "duration_bucket", getDurationBucket(event.getDuration())
                )
            );
        }
    }
    
    private String getDurationBucket(Duration duration) {
        long millis = duration.toMillis();
        if (millis < 1000) return "fast";
        if (millis < 5000) return "slow";
        return "very_slow";
    }
}
```

### 8. JVM Optimization Configuration

#### Production JVM Settings
```bash
# Memory configuration
JAVA_OPTS="-server -Xms4g -Xmx8g -XX:NewRatio=3 -XX:SurvivorRatio=8 -XX:MaxMetaspaceSize=512m"

# G1GC configuration for low latency
GC_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:G1HeapRegionSize=16m -XX:+G1UseAdaptiveIHOP"

# Memory optimizations
MEMORY_OPTS="-XX:+UseStringDeduplication -XX:+UseCompressedOops -XX:+UseCompressedClassPointers"

# JIT compiler optimizations
JIT_OPTS="-XX:+UseCodeCacheFlushing -XX:ReservedCodeCacheSize=256m -XX:InitialCodeCacheSize=64m"

# Monitoring and profiling
MONITORING_OPTS="-XX:+FlightRecorder -XX:StartFlightRecording=duration=60s,filename=trade-service.jfr,settings=profile"
```

#### GC Performance Monitor
```java
@Component
public class GCPerformanceMonitor {
    
    private final List<GarbageCollectorMXBean> gcBeans;
    private final MeterRegistry meterRegistry;
    
    @Scheduled(fixedRate = 30000)
    public void recordGCMetrics() {
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            String gcName = gcBean.getName().replace(" ", "_").toLowerCase();
            
            Gauge.builder("jvm.gc.collection.count")
                .tag("gc", gcName)
                .register(meterRegistry, gcBean, GarbageCollectorMXBean::getCollectionCount);
                
            Gauge.builder("jvm.gc.collection.time")
                .tag("gc", gcName)
                .register(meterRegistry, gcBean, GarbageCollectorMXBean::getCollectionTime);
        }
    }
}
```

## Data Models

### Performance Metrics Data Model
```java
@Entity
@Table(name = "performance_metrics")
public class PerformanceMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "metric_name", nullable = false)
    private String metricName;
    
    @Column(name = "metric_value", nullable = false)
    private Double metricValue;
    
    @Column(name = "metric_tags")
    private String metricTags; // JSON format
    
    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;
    
    // Getters and setters
}
```

### Cache Statistics Model
```java
public class CacheStatistics {
    private String cacheName;
    private long hitCount;
    private long missCount;
    private double hitRate;
    private long evictionCount;
    private double averageLoadTime;
    private long estimatedSize;
    
    // Getters and setters
}
```

## Error Handling

### Performance-Related Exception Hierarchy
```java
public class PerformanceException extends RuntimeException {
    public PerformanceException(String message) { super(message); }
    public PerformanceException(String message, Throwable cause) { super(message, cause); }
}

public class ConnectionPoolExhaustedException extends PerformanceException {
    public ConnectionPoolExhaustedException(String message) { super(message); }
}

public class TransactionTimeoutException extends PerformanceException {
    public TransactionTimeoutException(String message) { super(message); }
}

public class ExternalServiceTimeoutException extends PerformanceException {
    public ExternalServiceTimeoutException(String message, Throwable cause) { super(message, cause); }
}
```

### Circuit Breaker Fallback Strategies
```java
@Component
public class FallbackStrategies {
    
    public SecurityDTO getSecurityFallback(String securityId, Exception ex) {
        logger.warn("Security service fallback triggered for ID: {}", securityId, ex);
        return SecurityDTO.builder()
            .securityId(securityId)
            .ticker("UNKNOWN")
            .name("Service Unavailable")
            .build();
    }
    
    public PortfolioDTO getPortfolioFallback(String portfolioId, Exception ex) {
        logger.warn("Portfolio service fallback triggered for ID: {}", portfolioId, ex);
        return PortfolioDTO.builder()
            .portfolioId(portfolioId)
            .name("Service Unavailable")
            .build();
    }
}
```

## Testing Strategy

### Performance Testing Framework
```java
@Component
public class PerformanceTestHarness {
    
    public LoadTestResult executeLoadTest(LoadTestConfig config) {
        // Execute concurrent requests
        List<CompletableFuture<ResponseTime>> futures = IntStream.range(0, config.getConcurrentUsers())
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> executeRequest(config)))
            .collect(Collectors.toList());
        
        // Collect results
        List<ResponseTime> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
        return analyzeResults(results);
    }
    
    private LoadTestResult analyzeResults(List<ResponseTime> results) {
        DoubleSummaryStatistics stats = results.stream()
            .mapToDouble(ResponseTime::getDurationMs)
            .summaryStatistics();
        
        return LoadTestResult.builder()
            .averageResponseTime(stats.getAverage())
            .p95ResponseTime(calculatePercentile(results, 0.95))
            .p99ResponseTime(calculatePercentile(results, 0.99))
            .maxResponseTime(stats.getMax())
            .totalRequests(results.size())
            .build();
    }
}
```

### Database Performance Testing
```java
@Component
public class DatabasePerformanceTest {
    
    @Transactional
    public void testConnectionPoolUnderLoad() {
        // Simulate high concurrent database access
        List<CompletableFuture<Void>> futures = IntStream.range(0, 100)
            .mapToObj(i -> CompletableFuture.runAsync(() -> {
                // Execute database operations
                tradeOrderRepository.findAll(PageRequest.of(0, 10));
            }))
            .collect(Collectors.toList());
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
```

## Monitoring and Observability

### Performance Metrics Dashboard
```java
@RestController
@RequestMapping("/api/performance")
public class PerformanceMetricsController {
    
    @GetMapping("/metrics")
    public PerformanceMetricsResponse getPerformanceMetrics() {
        return PerformanceMetricsResponse.builder()
            .connectionPoolStats(getConnectionPoolStats())
            .cacheStats(getCacheStats())
            .gcStats(getGCStats())
            .responseTimeStats(getResponseTimeStats())
            .build();
    }
    
    @GetMapping("/health")
    public PerformanceHealthResponse getPerformanceHealth() {
        return PerformanceHealthResponse.builder()
            .connectionPoolHealthy(isConnectionPoolHealthy())
            .cacheHealthy(isCacheHealthy())
            .externalServicesHealthy(areExternalServicesHealthy())
            .overallHealth(calculateOverallHealth())
            .build();
    }
}
```

### Alerting Configuration
```yaml
alerts:
  - name: "High Response Time"
    condition: "p95_response_time > 1000ms"
    severity: "warning"
    
  - name: "Connection Pool Exhaustion"
    condition: "connection_pool_utilization > 90%"
    severity: "critical"
    
  - name: "Cache Hit Rate Low"
    condition: "cache_hit_rate < 80%"
    severity: "warning"
    
  - name: "External Service Circuit Breaker Open"
    condition: "circuit_breaker_state == 'OPEN'"
    severity: "critical"
```

This design provides a comprehensive approach to addressing the performance issues identified in the requirements, with a focus on reducing transaction scope, optimizing database connections, implementing asynchronous processing, and providing robust monitoring and observability.