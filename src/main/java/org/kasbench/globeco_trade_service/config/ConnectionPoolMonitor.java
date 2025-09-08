package org.kasbench.globeco_trade_service.config;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Connection Pool Monitor for HikariCP
 * Provides metrics collection and health checks for database connection pool
 */
@Component
public class ConnectionPoolMonitor implements HealthIndicator {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionPoolMonitor.class);
    
    private final HikariDataSource dataSource;
    private final MeterRegistry meterRegistry;
    
    // Health check thresholds
    private static final double CRITICAL_UTILIZATION_THRESHOLD = 0.90; // 90%
    private static final double WARNING_UTILIZATION_THRESHOLD = 0.80;  // 80%
    
    @Autowired
    public ConnectionPoolMonitor(DataSource dataSource, MeterRegistry meterRegistry) {
        if (!(dataSource instanceof HikariDataSource)) {
            throw new IllegalArgumentException("DataSource must be HikariDataSource for monitoring");
        }
        this.dataSource = (HikariDataSource) dataSource;
        this.meterRegistry = meterRegistry;
        
        // Register gauges for connection pool metrics
        registerConnectionPoolGauges();
    }
    
    /**
     * Register Micrometer gauges for connection pool metrics
     */
    private void registerConnectionPoolGauges() {
        HikariPoolMXBean poolBean = dataSource.getHikariPoolMXBean();
        
        if (poolBean != null) {
            Gauge.builder("hikari.connections.active", poolBean, bean -> (double) bean.getActiveConnections())
                .description("Active connections in the pool")
                .register(meterRegistry);
                
            Gauge.builder("hikari.connections.idle", poolBean, bean -> (double) bean.getIdleConnections())
                .description("Idle connections in the pool")
                .register(meterRegistry);
                
            Gauge.builder("hikari.connections.total", poolBean, bean -> (double) bean.getTotalConnections())
                .description("Total connections in the pool")
                .register(meterRegistry);
                
            Gauge.builder("hikari.connections.pending", poolBean, bean -> (double) bean.getThreadsAwaitingConnection())
                .description("Threads awaiting connections")
                .register(meterRegistry);
                
            // Custom utilization percentage gauge
            Gauge.builder("hikari.connections.utilization", this, ConnectionPoolMonitor::getConnectionUtilization)
                .description("Connection pool utilization percentage")
                .register(meterRegistry);
        }
    }
    
    /**
     * Scheduled task to record connection pool metrics and check for alerts
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void recordConnectionPoolMetrics() {
        try {
            HikariPoolMXBean poolBean = dataSource.getHikariPoolMXBean();
            if (poolBean == null) {
                logger.warn("HikariPoolMXBean is not available for metrics collection");
                return;
            }
            
            int activeConnections = poolBean.getActiveConnections();
            int idleConnections = poolBean.getIdleConnections();
            int totalConnections = poolBean.getTotalConnections();
            int threadsAwaiting = poolBean.getThreadsAwaitingConnection();
            double utilization = getConnectionUtilization();
            
            // Log metrics for debugging
            logger.debug("Connection Pool Metrics - Active: {}, Idle: {}, Total: {}, Awaiting: {}, Utilization: {:.2f}%",
                activeConnections, idleConnections, totalConnections, threadsAwaiting, utilization * 100);
            
            // Check for alert conditions
            checkAlertThresholds(utilization, threadsAwaiting);
            
        } catch (Exception e) {
            logger.error("Error recording connection pool metrics", e);
        }
    }
    
    /**
     * Check alert thresholds and log warnings/errors
     */
    private void checkAlertThresholds(double utilization, int threadsAwaiting) {
        if (utilization >= CRITICAL_UTILIZATION_THRESHOLD) {
            logger.error("CRITICAL: Connection pool utilization is {}% (threshold: {}%)", 
                String.format("%.2f", utilization * 100), String.format("%.2f", CRITICAL_UTILIZATION_THRESHOLD * 100));
        } else if (utilization >= WARNING_UTILIZATION_THRESHOLD) {
            logger.warn("WARNING: Connection pool utilization is {}% (threshold: {}%)", 
                String.format("%.2f", utilization * 100), String.format("%.2f", WARNING_UTILIZATION_THRESHOLD * 100));
        }
        
        if (threadsAwaiting > 0) {
            logger.warn("WARNING: {} threads are waiting for database connections", threadsAwaiting);
        }
    }
    
    /**
     * Calculate connection pool utilization percentage
     */
    private double getConnectionUtilization() {
        HikariPoolMXBean poolBean = dataSource.getHikariPoolMXBean();
        if (poolBean == null) {
            return 0.0;
        }
        
        int totalConnections = poolBean.getTotalConnections();
        int activeConnections = poolBean.getActiveConnections();
        
        if (totalConnections == 0) {
            return 0.0;
        }
        
        return (double) activeConnections / totalConnections;
    }
    
    /**
     * Health check implementation for Spring Boot Actuator
     */
    @Override
    public Health health() {
        try {
            HikariPoolMXBean poolBean = dataSource.getHikariPoolMXBean();
            if (poolBean == null) {
                return Health.down()
                    .withDetail("reason", "HikariPoolMXBean not available")
                    .build();
            }
            
            int activeConnections = poolBean.getActiveConnections();
            int idleConnections = poolBean.getIdleConnections();
            int totalConnections = poolBean.getTotalConnections();
            int threadsAwaiting = poolBean.getThreadsAwaitingConnection();
            double utilization = getConnectionUtilization();
            
            Health.Builder healthBuilder = Health.up()
                .withDetail("activeConnections", activeConnections)
                .withDetail("idleConnections", idleConnections)
                .withDetail("totalConnections", totalConnections)
                .withDetail("threadsAwaitingConnection", threadsAwaiting)
                .withDetail("utilizationPercentage", String.format("%.2f%%", utilization * 100))
                .withDetail("maxPoolSize", dataSource.getMaximumPoolSize())
                .withDetail("minIdle", dataSource.getMinimumIdle());
            
            // Determine health status based on utilization and waiting threads
            if (utilization >= CRITICAL_UTILIZATION_THRESHOLD || threadsAwaiting > 10) {
                return healthBuilder
                    .down()
                    .withDetail("reason", "Connection pool under severe stress")
                    .build();
            } else if (utilization >= WARNING_UTILIZATION_THRESHOLD || threadsAwaiting > 0) {
                return healthBuilder
                    .unknown()
                    .withDetail("reason", "Connection pool utilization high")
                    .build();
            }
            
            return healthBuilder.build();
            
        } catch (Exception e) {
            logger.error("Error checking connection pool health", e);
            return Health.down()
                .withDetail("reason", "Error checking connection pool health")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}