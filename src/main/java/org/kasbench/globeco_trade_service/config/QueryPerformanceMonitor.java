package org.kasbench.globeco_trade_service.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Component to monitor database query performance and detect slow queries.
 * Simplified version that avoids circular dependencies with EntityManagerFactory.
 */
@Component
public class QueryPerformanceMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryPerformanceMonitor.class);
    
    // Threshold for slow query detection (1 second)
    private static final long SLOW_QUERY_THRESHOLD_MS = 1000;
    
    // Threshold for very slow query detection (5 seconds)
    private static final long VERY_SLOW_QUERY_THRESHOLD_MS = 5000;
    
    private final MeterRegistry meterRegistry;
    
    // Metrics
    private final Counter slowQueryCounter;
    private final Counter verySlowQueryCounter;
    private final Timer queryExecutionTimer;
    private final AtomicLong totalQueries = new AtomicLong(0);
    private final AtomicLong slowQueries = new AtomicLong(0);
    private final AtomicLong verySlowQueries = new AtomicLong(0);
    
    // Thread-local storage for query timing
    private final ThreadLocal<QueryExecutionContext> queryContext = new ThreadLocal<>();
    
    // Cache for query type classification
    private final ConcurrentHashMap<String, String> queryTypeCache = new ConcurrentHashMap<>();
    
    @Autowired
    public QueryPerformanceMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.slowQueryCounter = Counter.builder("database.queries.slow")
            .description("Number of slow database queries (>1s)")
            .register(meterRegistry);
            
        this.verySlowQueryCounter = Counter.builder("database.queries.very_slow")
            .description("Number of very slow database queries (>5s)")
            .register(meterRegistry);
            
        this.queryExecutionTimer = Timer.builder("database.query.execution.time")
            .description("Database query execution time")
            .register(meterRegistry);
    }
    
    /**
     * Start timing a query execution
     */
    public void startQueryTiming(String sql, String queryType) {
        QueryExecutionContext context = new QueryExecutionContext(sql, queryType, Instant.now());
        queryContext.set(context);
        totalQueries.incrementAndGet();
    }
    
    /**
     * End timing a query execution and record metrics
     */
    public void endQueryTiming() {
        QueryExecutionContext context = queryContext.get();
        if (context != null) {
            try {
                Duration duration = Duration.between(context.startTime, Instant.now());
                long durationMs = duration.toMillis();
                
                // Record execution time metric
                queryExecutionTimer.record(duration);
                
                // Check for slow queries
                if (durationMs >= VERY_SLOW_QUERY_THRESHOLD_MS) {
                    handleVerySlowQuery(context, durationMs);
                } else if (durationMs >= SLOW_QUERY_THRESHOLD_MS) {
                    handleSlowQuery(context, durationMs);
                }
                
                // Log query performance for debugging (only in debug mode)
                if (logger.isDebugEnabled()) {
                    logger.debug("Query executed in {}ms - Type: {}, SQL: {}", 
                        durationMs, context.queryType, truncateSQL(context.sql));
                }
                
            } finally {
                queryContext.remove();
            }
        }
    }
    
    /**
     * Handle slow query detection and logging
     */
    private void handleSlowQuery(QueryExecutionContext context, long durationMs) {
        slowQueries.incrementAndGet();
        slowQueryCounter.increment();
        
        String queryType = classifyQueryType(context.sql);
        
        logger.warn("Slow query detected: duration={}ms, type={}, sql={}", 
            durationMs, queryType, truncateSQL(context.sql));
        
        // Record slow query metric with tags
        Counter.builder("database.queries.slow.by_type")
            .tag("query_type", queryType)
            .tag("duration_bucket", "slow")
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * Handle very slow query detection and logging
     */
    private void handleVerySlowQuery(QueryExecutionContext context, long durationMs) {
        verySlowQueries.incrementAndGet();
        verySlowQueryCounter.increment();
        
        String queryType = classifyQueryType(context.sql);
        
        logger.error("Very slow query detected: duration={}ms, type={}, sql={}", 
            durationMs, queryType, truncateSQL(context.sql));
        
        // Record very slow query metric with tags
        Counter.builder("database.queries.slow.by_type")
            .tag("query_type", queryType)
            .tag("duration_bucket", "very_slow")
            .register(meterRegistry)
            .increment();
        
        // Additional alerting could be implemented here (e.g., send to monitoring system)
        triggerSlowQueryAlert(context, durationMs, queryType);
    }
    
    /**
     * Classify query type based on SQL content
     */
    private String classifyQueryType(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return "unknown";
        }
        
        // Use cache to avoid repeated classification
        return queryTypeCache.computeIfAbsent(sql, this::doClassifyQueryType);
    }
    
    /**
     * Perform actual query type classification
     */
    private String doClassifyQueryType(String sql) {
        String normalizedSql = sql.trim().toLowerCase();
        
        if (normalizedSql.startsWith("select")) {
            if (normalizedSql.contains("join")) {
                return "select_join";
            } else if (normalizedSql.contains("where")) {
                return "select_filtered";
            } else {
                return "select_simple";
            }
        } else if (normalizedSql.startsWith("insert")) {
            return "insert";
        } else if (normalizedSql.startsWith("update")) {
            return "update";
        } else if (normalizedSql.startsWith("delete")) {
            return "delete";
        } else {
            return "other";
        }
    }
    
    /**
     * Truncate SQL for logging to avoid excessive log sizes
     */
    private String truncateSQL(String sql) {
        if (sql == null) return "null";
        if (sql.length() <= 200) return sql;
        return sql.substring(0, 200) + "...";
    }
    
    /**
     * Trigger alert for very slow queries
     */
    private void triggerSlowQueryAlert(QueryExecutionContext context, long durationMs, String queryType) {
        // This could be extended to integrate with alerting systems like PagerDuty, Slack, etc.
        logger.error("ALERT: Very slow query detected - Duration: {}ms, Type: {}, " +
            "Consider investigating query execution plan and adding appropriate indexes", 
            durationMs, queryType);
    }
    
    /**
     * Get current query performance statistics
     */
    public QueryPerformanceStats getPerformanceStats() {
        long total = totalQueries.get();
        long slow = slowQueries.get();
        long verySlow = verySlowQueries.get();
        
        double slowPercentage = total > 0 ? (slow * 100.0) / total : 0.0;
        double verySlowPercentage = total > 0 ? (verySlow * 100.0) / total : 0.0;
        
        return new QueryPerformanceStats(total, slow, verySlow, slowPercentage, verySlowPercentage);
    }
    
    /**
     * Reset performance statistics (useful for testing)
     */
    public void resetStats() {
        totalQueries.set(0);
        slowQueries.set(0);
        verySlowQueries.set(0);
        queryTypeCache.clear();
    }
    

    
    /**
     * Context object to store query execution information
     */
    private static class QueryExecutionContext {
        final String sql;
        final String queryType;
        final Instant startTime;
        
        QueryExecutionContext(String sql, String queryType, Instant startTime) {
            this.sql = sql;
            this.queryType = queryType;
            this.startTime = startTime;
        }
    }
    
    /**
     * Statistics object for query performance
     */
    public static class QueryPerformanceStats {
        private final long totalQueries;
        private final long slowQueries;
        private final long verySlowQueries;
        private final double slowPercentage;
        private final double verySlowPercentage;
        
        public QueryPerformanceStats(long totalQueries, long slowQueries, long verySlowQueries, 
                                   double slowPercentage, double verySlowPercentage) {
            this.totalQueries = totalQueries;
            this.slowQueries = slowQueries;
            this.verySlowQueries = verySlowQueries;
            this.slowPercentage = slowPercentage;
            this.verySlowPercentage = verySlowPercentage;
        }
        
        // Getters
        public long getTotalQueries() { return totalQueries; }
        public long getSlowQueries() { return slowQueries; }
        public long getVerySlowQueries() { return verySlowQueries; }
        public double getSlowPercentage() { return slowPercentage; }
        public double getVerySlowPercentage() { return verySlowPercentage; }
        
        @Override
        public String toString() {
            return String.format("QueryPerformanceStats{total=%d, slow=%d (%.2f%%), verySlow=%d (%.2f%%)}", 
                totalQueries, slowQueries, slowPercentage, verySlowQueries, verySlowPercentage);
        }
    }
}