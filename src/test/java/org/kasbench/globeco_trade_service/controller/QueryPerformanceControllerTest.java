package org.kasbench.globeco_trade_service.controller;

import org.kasbench.globeco_trade_service.config.QueryPerformanceMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryPerformanceControllerTest {
    
    @Mock
    private QueryPerformanceMonitor queryPerformanceMonitor;
    
    @InjectMocks
    private QueryPerformanceController queryPerformanceController;
    
    @Test
    void testGetQueryStats() {
        // Given
        QueryPerformanceMonitor.QueryPerformanceStats mockStats = 
            new QueryPerformanceMonitor.QueryPerformanceStats(100, 5, 1, 5.0, 1.0);
        when(queryPerformanceMonitor.getPerformanceStats()).thenReturn(mockStats);
        
        // When
        ResponseEntity<QueryPerformanceMonitor.QueryPerformanceStats> response = 
            queryPerformanceController.getQueryStats();
        
        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(100, response.getBody().getTotalQueries());
        assertEquals(5, response.getBody().getSlowQueries());
        assertEquals(1, response.getBody().getVerySlowQueries());
        assertEquals(5.0, response.getBody().getSlowPercentage());
        assertEquals(1.0, response.getBody().getVerySlowPercentage());
        
        verify(queryPerformanceMonitor).getPerformanceStats();
    }
    
    @Test
    void testResetQueryStats() {
        // When
        ResponseEntity<String> response = queryPerformanceController.resetQueryStats();
        
        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Query performance statistics reset successfully", response.getBody());
        
        verify(queryPerformanceMonitor).resetStats();
    }
    
    @Test
    void testGetQueryPerformanceHealthHealthy() {
        // Given - healthy system (low slow query percentage)
        QueryPerformanceMonitor.QueryPerformanceStats mockStats = 
            new QueryPerformanceMonitor.QueryPerformanceStats(1000, 30, 5, 3.0, 0.5);
        when(queryPerformanceMonitor.getPerformanceStats()).thenReturn(mockStats);
        
        // When
        ResponseEntity<QueryPerformanceController.QueryPerformanceHealth> response = 
            queryPerformanceController.getQueryPerformanceHealth();
        
        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("HEALTHY", response.getBody().getStatus());
        assertTrue(response.getBody().isHealthy());
        assertEquals(3.0, response.getBody().getSlowQueryPercentage());
        assertEquals(0.5, response.getBody().getVerySlowQueryPercentage());
        assertEquals(1000, response.getBody().getTotalQueries());
    }
    
    @Test
    void testGetQueryPerformanceHealthDegraded() {
        // Given - degraded system (high slow query percentage)
        QueryPerformanceMonitor.QueryPerformanceStats mockStats = 
            new QueryPerformanceMonitor.QueryPerformanceStats(1000, 80, 5, 8.0, 0.5);
        when(queryPerformanceMonitor.getPerformanceStats()).thenReturn(mockStats);
        
        // When
        ResponseEntity<QueryPerformanceController.QueryPerformanceHealth> response = 
            queryPerformanceController.getQueryPerformanceHealth();
        
        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("DEGRADED", response.getBody().getStatus());
        assertFalse(response.getBody().isHealthy());
        assertEquals(8.0, response.getBody().getSlowQueryPercentage());
        assertEquals(0.5, response.getBody().getVerySlowQueryPercentage());
    }
    
    @Test
    void testGetQueryPerformanceHealthCritical() {
        // Given - critical system (high very slow query percentage)
        QueryPerformanceMonitor.QueryPerformanceStats mockStats = 
            new QueryPerformanceMonitor.QueryPerformanceStats(1000, 80, 20, 8.0, 2.0);
        when(queryPerformanceMonitor.getPerformanceStats()).thenReturn(mockStats);
        
        // When
        ResponseEntity<QueryPerformanceController.QueryPerformanceHealth> response = 
            queryPerformanceController.getQueryPerformanceHealth();
        
        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("CRITICAL", response.getBody().getStatus());
        assertFalse(response.getBody().isHealthy());
        assertEquals(8.0, response.getBody().getSlowQueryPercentage());
        assertEquals(2.0, response.getBody().getVerySlowQueryPercentage());
    }
}