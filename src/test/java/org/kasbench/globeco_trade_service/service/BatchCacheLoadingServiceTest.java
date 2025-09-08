package org.kasbench.globeco_trade_service.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kasbench.globeco_trade_service.client.PortfolioServiceClient;
import org.kasbench.globeco_trade_service.client.SecurityServiceClient;
import org.kasbench.globeco_trade_service.dto.PortfolioDTO;
import org.kasbench.globeco_trade_service.dto.SecurityDTO;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchCacheLoadingServiceTest {

    @Mock
    private SecurityServiceClient securityServiceClient;

    @Mock
    private PortfolioServiceClient portfolioServiceClient;

    private Cache<String, SecurityDTO> securityCache;
    private Cache<String, PortfolioDTO> portfolioCache;
    private BatchCacheLoadingService batchCacheLoadingService;

    @BeforeEach
    void setUp() {
        securityCache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats()
                .build();

        portfolioCache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats()
                .build();

        batchCacheLoadingService = new BatchCacheLoadingService(
                securityServiceClient,
                portfolioServiceClient,
                securityCache,
                portfolioCache
        );
    }

    @Test
    void testBatchLoadSecurities_Success() throws Exception {
        // Given
        Set<String> securityIds = Set.of("SEC001", "SEC002", "SEC003");
        
        when(securityServiceClient.findSecurityById("SEC001"))
                .thenReturn(Optional.of(new SecurityDTO("SEC001", "AAPL")));
        when(securityServiceClient.findSecurityById("SEC002"))
                .thenReturn(Optional.of(new SecurityDTO("SEC002", "GOOGL")));
        when(securityServiceClient.findSecurityById("SEC003"))
                .thenReturn(Optional.of(new SecurityDTO("SEC003", "MSFT")));

        // When
        CompletableFuture<Map<String, SecurityDTO>> future = batchCacheLoadingService.batchLoadSecurities(securityIds);
        Map<String, SecurityDTO> result = future.get();

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get("SEC001").getTicker()).isEqualTo("AAPL");
        assertThat(result.get("SEC002").getTicker()).isEqualTo("GOOGL");
        assertThat(result.get("SEC003").getTicker()).isEqualTo("MSFT");
        
        // Verify caching
        assertThat(securityCache.getIfPresent("ID:SEC001")).isNotNull();
        assertThat(securityCache.getIfPresent("ID:SEC002")).isNotNull();
        assertThat(securityCache.getIfPresent("ID:SEC003")).isNotNull();
    }

    @Test
    void testBatchLoadSecurities_WithCachedEntries() throws Exception {
        // Given
        Set<String> securityIds = Set.of("SEC001", "SEC002");
        
        // Pre-populate cache
        SecurityDTO cachedSecurity = new SecurityDTO("SEC001", "AAPL");
        securityCache.put("ID:SEC001", cachedSecurity);
        
        when(securityServiceClient.findSecurityById("SEC002"))
                .thenReturn(Optional.of(new SecurityDTO("SEC002", "GOOGL")));

        // When
        CompletableFuture<Map<String, SecurityDTO>> future = batchCacheLoadingService.batchLoadSecurities(securityIds);
        Map<String, SecurityDTO> result = future.get();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get("SEC001")).isEqualTo(cachedSecurity);
        assertThat(result.get("SEC002").getTicker()).isEqualTo("GOOGL");
    }

    @Test
    void testBatchLoadSecurities_WithFailures() throws Exception {
        // Given
        Set<String> securityIds = Set.of("SEC001", "SEC002");
        
        when(securityServiceClient.findSecurityById("SEC001"))
                .thenReturn(Optional.of(new SecurityDTO("SEC001", "AAPL")));
        when(securityServiceClient.findSecurityById("SEC002"))
                .thenReturn(Optional.empty()); // Not found

        // When
        CompletableFuture<Map<String, SecurityDTO>> future = batchCacheLoadingService.batchLoadSecurities(securityIds);
        Map<String, SecurityDTO> result = future.get();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get("SEC001").getTicker()).isEqualTo("AAPL");
        assertThat(result.get("SEC002").getSecurityId()).isEqualTo("SEC002"); // Fallback
        assertThat(result.get("SEC002").getTicker()).isEqualTo("SEC002"); // Fallback
    }

    @Test
    void testBatchLoadPortfolios_Success() throws Exception {
        // Given
        Set<String> portfolioIds = Set.of("PORT001", "PORT002");
        
        when(portfolioServiceClient.findPortfolioById("PORT001"))
                .thenReturn(Optional.of(new PortfolioDTO("PORT001", "Main Portfolio")));
        when(portfolioServiceClient.findPortfolioById("PORT002"))
                .thenReturn(Optional.of(new PortfolioDTO("PORT002", "Trading Portfolio")));

        // When
        CompletableFuture<Map<String, PortfolioDTO>> future = batchCacheLoadingService.batchLoadPortfolios(portfolioIds);
        Map<String, PortfolioDTO> result = future.get();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get("PORT001").getName()).isEqualTo("Main Portfolio");
        assertThat(result.get("PORT002").getName()).isEqualTo("Trading Portfolio");
        
        // Verify caching
        assertThat(portfolioCache.getIfPresent("ID:PORT001")).isNotNull();
        assertThat(portfolioCache.getIfPresent("ID:PORT002")).isNotNull();
    }

    @Test
    void testBatchLoadSecuritiesByTicker_Success() throws Exception {
        // Given
        Set<String> tickers = Set.of("AAPL", "googl", " MSFT ");
        
        when(securityServiceClient.findSecurityByTicker("AAPL"))
                .thenReturn(Optional.of(new SecurityDTO("SEC001", "AAPL")));
        when(securityServiceClient.findSecurityByTicker("GOOGL"))
                .thenReturn(Optional.of(new SecurityDTO("SEC002", "GOOGL")));
        when(securityServiceClient.findSecurityByTicker("MSFT"))
                .thenReturn(Optional.of(new SecurityDTO("SEC003", "MSFT")));

        // When
        CompletableFuture<Map<String, SecurityDTO>> future = batchCacheLoadingService.batchLoadSecuritiesByTicker(tickers);
        Map<String, SecurityDTO> result = future.get();

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get("AAPL").getSecurityId()).isEqualTo("SEC001");
        assertThat(result.get("GOOGL").getSecurityId()).isEqualTo("SEC002");
        assertThat(result.get("MSFT").getSecurityId()).isEqualTo("SEC003");
    }

    @Test
    void testBatchLoadSecurities_EmptySet() throws Exception {
        // Given
        Set<String> emptySet = Set.of();

        // When
        CompletableFuture<Map<String, SecurityDTO>> future = batchCacheLoadingService.batchLoadSecurities(emptySet);
        Map<String, SecurityDTO> result = future.get();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testBatchLoadPortfolios_EmptySet() throws Exception {
        // Given
        Set<String> emptySet = Set.of();

        // When
        CompletableFuture<Map<String, PortfolioDTO>> future = batchCacheLoadingService.batchLoadPortfolios(emptySet);
        Map<String, PortfolioDTO> result = future.get();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testGetCacheStatistics() {
        // Given
        securityCache.put("TEST", new SecurityDTO("TEST", "TEST"));
        portfolioCache.put("TEST", new PortfolioDTO("TEST", "TEST"));

        // When
        BatchCacheLoadingService.BatchCacheStats stats = batchCacheLoadingService.getCacheStatistics();

        // Then
        assertThat(stats.getSecurityCacheSize()).isEqualTo(1);
        assertThat(stats.getPortfolioCacheSize()).isEqualTo(1);
        assertThat(stats.toString()).contains("securityCache");
        assertThat(stats.toString()).contains("portfolioCache");
    }

    @Test
    void testBatchLoadSecurities_WithException() throws Exception {
        // Given
        Set<String> securityIds = Set.of("SEC001");
        
        when(securityServiceClient.findSecurityById(anyString()))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When
        CompletableFuture<Map<String, SecurityDTO>> future = batchCacheLoadingService.batchLoadSecurities(securityIds);
        Map<String, SecurityDTO> result = future.get();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get("SEC001").getSecurityId()).isEqualTo("SEC001"); // Fallback
    }
}