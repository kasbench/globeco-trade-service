package org.kasbench.globeco_trade_service.config;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ConnectionPoolMonitorTest {

    @Mock
    private HikariDataSource dataSource;
    
    @Mock
    private HikariPoolMXBean poolMXBean;
    
    private MeterRegistry meterRegistry;
    private ConnectionPoolMonitor connectionPoolMonitor;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    void testHealthCheckHealthyPool() {
        // Arrange
        when(dataSource.getHikariPoolMXBean()).thenReturn(poolMXBean);
        when(dataSource.getMaximumPoolSize()).thenReturn(100);
        when(dataSource.getMinimumIdle()).thenReturn(25);
        when(poolMXBean.getActiveConnections()).thenReturn(30);
        when(poolMXBean.getIdleConnections()).thenReturn(20);
        when(poolMXBean.getTotalConnections()).thenReturn(50);
        when(poolMXBean.getThreadsAwaitingConnection()).thenReturn(0);
        
        connectionPoolMonitor = new ConnectionPoolMonitor(dataSource, meterRegistry);

        // Act
        Health health = connectionPoolMonitor.health();

        // Assert
        assertEquals(Status.UP, health.getStatus());
        assertEquals(30, health.getDetails().get("activeConnections"));
        assertEquals(20, health.getDetails().get("idleConnections"));
        assertEquals(50, health.getDetails().get("totalConnections"));
        assertEquals(0, health.getDetails().get("threadsAwaitingConnection"));
        assertEquals("60.00%", health.getDetails().get("utilizationPercentage"));
    }

    @Test
    void testHealthCheckWarningUtilization() {
        // Arrange - 85% utilization (above 80% warning threshold)
        when(dataSource.getHikariPoolMXBean()).thenReturn(poolMXBean);
        when(dataSource.getMaximumPoolSize()).thenReturn(100);
        when(dataSource.getMinimumIdle()).thenReturn(25);
        when(poolMXBean.getActiveConnections()).thenReturn(85);
        when(poolMXBean.getIdleConnections()).thenReturn(15);
        when(poolMXBean.getTotalConnections()).thenReturn(100);
        when(poolMXBean.getThreadsAwaitingConnection()).thenReturn(2);
        
        connectionPoolMonitor = new ConnectionPoolMonitor(dataSource, meterRegistry);

        // Act
        Health health = connectionPoolMonitor.health();

        // Assert
        assertEquals(Status.UNKNOWN, health.getStatus());
        assertEquals("Connection pool utilization high", health.getDetails().get("reason"));
        assertEquals("85.00%", health.getDetails().get("utilizationPercentage"));
    }

    @Test
    void testHealthCheckCriticalUtilization() {
        // Arrange - 95% utilization (above 90% critical threshold)
        when(dataSource.getHikariPoolMXBean()).thenReturn(poolMXBean);
        when(dataSource.getMaximumPoolSize()).thenReturn(100);
        when(dataSource.getMinimumIdle()).thenReturn(25);
        when(poolMXBean.getActiveConnections()).thenReturn(95);
        when(poolMXBean.getIdleConnections()).thenReturn(5);
        when(poolMXBean.getTotalConnections()).thenReturn(100);
        when(poolMXBean.getThreadsAwaitingConnection()).thenReturn(5);
        
        connectionPoolMonitor = new ConnectionPoolMonitor(dataSource, meterRegistry);

        // Act
        Health health = connectionPoolMonitor.health();

        // Assert
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("Connection pool under severe stress", health.getDetails().get("reason"));
        assertEquals("95.00%", health.getDetails().get("utilizationPercentage"));
    }

    @Test
    void testHealthCheckManyThreadsWaiting() {
        // Arrange - Many threads waiting (above 10 threshold)
        when(dataSource.getHikariPoolMXBean()).thenReturn(poolMXBean);
        when(dataSource.getMaximumPoolSize()).thenReturn(100);
        when(dataSource.getMinimumIdle()).thenReturn(25);
        when(poolMXBean.getActiveConnections()).thenReturn(50);
        when(poolMXBean.getIdleConnections()).thenReturn(50);
        when(poolMXBean.getTotalConnections()).thenReturn(100);
        when(poolMXBean.getThreadsAwaitingConnection()).thenReturn(15);
        
        connectionPoolMonitor = new ConnectionPoolMonitor(dataSource, meterRegistry);

        // Act
        Health health = connectionPoolMonitor.health();

        // Assert
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("Connection pool under severe stress", health.getDetails().get("reason"));
        assertEquals(15, health.getDetails().get("threadsAwaitingConnection"));
    }

    @Test
    void testHealthCheckNullPoolBean() {
        // Arrange
        when(dataSource.getHikariPoolMXBean()).thenReturn(null);
        ConnectionPoolMonitor monitor = new ConnectionPoolMonitor(dataSource, meterRegistry);

        // Act
        Health health = monitor.health();

        // Assert
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("HikariPoolMXBean not available", health.getDetails().get("reason"));
    }

    @Test
    void testRecordConnectionPoolMetrics() {
        // Arrange
        lenient().when(dataSource.getHikariPoolMXBean()).thenReturn(poolMXBean);
        lenient().when(dataSource.getMaximumPoolSize()).thenReturn(100);
        lenient().when(dataSource.getMinimumIdle()).thenReturn(25);
        lenient().when(poolMXBean.getActiveConnections()).thenReturn(40);
        lenient().when(poolMXBean.getIdleConnections()).thenReturn(10);
        lenient().when(poolMXBean.getTotalConnections()).thenReturn(50);
        lenient().when(poolMXBean.getThreadsAwaitingConnection()).thenReturn(0);
        
        connectionPoolMonitor = new ConnectionPoolMonitor(dataSource, meterRegistry);

        // Act
        connectionPoolMonitor.recordConnectionPoolMetrics();

        // Assert - No exceptions should be thrown
        // Verify that the method completes successfully
        verify(poolMXBean, atLeastOnce()).getActiveConnections();
        verify(poolMXBean, atLeastOnce()).getTotalConnections();
        verify(poolMXBean, atLeastOnce()).getThreadsAwaitingConnection();
    }

    @Test
    void testConstructorWithNonHikariDataSource() {
        // Arrange
        javax.sql.DataSource nonHikariDataSource = mock(javax.sql.DataSource.class);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            new ConnectionPoolMonitor(nonHikariDataSource, meterRegistry));
    }

    @Test
    void testUtilizationCalculationWithZeroTotal() {
        // Arrange
        when(dataSource.getHikariPoolMXBean()).thenReturn(poolMXBean);
        when(dataSource.getMaximumPoolSize()).thenReturn(100);
        when(dataSource.getMinimumIdle()).thenReturn(25);
        when(poolMXBean.getTotalConnections()).thenReturn(0);
        when(poolMXBean.getActiveConnections()).thenReturn(0);
        when(poolMXBean.getIdleConnections()).thenReturn(0);
        when(poolMXBean.getThreadsAwaitingConnection()).thenReturn(0);
        
        connectionPoolMonitor = new ConnectionPoolMonitor(dataSource, meterRegistry);

        // Act
        Health health = connectionPoolMonitor.health();

        // Assert
        assertEquals("0.00%", health.getDetails().get("utilizationPercentage"));
    }
}