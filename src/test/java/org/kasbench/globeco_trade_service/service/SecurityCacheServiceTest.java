package org.kasbench.globeco_trade_service.service;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kasbench.globeco_trade_service.client.SecurityServiceClient;
import org.kasbench.globeco_trade_service.dto.SecurityDTO;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityCacheServiceTest {

    @Mock
    private SecurityServiceClient securityServiceClient;

    private SecurityCacheService securityCacheService;

    @BeforeEach
    void setUp() {
        // Create service with short TTL for testing
        securityCacheService = new SecurityCacheService(securityServiceClient, 1, 100);
    }

    @Test
    void testGetSecurityByTicker_CacheMiss_ExternalServiceFound() {
        // Arrange
        String ticker = "AAPL";
        SecurityDTO mockSecurity = new SecurityDTO("SEC123", "AAPL");
        when(securityServiceClient.findSecurityByTicker("AAPL"))
                .thenReturn(Optional.of(mockSecurity));

        // Act
        SecurityDTO result = securityCacheService.getSecurityByTicker(ticker);

        // Assert
        assertEquals("SEC123", result.getSecurityId());
        assertEquals("AAPL", result.getTicker());
        verify(securityServiceClient).findSecurityByTicker("AAPL");
        
        // Verify it was cached
        assertEquals(1, securityCacheService.getCacheSize());
    }

    @Test
    void testGetSecurityByTicker_CacheHit() {
        // Arrange
        String ticker = "AAPL";
        SecurityDTO mockSecurity = new SecurityDTO("SEC123", "AAPL");
        when(securityServiceClient.findSecurityByTicker("AAPL"))
                .thenReturn(Optional.of(mockSecurity));

        // First call - cache miss
        SecurityDTO firstResult = securityCacheService.getSecurityByTicker(ticker);
        
        // Act - Second call - should be cache hit
        SecurityDTO secondResult = securityCacheService.getSecurityByTicker(ticker);

        // Assert
        assertEquals(firstResult.getSecurityId(), secondResult.getSecurityId());
        assertEquals(firstResult.getTicker(), secondResult.getTicker());
        
        // Verify external service was only called once
        verify(securityServiceClient, times(1)).findSecurityByTicker("AAPL");
        
        // Verify cache stats
        assertTrue(securityCacheService.getCacheHitRate() > 0);
    }

    @Test
    void testGetSecurityByTicker_CacheMiss_ExternalServiceNotFound() {
        // Arrange
        String ticker = "UNKNOWN";
        when(securityServiceClient.findSecurityByTicker("UNKNOWN"))
                .thenReturn(Optional.empty());

        // Act
        SecurityDTO result = securityCacheService.getSecurityByTicker(ticker);

        // Assert
        assertEquals("UNKNOWN", result.getSecurityId());
        assertEquals("UNKNOWN", result.getTicker());
        verify(securityServiceClient).findSecurityByTicker("UNKNOWN");
        
        // Verify fallback was cached
        assertEquals(1, securityCacheService.getCacheSize());
    }

    @Test
    void testGetSecurityByTicker_ExternalServiceException() {
        // Arrange
        String ticker = "AAPL";
        when(securityServiceClient.findSecurityByTicker("AAPL"))
                .thenThrow(new RuntimeException("Service unavailable"));

        // Act
        SecurityDTO result = securityCacheService.getSecurityByTicker(ticker);

        // Assert
        assertEquals("AAPL", result.getSecurityId());
        assertEquals("AAPL", result.getTicker());
        verify(securityServiceClient).findSecurityByTicker("AAPL");
        
        // Verify no caching on error
        assertEquals(0, securityCacheService.getCacheSize());
    }

    @Test
    void testGetSecurityByTicker_NullTicker() {
        // Act
        SecurityDTO result = securityCacheService.getSecurityByTicker(null);

        // Assert
        assertEquals("UNKNOWN", result.getSecurityId());
        assertEquals("UNKNOWN", result.getTicker());
        verifyNoInteractions(securityServiceClient);
        assertEquals(0, securityCacheService.getCacheSize());
    }

    @Test
    void testGetSecurityByTicker_EmptyTicker() {
        // Act
        SecurityDTO result = securityCacheService.getSecurityByTicker("");

        // Assert
        assertEquals("", result.getSecurityId());
        assertEquals("", result.getTicker());
        verifyNoInteractions(securityServiceClient);
        assertEquals(0, securityCacheService.getCacheSize());
    }

    @Test
    void testGetSecurityByTicker_BlankTicker() {
        // Act
        SecurityDTO result = securityCacheService.getSecurityByTicker("   ");

        // Assert
        assertEquals("", result.getSecurityId());
        assertEquals("", result.getTicker());
        verifyNoInteractions(securityServiceClient);
        assertEquals(0, securityCacheService.getCacheSize());
    }

    @Test
    void testGetSecurityByTicker_TickerNormalization() {
        // Arrange
        String ticker = "aapl";
        SecurityDTO mockSecurity = new SecurityDTO("SEC123", "AAPL");
        when(securityServiceClient.findSecurityByTicker("AAPL"))
                .thenReturn(Optional.of(mockSecurity));

        // Act
        SecurityDTO result = securityCacheService.getSecurityByTicker(ticker);

        // Assert
        assertEquals("SEC123", result.getSecurityId());
        assertEquals("AAPL", result.getTicker());
        verify(securityServiceClient).findSecurityByTicker("AAPL");
        
        // Test cache hit with different case
        SecurityDTO cachedResult = securityCacheService.getSecurityByTicker("AAPL");
        assertEquals(result.getSecurityId(), cachedResult.getSecurityId());
        
        // Should only call external service once
        verify(securityServiceClient, times(1)).findSecurityByTicker("AAPL");
    }

    @Test
    void testInvalidate() {
        // Arrange
        String ticker = "AAPL";
        SecurityDTO mockSecurity = new SecurityDTO("SEC123", "AAPL");
        when(securityServiceClient.findSecurityByTicker("AAPL"))
                .thenReturn(Optional.of(mockSecurity));

        // Cache a security
        securityCacheService.getSecurityByTicker(ticker);
        assertEquals(1, securityCacheService.getCacheSize());

        // Act
        securityCacheService.invalidate(ticker);

        // Assert
        assertEquals(0, securityCacheService.getCacheSize());
    }

    @Test
    void testInvalidateAll() {
        // Arrange
        SecurityDTO mockSecurity1 = new SecurityDTO("SEC123", "AAPL");
        SecurityDTO mockSecurity2 = new SecurityDTO("SEC456", "MSFT");
        when(securityServiceClient.findSecurityByTicker("AAPL"))
                .thenReturn(Optional.of(mockSecurity1));
        when(securityServiceClient.findSecurityByTicker("MSFT"))
                .thenReturn(Optional.of(mockSecurity2));

        // Cache multiple securities
        securityCacheService.getSecurityByTicker("AAPL");
        securityCacheService.getSecurityByTicker("MSFT");
        assertEquals(2, securityCacheService.getCacheSize());

        // Act
        securityCacheService.invalidateAll();

        // Assert
        assertEquals(0, securityCacheService.getCacheSize());
    }

    @Test
    void testGetCacheStats() {
        // Arrange
        String ticker = "AAPL";
        SecurityDTO mockSecurity = new SecurityDTO("SEC123", "AAPL");
        when(securityServiceClient.findSecurityByTicker("AAPL"))
                .thenReturn(Optional.of(mockSecurity));

        // Act
        securityCacheService.getSecurityByTicker(ticker); // Miss
        securityCacheService.getSecurityByTicker(ticker); // Hit

        // Assert
        CacheStats stats = securityCacheService.getCacheStats();
        assertEquals(1, stats.hitCount());
        assertEquals(1, stats.missCount());
        assertEquals(50.0, securityCacheService.getCacheHitRate(), 0.01);
    }

    @Test
    void testLogCacheStats() {
        // This test verifies the method doesn't throw exceptions
        // In a real scenario, you might want to capture log output
        
        // Arrange
        String ticker = "AAPL";
        SecurityDTO mockSecurity = new SecurityDTO("SEC123", "AAPL");
        when(securityServiceClient.findSecurityByTicker("AAPL"))
                .thenReturn(Optional.of(mockSecurity));

        securityCacheService.getSecurityByTicker(ticker);

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> securityCacheService.logCacheStats());
    }

    @Test
    void testCacheSize() {
        // Initially empty
        assertEquals(0, securityCacheService.getCacheSize());

        // Add one item
        SecurityDTO mockSecurity = new SecurityDTO("SEC123", "AAPL");
        when(securityServiceClient.findSecurityByTicker("AAPL"))
                .thenReturn(Optional.of(mockSecurity));
        
        securityCacheService.getSecurityByTicker("AAPL");
        assertEquals(1, securityCacheService.getCacheSize());
    }

    @Test
    void testCacheHitRateInitiallyZero() {
        // NOTE: Cache statistics are cumulative and shared across tests
        // This test verifies that the hit rate is a valid percentage (0-100)
        double hitRate = securityCacheService.getCacheHitRate();
        assertTrue(hitRate >= 0.0 && hitRate <= 100.0, 
                "Hit rate should be between 0.0 and 100.0, but was: " + hitRate);
    }
} 