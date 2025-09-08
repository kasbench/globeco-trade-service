package org.kasbench.globeco_trade_service.controller;

import org.kasbench.globeco_trade_service.config.QueryPerformanceMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller to expose query performance monitoring endpoints.
 * Provides access to query performance statistics and management operations.
 */
@RestController
@RequestMapping("/api/performance/queries")
public class QueryPerformanceController {
    
    @Autowired
    private QueryPerformanceMonitor queryPerformanceMonitor;
    
    /**
     * Get current query performance statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<QueryPerformanceMonitor.QueryPerformanceStats> getQueryStats() {
        QueryPerformanceMonitor.QueryPerformanceStats stats = queryPerformanceMonitor.getPerformanceStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Reset query performance statistics
     */
    @PostMapping("/reset")
    public ResponseEntity<String> resetQueryStats() {
        queryPerformanceMonitor.resetStats();
        return ResponseEntity.ok("Query performance statistics reset successfully");
    }
    
    /**
     * Health check endpoint for query performance monitoring
     */
    @GetMapping("/health")
    public ResponseEntity<QueryPerformanceHealth> getQueryPerformanceHealth() {
        QueryPerformanceMonitor.QueryPerformanceStats stats = queryPerformanceMonitor.getPerformanceStats();
        
        // Consider system healthy if less than 5% of queries are slow
        boolean healthy = stats.getSlowPercentage() < 5.0;
        String status = healthy ? "HEALTHY" : "DEGRADED";
        
        // Consider critical if more than 1% of queries are very slow
        if (stats.getVerySlowPercentage() > 1.0) {
            status = "CRITICAL";
            healthy = false;
        }
        
        QueryPerformanceHealth health = new QueryPerformanceHealth(
            status, 
            healthy, 
            stats.getSlowPercentage(), 
            stats.getVerySlowPercentage(),
            stats.getTotalQueries()
        );
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Query performance health response object
     */
    public static class QueryPerformanceHealth {
        private final String status;
        private final boolean healthy;
        private final double slowQueryPercentage;
        private final double verySlowQueryPercentage;
        private final long totalQueries;
        
        public QueryPerformanceHealth(String status, boolean healthy, double slowQueryPercentage, 
                                    double verySlowQueryPercentage, long totalQueries) {
            this.status = status;
            this.healthy = healthy;
            this.slowQueryPercentage = slowQueryPercentage;
            this.verySlowQueryPercentage = verySlowQueryPercentage;
            this.totalQueries = totalQueries;
        }
        
        // Getters
        public String getStatus() { return status; }
        public boolean isHealthy() { return healthy; }
        public double getSlowQueryPercentage() { return slowQueryPercentage; }
        public double getVerySlowQueryPercentage() { return verySlowQueryPercentage; }
        public long getTotalQueries() { return totalQueries; }
    }
}