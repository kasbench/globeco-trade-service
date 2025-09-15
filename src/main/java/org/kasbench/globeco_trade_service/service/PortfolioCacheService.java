package org.kasbench.globeco_trade_service.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.kasbench.globeco_trade_service.client.PortfolioServiceClient;
import org.kasbench.globeco_trade_service.dto.PortfolioDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class PortfolioCacheService {
    private static final Logger logger = LoggerFactory.getLogger(PortfolioCacheService.class);
    
    private final PortfolioServiceClient portfolioServiceClient;
    private final Cache<String, PortfolioDTO> portfolioCache;
    
    public PortfolioCacheService(
            PortfolioServiceClient portfolioServiceClient,
            @Value("${cache.portfolio.ttl-minutes:5}") int ttlMinutes,
            @Value("${cache.portfolio.max-size:1000}") int maxSize) {
        this.portfolioServiceClient = portfolioServiceClient;
        this.portfolioCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(Duration.ofMinutes(ttlMinutes))
                .recordStats()
                .build();
        
        logger.debug("PortfolioCacheService initialized with TTL: {} minutes, Max Size: {}", ttlMinutes, maxSize);
    }
    
    /**
     * Get portfolio by ID - creates fallback since Portfolio Service doesn't support ID lookup
     * @param portfolioId The portfolio ID to create fallback for
     * @return PortfolioDTO with portfolioId as both portfolioId and name fields
     */
    public PortfolioDTO getPortfolioById(String portfolioId) {
        if (portfolioId == null || portfolioId.trim().isEmpty()) {
            logger.warn("getPortfolioById called with null or empty portfolioId");
            return createFallbackPortfolioById(portfolioId);
        }
        
        String normalizedId = portfolioId.trim();
        String cacheKey = "ID:" + normalizedId; // Prefix to distinguish from name cache keys
        
        try {
            // Try cache first
            PortfolioDTO cached = portfolioCache.getIfPresent(cacheKey);
            if (cached != null) {
                logger.debug("Cache hit for portfolio ID: {}", normalizedId);
                return cached;
            }
            
            // Cache miss - call external service
            logger.debug("Cache miss for portfolio ID: {}, calling external service", normalizedId);
            Optional<PortfolioDTO> portfolioOpt = portfolioServiceClient.findPortfolioById(normalizedId);
            
            if (portfolioOpt.isPresent()) {
                PortfolioDTO portfolio = portfolioOpt.get();
                portfolioCache.put(cacheKey, portfolio);
                logger.debug("Cached portfolio: {} -> {}", normalizedId, portfolio.getName());
                return portfolio;
            } else {
                // External service didn't find the portfolio - cache a fallback
                PortfolioDTO fallback = createFallbackPortfolioById(normalizedId);
                portfolioCache.put(cacheKey, fallback);
                logger.debug("Cached fallback portfolio for ID: {}", normalizedId);
                return fallback;
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving portfolio for ID {}: {}", normalizedId, e.getMessage(), e);
            // Return fallback without caching on error
            return createFallbackPortfolioById(normalizedId);
        }
    }

    /**
     * Get portfolio by name, using cache first, then external service
     * @param name The portfolio name to look up
     * @return PortfolioDTO with name and portfolioId, or fallback with name as both fields
     */
    public PortfolioDTO getPortfolioByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            logger.warn("getPortfolioByName called with null or empty name");
            return createFallbackPortfolio(name);
        }
        
        String normalizedName = name.trim();
        
        try {
            // Try cache first
            PortfolioDTO cached = portfolioCache.getIfPresent(normalizedName);
            if (cached != null) {
                logger.debug("Cache hit for portfolio name: {}", normalizedName);
                return cached;
            }
            
            // Cache miss - call external service
            logger.debug("Cache miss for portfolio name: {}, calling external service", normalizedName);
            Optional<PortfolioDTO> portfolioOpt = portfolioServiceClient.findPortfolioByName(normalizedName);
            
            if (portfolioOpt.isPresent()) {
                PortfolioDTO portfolio = portfolioOpt.get();
                portfolioCache.put(normalizedName, portfolio);
                logger.debug("Cached portfolio: {} -> {}", normalizedName, portfolio.getPortfolioId());
                return portfolio;
            } else {
                // External service didn't find the portfolio - cache a fallback
                PortfolioDTO fallback = createFallbackPortfolio(normalizedName);
                portfolioCache.put(normalizedName, fallback);
                logger.debug("Cached fallback portfolio for name: {}", normalizedName);
                return fallback;
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving portfolio for name {}: {}", normalizedName, e.getMessage(), e);
            // Return fallback without caching on error
            return createFallbackPortfolio(normalizedName);
        }
    }
    
    /**
     * Create a fallback PortfolioDTO when external service is unavailable or portfolio not found
     */
    private PortfolioDTO createFallbackPortfolio(String name) {
        String safeName = name != null ? name.trim() : "UNKNOWN";
        return new PortfolioDTO(safeName, safeName);
    }
    
    /**
     * Create a fallback PortfolioDTO by ID when Portfolio Service doesn't support ID lookup
     */
    private PortfolioDTO createFallbackPortfolioById(String portfolioId) {
        String safeId = portfolioId != null ? portfolioId.trim() : "UNKNOWN";
        return new PortfolioDTO(safeId, safeId); // Use ID as both portfolioId and name
    }
    
    /**
     * Manually invalidate cache entry for a portfolio name
     */
    public void invalidate(String name) {
        if (name != null) {
            String normalizedName = name.trim();
            portfolioCache.invalidate(normalizedName);
            logger.debug("Invalidated cache entry for portfolio name: {}", normalizedName);
        }
    }
    
    /**
     * Clear all cache entries
     */
    public void invalidateAll() {
        portfolioCache.invalidateAll();
        logger.debug("Cleared all portfolio cache entries");
    }
    
    /**
     * Get cache statistics for monitoring
     */
    public CacheStats getCacheStats() {
        return portfolioCache.stats();
    }
    
    /**
     * Get cache size
     */
    public long getCacheSize() {
        return portfolioCache.estimatedSize();
    }
    
    /**
     * Get cache hit rate as percentage
     */
    public double getCacheHitRate() {
        CacheStats stats = getCacheStats();
        return stats.hitRate() * 100.0;
    }
    
    /**
     * Log cache statistics
     */
    public void logCacheStats() {
        CacheStats stats = getCacheStats();
        logger.debug("Portfolio Cache Stats - Size: {}, Hit Rate: {:.2f}%, Hits: {}, Misses: {}, Evictions: {}",
                getCacheSize(),
                getCacheHitRate(),
                stats.hitCount(),
                stats.missCount(),
                stats.evictionCount());
    }
} 