package org.kasbench.globeco_trade_service.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManagerFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryPerformanceMonitorTest {
    
    @Mock
    private EntityManagerFactory entityManagerFactory;
    
    private MeterRegistry meterRegistry;
    private QueryPerformanceMonitor queryPerformanceMonitor;
    
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        queryPerformanceMonitor = new QueryPerformanceMonitor(meterRegistry);
    }
    
    @Test
    void testQueryTimingForFastQuery() throws InterruptedException {
        // Given
        String sql = "SELECT * FROM trade_order WHERE id = 1";
        String queryType = "select";
        
        // When
        queryPerformanceMonitor.startQueryTiming(sql, queryType);
        Thread.sleep(50); // Simulate fast query (50ms)
        queryPerformanceMonitor.endQueryTiming();
        
        // Then
        QueryPerformanceMonitor.QueryPerformanceStats stats = queryPerformanceMonitor.getPerformanceStats();
        assertEquals(1, stats.getTotalQueries());
        assertEquals(0, stats.getSlowQueries());
        assertEquals(0, stats.getVerySlowQueries());
        assertEquals(0.0, stats.getSlowPercentage());
    }
    
    @Test
    void testQueryTimingForSlowQuery() throws InterruptedException {
        // Given
        String sql = "SELECT * FROM trade_order t JOIN execution e ON t.id = e.trade_order_id";
        String queryType = "select_join";
        
        // When
        queryPerformanceMonitor.startQueryTiming(sql, queryType);
        Thread.sleep(1100); // Simulate slow query (1.1 seconds)
        queryPerformanceMonitor.endQueryTiming();
        
        // Then
        QueryPerformanceMonitor.QueryPerformanceStats stats = queryPerformanceMonitor.getPerformanceStats();
        assertEquals(1, stats.getTotalQueries());
        assertEquals(1, stats.getSlowQueries());
        assertEquals(0, stats.getVerySlowQueries());
        assertEquals(100.0, stats.getSlowPercentage());
    }
    
    @Test
    void testQueryClassification() {
        // Test different SQL query types
        queryPerformanceMonitor.startQueryTiming("SELECT * FROM trade_order", "select");
        queryPerformanceMonitor.endQueryTiming();
        
        queryPerformanceMonitor.startQueryTiming("INSERT INTO trade_order VALUES (...)", "insert");
        queryPerformanceMonitor.endQueryTiming();
        
        queryPerformanceMonitor.startQueryTiming("UPDATE trade_order SET quantity = 100", "update");
        queryPerformanceMonitor.endQueryTiming();
        
        queryPerformanceMonitor.startQueryTiming("DELETE FROM trade_order WHERE id = 1", "delete");
        queryPerformanceMonitor.endQueryTiming();
        
        QueryPerformanceMonitor.QueryPerformanceStats stats = queryPerformanceMonitor.getPerformanceStats();
        assertEquals(4, stats.getTotalQueries());
    }
    
    @Test
    void testPerformanceStatsCalculation() {
        // Execute multiple queries with different performance characteristics
        
        // 3 fast queries
        for (int i = 0; i < 3; i++) {
            queryPerformanceMonitor.startQueryTiming("SELECT * FROM trade_order WHERE id = " + i, "select");
            queryPerformanceMonitor.endQueryTiming();
        }
        
        // 1 slow query (simulated by manipulating internal state for testing)
        queryPerformanceMonitor.startQueryTiming("SLOW QUERY", "select");
        try {
            Thread.sleep(1100); // Make it slow
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        queryPerformanceMonitor.endQueryTiming();
        
        QueryPerformanceMonitor.QueryPerformanceStats stats = queryPerformanceMonitor.getPerformanceStats();
        assertEquals(4, stats.getTotalQueries());
        assertEquals(1, stats.getSlowQueries());
        assertEquals(25.0, stats.getSlowPercentage(), 0.1);
    }
    
    @Test
    void testResetStats() {
        // Given - execute some queries
        queryPerformanceMonitor.startQueryTiming("SELECT * FROM trade_order", "select");
        queryPerformanceMonitor.endQueryTiming();
        
        QueryPerformanceMonitor.QueryPerformanceStats statsBefore = queryPerformanceMonitor.getPerformanceStats();
        assertEquals(1, statsBefore.getTotalQueries());
        
        // When - reset stats
        queryPerformanceMonitor.resetStats();
        
        // Then - stats should be reset
        QueryPerformanceMonitor.QueryPerformanceStats statsAfter = queryPerformanceMonitor.getPerformanceStats();
        assertEquals(0, statsAfter.getTotalQueries());
        assertEquals(0, statsAfter.getSlowQueries());
        assertEquals(0, statsAfter.getVerySlowQueries());
    }
    
    @Test
    void testQueryPerformanceStatsToString() {
        QueryPerformanceMonitor.QueryPerformanceStats stats = 
            new QueryPerformanceMonitor.QueryPerformanceStats(100, 5, 1, 5.0, 1.0);
        
        String result = stats.toString();
        assertTrue(result.contains("total=100"));
        assertTrue(result.contains("slow=5"));
        assertTrue(result.contains("verySlow=1"));
        assertTrue(result.contains("5.00%"));
        assertTrue(result.contains("1.00%"));
    }
    
    @Test
    void testNullSqlHandling() {
        // Should not throw exception with null SQL
        queryPerformanceMonitor.startQueryTiming(null, "unknown");
        queryPerformanceMonitor.endQueryTiming();
        
        QueryPerformanceMonitor.QueryPerformanceStats stats = queryPerformanceMonitor.getPerformanceStats();
        assertEquals(1, stats.getTotalQueries());
    }
}