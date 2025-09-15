package org.kasbench.globeco_trade_service.service;

import com.github.benmanes.caffeine.cache.Cache;
import org.kasbench.globeco_trade_service.client.PortfolioServiceClient;
import org.kasbench.globeco_trade_service.client.SecurityServiceClient;
import org.kasbench.globeco_trade_service.dto.PortfolioDTO;
import org.kasbench.globeco_trade_service.dto.SecurityDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Service for batch loading cache data with parallel processing.
 * Implements requirements 7.3 and 7.5 from the performance optimization spec.
 */
@Service
public class BatchCacheLoadingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchCacheLoadingService.class);
    
    private final SecurityServiceClient securityServiceClient;
    private final PortfolioServiceClient portfolioServiceClient;
    private final Cache<String, SecurityDTO> optimizedSecurityCache;
    private final Cache<String, PortfolioDTO> optimizedPortfolioCache;
    private final ExecutorService batchLoadingExecutor;
    
    public BatchCacheLoadingService(
            SecurityServiceClient securityServiceClient,
            PortfolioServiceClient portfolioServiceClient,
            @Qualifier("optimizedSecurityCache") Cache<String, SecurityDTO> optimizedSecurityCache,
            @Qualifier("optimizedPortfolioCache") Cache<String, PortfolioDTO> optimizedPortfolioCache) {
        
        this.securityServiceClient = securityServiceClient;
        this.portfolioServiceClient = portfolioServiceClient;
        this.optimizedSecurityCache = optimizedSecurityCache;
        this.optimizedPortfolioCache = optimizedPortfolioCache;
        
        // Create dedicated thread pool for batch loading operations
        this.batchLoadingExecutor = Executors.newFixedThreadPool(10, r -> {
            Thread t = new Thread(r, "batch-cache-loader");
            t.setDaemon(true);
            return t;
        });
        
        logger.debug("BatchCacheLoadingService initialized with dedicated thread pool");
    }
    
    /**
     * Parallel batch loading for security data
     * Requirement 7.3: Create parallel batch loading for security data
     * 
     * @param securityIds Set of security IDs to load
     * @return CompletableFuture containing concurrent map of loaded securities
     */
    public CompletableFuture<Map<String, SecurityDTO>> batchLoadSecurities(Set<String> securityIds) {
        if (securityIds == null || securityIds.isEmpty()) {
            logger.debug("No security IDs provided for batch loading");
            return CompletableFuture.completedFuture(new ConcurrentHashMap<>());
        }
        
        logger.debug("Starting batch load for {} securities", securityIds.size());
        
        return CompletableFuture.supplyAsync(() -> {
            // Use ConcurrentHashMap for thread safety
            Map<String, SecurityDTO> results = new ConcurrentHashMap<>();
            
            // Filter out securities already in cache
            Set<String> uncachedIds = securityIds.stream()
                    .filter(id -> optimizedSecurityCache.getIfPresent(getCacheKey("ID", id)) == null)
                    .collect(Collectors.toSet());
            
            if (uncachedIds.isEmpty()) {
                logger.debug("All securities found in cache, no external calls needed");
                // Return cached values
                securityIds.forEach(id -> {
                    SecurityDTO cached = optimizedSecurityCache.getIfPresent(getCacheKey("ID", id));
                    if (cached != null) {
                        results.put(id, cached);
                    }
                });
                return results;
            }
            
            logger.debug("Loading {} uncached securities from external service", uncachedIds.size());
            
            // Create parallel futures for each security lookup
            Map<String, CompletableFuture<Optional<SecurityDTO>>> futures = uncachedIds.stream()
                    .collect(Collectors.toConcurrentMap(
                            id -> id,
                            id -> CompletableFuture.supplyAsync(() -> {
                                try {
                                    return securityServiceClient.findSecurityById(id);
                                } catch (Exception e) {
                                    logger.warn("Failed to load security {}: {}", id, e.getMessage());
                                    return Optional.<SecurityDTO>empty();
                                }
                            }, batchLoadingExecutor)
                    ));
            
            // Wait for all futures to complete and collect results
            futures.forEach((id, future) -> {
                try {
                    Optional<SecurityDTO> securityOpt = future.join();
                    if (securityOpt.isPresent()) {
                        SecurityDTO security = securityOpt.get();
                        results.put(id, security);
                        // Cache the result
                        optimizedSecurityCache.put(getCacheKey("ID", id), security);
                        logger.debug("Loaded and cached security: {}", id);
                    } else {
                        // Create fallback and cache it
                        SecurityDTO fallback = createFallbackSecurity(id);
                        results.put(id, fallback);
                        optimizedSecurityCache.put(getCacheKey("ID", id), fallback);
                        logger.debug("Created fallback for security: {}", id);
                    }
                } catch (Exception e) {
                    logger.error("Error processing security {}: {}", id, e.getMessage(), e);
                    // Create fallback for error cases
                    SecurityDTO fallback = createFallbackSecurity(id);
                    results.put(id, fallback);
                }
            });
            
            // Add any cached securities that were requested
            securityIds.stream()
                    .filter(id -> !uncachedIds.contains(id))
                    .forEach(id -> {
                        SecurityDTO cached = optimizedSecurityCache.getIfPresent(getCacheKey("ID", id));
                        if (cached != null) {
                            results.put(id, cached);
                        }
                    });
            
            logger.debug("Batch load completed for securities. Loaded: {}, Total requested: {}", 
                    results.size(), securityIds.size());
            
            return results;
            
        }, batchLoadingExecutor);
    }
    
    /**
     * Parallel batch loading for portfolio data
     * Requirement 7.5: Create parallel batch loading for portfolio data
     * 
     * @param portfolioIds Set of portfolio IDs to load
     * @return CompletableFuture containing concurrent map of loaded portfolios
     */
    public CompletableFuture<Map<String, PortfolioDTO>> batchLoadPortfolios(Set<String> portfolioIds) {
        if (portfolioIds == null || portfolioIds.isEmpty()) {
            logger.debug("No portfolio IDs provided for batch loading");
            return CompletableFuture.completedFuture(new ConcurrentHashMap<>());
        }
        
        logger.debug("Starting batch load for {} portfolios", portfolioIds.size());
        
        return CompletableFuture.supplyAsync(() -> {
            // Use ConcurrentHashMap for thread safety
            Map<String, PortfolioDTO> results = new ConcurrentHashMap<>();
            
            // Filter out portfolios already in cache
            Set<String> uncachedIds = portfolioIds.stream()
                    .filter(id -> optimizedPortfolioCache.getIfPresent(getCacheKey("ID", id)) == null)
                    .collect(Collectors.toSet());
            
            if (uncachedIds.isEmpty()) {
                logger.debug("All portfolios found in cache, no external calls needed");
                // Return cached values
                portfolioIds.forEach(id -> {
                    PortfolioDTO cached = optimizedPortfolioCache.getIfPresent(getCacheKey("ID", id));
                    if (cached != null) {
                        results.put(id, cached);
                    }
                });
                return results;
            }
            
            logger.debug("Loading {} uncached portfolios from external service", uncachedIds.size());
            
            // Create parallel futures for each portfolio lookup
            Map<String, CompletableFuture<Optional<PortfolioDTO>>> futures = uncachedIds.stream()
                    .collect(Collectors.toConcurrentMap(
                            id -> id,
                            id -> CompletableFuture.supplyAsync(() -> {
                                try {
                                    return portfolioServiceClient.findPortfolioById(id);
                                } catch (Exception e) {
                                    logger.warn("Failed to load portfolio {}: {}", id, e.getMessage());
                                    return Optional.<PortfolioDTO>empty();
                                }
                            }, batchLoadingExecutor)
                    ));
            
            // Wait for all futures to complete and collect results
            futures.forEach((id, future) -> {
                try {
                    Optional<PortfolioDTO> portfolioOpt = future.join();
                    if (portfolioOpt.isPresent()) {
                        PortfolioDTO portfolio = portfolioOpt.get();
                        results.put(id, portfolio);
                        // Cache the result
                        optimizedPortfolioCache.put(getCacheKey("ID", id), portfolio);
                        logger.debug("Loaded and cached portfolio: {}", id);
                    } else {
                        // Create fallback and cache it
                        PortfolioDTO fallback = createFallbackPortfolio(id);
                        results.put(id, fallback);
                        optimizedPortfolioCache.put(getCacheKey("ID", id), fallback);
                        logger.debug("Created fallback for portfolio: {}", id);
                    }
                } catch (Exception e) {
                    logger.error("Error processing portfolio {}: {}", id, e.getMessage(), e);
                    // Create fallback for error cases
                    PortfolioDTO fallback = createFallbackPortfolio(id);
                    results.put(id, fallback);
                }
            });
            
            // Add any cached portfolios that were requested
            portfolioIds.stream()
                    .filter(id -> !uncachedIds.contains(id))
                    .forEach(id -> {
                        PortfolioDTO cached = optimizedPortfolioCache.getIfPresent(getCacheKey("ID", id));
                        if (cached != null) {
                            results.put(id, cached);
                        }
                    });
            
            logger.debug("Batch load completed for portfolios. Loaded: {}, Total requested: {}", 
                    results.size(), portfolioIds.size());
            
            return results;
            
        }, batchLoadingExecutor);
    }
    
    /**
     * Batch load securities by ticker symbols
     * 
     * @param tickers Set of ticker symbols to load
     * @return CompletableFuture containing concurrent map of loaded securities by ticker
     */
    public CompletableFuture<Map<String, SecurityDTO>> batchLoadSecuritiesByTicker(Set<String> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            logger.debug("No tickers provided for batch loading");
            return CompletableFuture.completedFuture(new ConcurrentHashMap<>());
        }
        
        logger.debug("Starting batch load for {} securities by ticker", tickers.size());
        
        return CompletableFuture.supplyAsync(() -> {
            Map<String, SecurityDTO> results = new ConcurrentHashMap<>();
            
            // Normalize tickers and filter out cached ones
            Set<String> normalizedTickers = tickers.stream()
                    .map(ticker -> ticker.trim().toUpperCase())
                    .collect(Collectors.toSet());
            
            Set<String> uncachedTickers = normalizedTickers.stream()
                    .filter(ticker -> optimizedSecurityCache.getIfPresent(ticker) == null)
                    .collect(Collectors.toSet());
            
            if (uncachedTickers.isEmpty()) {
                logger.debug("All securities found in cache by ticker, no external calls needed");
                normalizedTickers.forEach(ticker -> {
                    SecurityDTO cached = optimizedSecurityCache.getIfPresent(ticker);
                    if (cached != null) {
                        results.put(ticker, cached);
                    }
                });
                return results;
            }
            
            // Create parallel futures for each ticker lookup
            Map<String, CompletableFuture<Optional<SecurityDTO>>> futures = uncachedTickers.stream()
                    .collect(Collectors.toConcurrentMap(
                            ticker -> ticker,
                            ticker -> CompletableFuture.supplyAsync(() -> {
                                try {
                                    return securityServiceClient.findSecurityByTicker(ticker);
                                } catch (Exception e) {
                                    logger.warn("Failed to load security by ticker {}: {}", ticker, e.getMessage());
                                    return Optional.<SecurityDTO>empty();
                                }
                            }, batchLoadingExecutor)
                    ));
            
            // Process results
            futures.forEach((ticker, future) -> {
                try {
                    Optional<SecurityDTO> securityOpt = future.join();
                    if (securityOpt.isPresent()) {
                        SecurityDTO security = securityOpt.get();
                        results.put(ticker, security);
                        optimizedSecurityCache.put(ticker, security);
                        logger.debug("Loaded and cached security by ticker: {}", ticker);
                    } else {
                        SecurityDTO fallback = createFallbackSecurity(ticker);
                        results.put(ticker, fallback);
                        optimizedSecurityCache.put(ticker, fallback);
                        logger.debug("Created fallback for ticker: {}", ticker);
                    }
                } catch (Exception e) {
                    logger.error("Error processing ticker {}: {}", ticker, e.getMessage(), e);
                    SecurityDTO fallback = createFallbackSecurity(ticker);
                    results.put(ticker, fallback);
                }
            });
            
            // Add cached securities
            normalizedTickers.stream()
                    .filter(ticker -> !uncachedTickers.contains(ticker))
                    .forEach(ticker -> {
                        SecurityDTO cached = optimizedSecurityCache.getIfPresent(ticker);
                        if (cached != null) {
                            results.put(ticker, cached);
                        }
                    });
            
            logger.debug("Batch load by ticker completed. Loaded: {}, Total requested: {}", 
                    results.size(), tickers.size());
            
            return results;
            
        }, batchLoadingExecutor);
    }
    
    /**
     * Get cache statistics for monitoring
     */
    public BatchCacheStats getCacheStatistics() {
        return new BatchCacheStats(
                optimizedSecurityCache.estimatedSize(),
                optimizedSecurityCache.stats().hitRate(),
                optimizedPortfolioCache.estimatedSize(),
                optimizedPortfolioCache.stats().hitRate()
        );
    }
    
    /**
     * Create cache key with prefix
     */
    private String getCacheKey(String prefix, String id) {
        return prefix + ":" + id;
    }
    
    /**
     * Create fallback security DTO
     */
    private SecurityDTO createFallbackSecurity(String identifier) {
        String safeId = identifier != null ? identifier.trim() : "UNKNOWN";
        return new SecurityDTO(safeId, safeId);
    }
    
    /**
     * Create fallback portfolio DTO
     */
    private PortfolioDTO createFallbackPortfolio(String identifier) {
        String safeId = identifier != null ? identifier.trim() : "UNKNOWN";
        return new PortfolioDTO(safeId, safeId);
    }
    
    /**
     * Statistics holder for batch cache operations
     */
    public static class BatchCacheStats {
        private final long securityCacheSize;
        private final double securityHitRate;
        private final long portfolioCacheSize;
        private final double portfolioHitRate;
        
        public BatchCacheStats(long securityCacheSize, double securityHitRate, 
                              long portfolioCacheSize, double portfolioHitRate) {
            this.securityCacheSize = securityCacheSize;
            this.securityHitRate = securityHitRate;
            this.portfolioCacheSize = portfolioCacheSize;
            this.portfolioHitRate = portfolioHitRate;
        }
        
        public long getSecurityCacheSize() { return securityCacheSize; }
        public double getSecurityHitRate() { return securityHitRate; }
        public long getPortfolioCacheSize() { return portfolioCacheSize; }
        public double getPortfolioHitRate() { return portfolioHitRate; }
        
        @Override
        public String toString() {
            return String.format("BatchCacheStats{securityCache: size=%d, hitRate=%.2f%%, " +
                               "portfolioCache: size=%d, hitRate=%.2f%%}",
                    securityCacheSize, securityHitRate * 100,
                    portfolioCacheSize, portfolioHitRate * 100);
        }
    }
}