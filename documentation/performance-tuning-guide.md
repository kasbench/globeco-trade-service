# Globeco Trade Service: Performance Tuning Guide

## Overview

This guide provides comprehensive performance optimization strategies for the Globeco Trade Service v2 API, focusing on caching, database optimization, external service integration, and JVM tuning.

## Performance Baseline

### Target Performance Metrics
- **API Response Time**: 95th percentile < 500ms for typical queries
- **Throughput**: Support 1000+ concurrent requests
- **Cache Hit Rate**: > 80% for external service data
- **Database Connection Pool**: < 80% utilization under normal load
- **Memory Usage**: < 70% of allocated heap under normal load

### Current Performance Characteristics
- v1 endpoints: Average 200ms response time
- v2 endpoints: Average 300ms response time (due to external service enrichment)
- Database queries: Average 50ms for typical operations
- External service calls: Average 100ms per call

## JVM Optimization

### Heap Configuration

#### Production Settings
```bash
# Recommended JVM flags for production
JAVA_OPTS="-server \
           -Xms4g \
           -Xmx8g \
           -XX:NewRatio=3 \
           -XX:SurvivorRatio=8 \
           -XX:MaxMetaspaceSize=512m"
```

#### Garbage Collection Tuning

##### G1GC (Recommended for most workloads)
```bash
# G1 Garbage Collector (best for low latency)
GC_OPTS="-XX:+UseG1GC \
         -XX:MaxGCPauseMillis=200 \
         -XX:G1HeapRegionSize=16m \
         -XX:+G1UseAdaptiveIHOP \
         -XX:G1MixedGCCountTarget=8 \
         -XX:G1MixedGCLiveThresholdPercent=85"
```

##### ZGC (For ultra-low latency requirements)
```bash
# ZGC for applications requiring < 10ms pause times
GC_OPTS="-XX:+UseZGC \
         -XX:+UnlockExperimentalVMOptions"
```

#### Memory Management
```bash
# Additional memory optimizations
MEMORY_OPTS="-XX:+UseStringDeduplication \
             -XX:+UseCompressedOops \
             -XX:+UseCompressedClassPointers \
             -XX:+OptimizeStringConcat"
```

### JVM Monitoring and Profiling

#### JFR (Java Flight Recorder)
```bash
# Enable JFR for production monitoring
JFR_OPTS="-XX:+FlightRecorder \
          -XX:StartFlightRecording=duration=60s,filename=trade-service.jfr,settings=profile"
```

#### JIT Compiler Optimization
```bash
# JIT compilation optimizations
JIT_OPTS="-XX:+UseCodeCacheFlushing \
          -XX:ReservedCodeCacheSize=256m \
          -XX:InitialCodeCacheSize=64m"
```

## Database Optimization

### Connection Pool Tuning

#### HikariCP Configuration
```yaml
spring:
  datasource:
    hikari:
      # Core pool settings
      maximum-pool-size: 30        # Max connections (adjust based on load)
      minimum-idle: 10             # Min idle connections
      connection-timeout: 30000    # 30 seconds
      idle-timeout: 600000         # 10 minutes
      max-lifetime: 1800000        # 30 minutes
      
      # Performance optimizations
      leak-detection-threshold: 60000  # 1 minute
      validation-timeout: 5000         # 5 seconds
      pool-name: "TradeServiceCP"
      
      # Connection properties
      connection-test-query: "SELECT 1"
      connection-init-sql: "SET TIME ZONE 'UTC'"
      
      # Datasource properties
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true
        useLocalSessionState: true
        rewriteBatchedStatements: true
        cacheResultSetMetadata: true
        cacheServerConfiguration: true
        elideSetAutoCommits: true
        maintainTimeStats: false
```

### Query Optimization

#### JPA/Hibernate Configuration
```yaml
spring:
  jpa:
    properties:
      hibernate:
        # Query optimization
        jdbc:
          batch_size: 25           # Batch inserts/updates
          fetch_size: 50           # JDBC fetch size
        
        # Statement caching
        query:
          plan_cache_max_size: 2048
          plan_parameter_metadata_max_size: 128
        
        # Connection handling
        connection:
          provider_disables_autocommit: true
          
        # Query execution
        order_inserts: true
        order_updates: true
        generate_statistics: false  # Disable in production
        
        # Second-level cache (if needed)
        cache:
          use_second_level_cache: false
          use_query_cache: false
```

### Database Indexes

#### Performance-Critical Indexes
```sql
-- Composite indexes for common filter combinations
CREATE INDEX CONCURRENTLY idx_trade_orders_portfolio_security 
ON trade_orders (portfolio_id, security_id);

CREATE INDEX CONCURRENTLY idx_trade_orders_order_type_submitted 
ON trade_orders (order_type, submitted);

CREATE INDEX CONCURRENTLY idx_trade_orders_quantity_range 
ON trade_orders (quantity) WHERE quantity > 0;

-- Covering indexes for common queries
CREATE INDEX CONCURRENTLY idx_trade_orders_filtering 
ON trade_orders (portfolio_id, security_id, order_type, submitted) 
INCLUDE (id, order_id, quantity, quantity_sent);

-- Time-based indexes for recent data queries
CREATE INDEX CONCURRENTLY idx_trade_orders_created_at 
ON trade_orders (created_at DESC) WHERE created_at > NOW() - INTERVAL '30 days';

-- Partial indexes for frequently filtered data
CREATE INDEX CONCURRENTLY idx_trade_orders_submitted_true 
ON trade_orders (portfolio_id, security_id) WHERE submitted = true;

-- Execution table indexes
CREATE INDEX CONCURRENTLY idx_executions_order_id_quantity 
ON executions (order_id, quantity DESC);

CREATE INDEX CONCURRENTLY idx_executions_price_range 
ON executions (price) WHERE price > 0;
```

#### Index Maintenance
```sql
-- Regular index maintenance (run during off-peak hours)
REINDEX INDEX CONCURRENTLY idx_trade_orders_portfolio_security;
ANALYZE trade_orders;
ANALYZE executions;

-- Monitor index usage
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes 
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;
```

## Caching Optimization

### Caffeine Cache Configuration

#### Fine-tuned Cache Settings
```java
@Configuration
public class CacheConfig {
    
    @Bean("securityCache")
    public Cache<String, SecurityDTO> securityCache() {
        return Caffeine.newBuilder()
            .maximumSize(2000)                          // Increased size
            .expireAfterWrite(Duration.ofMinutes(10))   // Extended TTL
            .refreshAfterWrite(Duration.ofMinutes(8))   // Proactive refresh
            .recordStats()                              // Enable metrics
            .removalListener(this::onSecurityCacheRemoval)
            .buildAsync(this::loadSecurity)
            .synchronous();
    }
    
    @Bean("portfolioCache")
    public Cache<String, PortfolioDTO> portfolioCache() {
        return Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(15))   // Portfolios change less frequently
            .refreshAfterWrite(Duration.ofMinutes(12))
            .recordStats()
            .removalListener(this::onPortfolioCacheRemoval)
            .buildAsync(this::loadPortfolio)
            .synchronous();
    }
    
    // Cache warming on startup
    @EventListener(ApplicationReadyEvent.class)
    public void warmupCaches() {
        CompletableFuture.allOf(
            CompletableFuture.runAsync(this::warmupSecurityCache),
            CompletableFuture.runAsync(this::warmupPortfolioCache)
        );
    }
    
    private void warmupSecurityCache() {
        // Pre-load frequently accessed securities
        List<String> frequentTickers = Arrays.asList("AAPL", "MSFT", "GOOGL", "AMZN", "TSLA");
        frequentTickers.parallelStream().forEach(ticker -> {
            try {
                securityCache().get(ticker);
            } catch (Exception e) {
                log.warn("Failed to warm up security cache for ticker: {}", ticker, e);
            }
        });
    }
}
```

### Cache Monitoring and Optimization

#### Cache Metrics Collection
```java
@Component
public class CacheMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    private final SecurityCacheService securityCacheService;
    private final PortfolioCacheService portfolioCacheService;
    
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void collectCacheMetrics() {
        // Security cache metrics
        CacheStats securityStats = securityCacheService.getStats();
        recordCacheMetrics("security", securityStats);
        
        // Portfolio cache metrics
        CacheStats portfolioStats = portfolioCacheService.getStats();
        recordCacheMetrics("portfolio", portfolioStats);
    }
    
    private void recordCacheMetrics(String cacheName, CacheStats stats) {
        Gauge.builder("cache.hit.rate")
            .tag("cache", cacheName)
            .register(meterRegistry, stats, CacheStats::hitRate);
            
        Gauge.builder("cache.miss.rate")
            .tag("cache", cacheName)
            .register(meterRegistry, stats, CacheStats::missRate);
            
        Gauge.builder("cache.eviction.count")
            .tag("cache", cacheName)
            .register(meterRegistry, stats, CacheStats::evictionCount);
            
        Gauge.builder("cache.load.average.time")
            .tag("cache", cacheName)
            .register(meterRegistry, stats, s -> s.averageLoadPenalty() / 1_000_000); // Convert to ms
    }
}
```

#### Cache Performance Analysis
```java
@Component
public class CachePerformanceAnalyzer {
    
    @EventListener
    public void handleCacheLoadEvent(CacheLoadEvent event) {
        if (event.getLoadTime() > Duration.ofMillis(500)) {
            log.warn("Slow cache load detected: cache={}, key={}, loadTime={}ms", 
                event.getCacheName(), event.getKey(), event.getLoadTime().toMillis());
        }
    }
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void analyzeCachePerformance() {
        // Analyze hit rates and recommend optimizations
        analyzeSecurityCachePerformance();
        analyzePortfolioCachePerformance();
    }
    
    private void analyzeSecurityCachePerformance() {
        CacheStats stats = securityCacheService.getStats();
        double hitRate = stats.hitRate();
        
        if (hitRate < 0.8) {
            log.warn("Security cache hit rate below threshold: {}%. Consider increasing TTL or cache size.", 
                hitRate * 100);
            
            // Analyze miss patterns
            Map<String, Long> missPatterns = securityCacheService.getMissPatterns();
            log.info("Top cache misses: {}", missPatterns);
        }
    }
}
```

## External Service Optimization

### HTTP Client Configuration

#### RestTemplate Optimization
```java
@Configuration
public class HttpClientConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        // Use Apache HTTP Client for better performance
        HttpComponentsClientHttpRequestFactory factory = 
            new HttpComponentsClientHttpRequestFactory();
        
        factory.setHttpClient(httpClient());
        factory.setConnectTimeout(5000);       // 5 seconds
        factory.setReadTimeout(10000);         // 10 seconds
        
        RestTemplate restTemplate = new RestTemplate(factory);
        
        // Add performance interceptors
        restTemplate.getInterceptors().add(new TimingInterceptor());
        restTemplate.getInterceptors().add(new RetryInterceptor());
        
        return restTemplate;
    }
    
    @Bean
    public HttpClient httpClient() {
        return HttpClients.custom()
            .setMaxConnTotal(200)                    // Total connection pool size
            .setMaxConnPerRoute(50)                  // Connections per route
            .setConnectionTimeToLive(30, TimeUnit.SECONDS)
            .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())
            .setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
            .evictIdleConnections(30, TimeUnit.SECONDS)
            .build();
    }
}
```

### Circuit Breaker Pattern

#### Resilience4j Configuration
```java
@Configuration
public class CircuitBreakerConfig {
    
    @Bean
    public CircuitBreaker securityServiceCircuitBreaker() {
        return CircuitBreaker.ofDefaults("securityService")
            .toBuilder()
            .failureRateThreshold(50)                    // 50% failure rate
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
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
            .build();
    }
}
```

### Parallel External Service Calls

#### Batch Processing Optimization
```java
@Service
public class OptimizedTradeOrderService {
    
    private final SecurityCacheService securityCacheService;
    private final PortfolioCacheService portfolioCacheService;
    private final Executor asyncExecutor;
    
    public Page<EnhancedTradeOrderResponseDTO> getTradeOrdersOptimized(
            TradeOrderFilter filter, Pageable pageable) {
        
        // Get base data from database
        Page<TradeOrder> tradeOrders = tradeOrderRepository.findAll(
            TradeOrderSpecification.build(filter), pageable);
        
        // Extract unique IDs for batch processing
        Set<String> securityIds = extractSecurityIds(tradeOrders.getContent());
        Set<String> portfolioIds = extractPortfolioIds(tradeOrders.getContent());
        
        // Parallel external service calls
        CompletableFuture<Map<String, SecurityDTO>> securityFuture = 
            CompletableFuture.supplyAsync(() -> 
                batchLoadSecurities(securityIds), asyncExecutor);
                
        CompletableFuture<Map<String, PortfolioDTO>> portfolioFuture = 
            CompletableFuture.supplyAsync(() -> 
                batchLoadPortfolios(portfolioIds), asyncExecutor);
        
        // Wait for completion and build response
        try {
            Map<String, SecurityDTO> securities = securityFuture.get(5, TimeUnit.SECONDS);
            Map<String, PortfolioDTO> portfolios = portfolioFuture.get(5, TimeUnit.SECONDS);
            
            List<EnhancedTradeOrderResponseDTO> enhancedOrders = 
                enhanceTradeOrders(tradeOrders.getContent(), securities, portfolios);
            
            return new PageImpl<>(enhancedOrders, pageable, tradeOrders.getTotalElements());
            
        } catch (TimeoutException e) {
            log.warn("External service timeout, falling back to basic response");
            return buildBasicResponse(tradeOrders);
        }
    }
    
    private Map<String, SecurityDTO> batchLoadSecurities(Set<String> securityIds) {
        return securityIds.parallelStream()
            .collect(Collectors.toConcurrentMap(
                id -> id,
                id -> securityCacheService.getSecurityById(id)
            ));
    }
}
```

## API Performance Optimization

### Response Compression

#### GZIP Configuration
```yaml
server:
  compression:
    enabled: true
    mime-types: 
      - application/json
      - application/xml
      - text/html
      - text/xml
      - text/plain
    min-response-size: 1024    # Compress responses > 1KB
```

### Pagination Optimization

#### Efficient Cursor-based Pagination
```java
@Service
public class PaginationOptimizationService {
    
    // Use cursor-based pagination for large datasets
    public CursorPage<TradeOrder> getTradeOrdersCursor(
            String cursor, int limit, TradeOrderFilter filter) {
        
        Specification<TradeOrder> spec = TradeOrderSpecification.build(filter);
        
        if (cursor != null) {
            // Add cursor condition for efficient pagination
            Long cursorId = decodeCursor(cursor);
            spec = spec.and((root, query, cb) -> 
                cb.greaterThan(root.get("id"), cursorId));
        }
        
        Pageable pageable = PageRequest.of(0, limit + 1, Sort.by("id"));
        List<TradeOrder> orders = tradeOrderRepository.findAll(spec, pageable).getContent();
        
        boolean hasNext = orders.size() > limit;
        if (hasNext) {
            orders = orders.subList(0, limit);
        }
        
        String nextCursor = hasNext ? encodeCursor(orders.get(limit - 1).getId()) : null;
        return new CursorPage<>(orders, nextCursor, hasNext);
    }
}
```

### Query Result Streaming

#### Streaming Large Result Sets
```java
@RestController
public class StreamingTradeOrderController {
    
    @GetMapping(value = "/api/v2/tradeOrders/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity<StreamingResponseBody> streamTradeOrders(
            TradeOrderFilter filter) {
        
        StreamingResponseBody stream = outputStream -> {
            try (PrintWriter writer = new PrintWriter(outputStream)) {
                tradeOrderRepository.streamByFilter(filter)
                    .map(this::enhanceTradeOrder)
                    .forEach(order -> {
                        writer.println(objectMapper.writeValueAsString(order));
                        writer.flush();
                    });
            }
        };
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_NDJSON_VALUE)
            .body(stream);
    }
}
```

## Monitoring and Profiling

### Performance Metrics

#### Custom Performance Metrics
```java
@Component
public class PerformanceMetrics {
    
    private final Timer apiResponseTimer;
    private final Counter slowQueryCounter;
    private final Gauge activeConnectionsGauge;
    
    public PerformanceMetrics(MeterRegistry registry) {
        this.apiResponseTimer = Timer.builder("api.response.time")
            .description("API response time")
            .register(registry);
            
        this.slowQueryCounter = Counter.builder("database.slow.queries")
            .description("Count of slow database queries")
            .register(registry);
            
        this.activeConnectionsGauge = Gauge.builder("database.connections.active")
            .description("Active database connections")
            .register(registry, this, PerformanceMetrics::getActiveConnections);
    }
    
    @EventListener
    public void handleSlowQuery(SlowQueryEvent event) {
        if (event.getDuration().toMillis() > 1000) {
            slowQueryCounter.increment(
                Tags.of(
                    "query", event.getQueryType(),
                    "duration", String.valueOf(event.getDuration().toMillis())
                )
            );
        }
    }
}
```

### Load Testing

#### JMeter Test Plan Configuration
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan testname="Trade Service Load Test">
      <elementProp name="TestPlan.arguments" elementType="Arguments" guiclass="ArgumentsPanel">
        <collectionProp name="Arguments.arguments">
          <elementProp name="BASE_URL" elementType="Argument">
            <stringProp name="Argument.name">BASE_URL</stringProp>
            <stringProp name="Argument.value">http://localhost:8080</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </TestPlan>
    
    <hashTree>
      <ThreadGroup testname="API Load Test">
        <stringProp name="ThreadGroup.num_threads">100</stringProp>
        <stringProp name="ThreadGroup.ramp_time">60</stringProp>
        <stringProp name="ThreadGroup.duration">300</stringProp>
        <boolProp name="ThreadGroup.scheduler">true</boolProp>
      </ThreadGroup>
      
      <hashTree>
        <HTTPSamplerProxy testname="Get Trade Orders v2">
          <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
            <collectionProp name="Arguments.arguments">
              <elementProp name="page" elementType="HTTPArgument">
                <stringProp name="Argument.value">0</stringProp>
              </elementProp>
              <elementProp name="size" elementType="HTTPArgument">
                <stringProp name="Argument.value">20</stringProp>
              </elementProp>
            </collectionProp>
          </elementProp>
          <stringProp name="HTTPSampler.domain">${BASE_URL}</stringProp>
          <stringProp name="HTTPSampler.path">/api/v2/tradeOrders</stringProp>
          <stringProp name="HTTPSampler.method">GET</stringProp>
        </HTTPSamplerProxy>
      </hashTree>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

## Performance Troubleshooting

### Common Performance Issues

#### 1. High Memory Usage
**Symptoms:**
- OutOfMemoryError exceptions
- High GC frequency
- Slow response times

**Investigation:**
```bash
# Heap dump analysis
jcmd <pid> GC.run_finalization
jcmd <pid> VM.gc_heap_info
jmap -dump:format=b,file=heapdump.hprof <pid>

# GC analysis
-XX:+PrintGC -XX:+PrintGCDetails -XX:+PrintGCTimeStamps
```

**Solutions:**
- Increase heap size: `-Xmx8g`
- Tune GC: Switch to G1GC or ZGC
- Optimize cache sizes
- Fix memory leaks in external service clients

#### 2. Database Performance Issues
**Symptoms:**
- High database CPU usage
- Long-running queries
- Connection pool exhaustion

**Investigation:**
```sql
-- Monitor active queries
SELECT 
    pid,
    now() - pg_stat_activity.query_start AS duration,
    query 
FROM pg_stat_activity 
WHERE (now() - pg_stat_activity.query_start) > interval '5 minutes';

-- Check index usage
SELECT 
    tablename,
    indexname,
    idx_scan,
    idx_tup_read
FROM pg_stat_user_indexes 
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;
```

**Solutions:**
- Add missing indexes
- Optimize query plans
- Increase connection pool size
- Use read replicas for read-heavy operations

#### 3. External Service Latency
**Symptoms:**
- High API response times
- Frequent timeouts
- Low cache hit rates

**Investigation:**
```bash
# Monitor external service response times
curl -w "@curl-format.txt" -o /dev/null -s "http://security-service:8000/health"

# Check circuit breaker status
curl http://localhost:8080/actuator/circuitbreakers
```

**Solutions:**
- Increase timeout values
- Implement circuit breakers
- Optimize cache configuration
- Use parallel processing for batch calls

### Performance Monitoring Dashboard

#### Key Performance Indicators (KPIs)
```yaml
# Grafana Dashboard Query Examples
queries:
  - name: "API Response Time P95"
    query: 'histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))'
    
  - name: "Cache Hit Rate"
    query: 'avg(cache_hit_rate) by (cache)'
    
  - name: "Database Connection Pool Usage"
    query: '(hikaricp_connections_active / hikaricp_connections_max) * 100'
    
  - name: "External Service Call Rate"
    query: 'rate(external_service_calls_total[5m])'
    
  - name: "JVM Memory Usage"
    query: '(jvm_memory_used_bytes / jvm_memory_max_bytes) * 100'
```

## Capacity Planning

### Resource Requirements

#### Production Environment Sizing
```yaml
# Kubernetes resource allocation
resources:
  requests:
    memory: "4Gi"
    cpu: "2"
  limits:
    memory: "8Gi"
    cpu: "4"

# Database sizing
database:
  connections: 30
  memory: "16Gi"
  cpu: "8"
  storage: "500Gi"

# Cache sizing
cache:
  security_cache_size: 2000
  portfolio_cache_size: 1000
  total_memory_overhead: "1Gi"
```

#### Scaling Thresholds
```yaml
# Horizontal Pod Autoscaler
scaling:
  min_replicas: 3
  max_replicas: 20
  target_cpu_utilization: 70
  target_memory_utilization: 80
  
  scale_up_policies:
    - type: "Percent"
      value: 100
      period_seconds: 60
      
  scale_down_policies:
    - type: "Percent"
      value: 10
      period_seconds: 300
```

## Optimization Checklist

### Pre-Production Checklist
- [ ] JVM heap size optimized for workload
- [ ] Garbage collector tuned for latency requirements
- [ ] Database connection pool sized correctly
- [ ] Cache hit rates > 80% for external services
- [ ] Database indexes created for common queries
- [ ] Circuit breakers configured for external services
- [ ] Performance monitoring dashboards set up
- [ ] Load testing completed with expected traffic
- [ ] Memory leak testing completed
- [ ] Database query performance validated

### Post-Deployment Monitoring
- [ ] API response times within SLA
- [ ] Database performance stable
- [ ] Cache performance optimal
- [ ] External service integration healthy
- [ ] JVM metrics normal
- [ ] No memory leaks detected
- [ ] Scaling policies effective
- [ ] Alert thresholds appropriate 