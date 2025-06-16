package org.kasbench.globeco_trade_service.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.kasbench.globeco_trade_service.client.SecurityServiceClient;
import org.kasbench.globeco_trade_service.dto.SecurityDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class SecurityCacheService {
    private static final Logger logger = LoggerFactory.getLogger(SecurityCacheService.class);
    
    private final SecurityServiceClient securityServiceClient;
    private final Cache<String, SecurityDTO> securityCache;
    
    public SecurityCacheService(
            SecurityServiceClient securityServiceClient,
            @Value("${cache.security.ttl-minutes:5}") int ttlMinutes,
            @Value("${cache.security.max-size:1000}") int maxSize) {
        this.securityServiceClient = securityServiceClient;
        this.securityCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(Duration.ofMinutes(ttlMinutes))
                .recordStats()
                .build();
        
        logger.info("SecurityCacheService initialized with TTL: {} minutes, Max Size: {}", ttlMinutes, maxSize);
    }
    
    /**
     * Get security by ticker, using cache first, then external service
     * @param ticker The security ticker to look up
     * @return SecurityDTO with ticker and securityId, or fallback with ticker as both fields
     */
    public SecurityDTO getSecurityByTicker(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            logger.warn("getSecurityByTicker called with null or empty ticker");
            return createFallbackSecurity(ticker);
        }
        
        String normalizedTicker = ticker.trim().toUpperCase();
        
        try {
            // Try cache first
            SecurityDTO cached = securityCache.getIfPresent(normalizedTicker);
            if (cached != null) {
                logger.debug("Cache hit for ticker: {}", normalizedTicker);
                return cached;
            }
            
            // Cache miss - call external service
            logger.debug("Cache miss for ticker: {}, calling external service", normalizedTicker);
            Optional<SecurityDTO> securityOpt = securityServiceClient.findSecurityByTicker(normalizedTicker);
            
            if (securityOpt.isPresent()) {
                SecurityDTO security = securityOpt.get();
                securityCache.put(normalizedTicker, security);
                logger.debug("Cached security: {} -> {}", normalizedTicker, security.getSecurityId());
                return security;
            } else {
                // External service didn't find the security - cache a fallback
                SecurityDTO fallback = createFallbackSecurity(normalizedTicker);
                securityCache.put(normalizedTicker, fallback);
                logger.debug("Cached fallback security for ticker: {}", normalizedTicker);
                return fallback;
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving security for ticker {}: {}", normalizedTicker, e.getMessage(), e);
            // Return fallback without caching on error
            return createFallbackSecurity(normalizedTicker);
        }
    }
    
    /**
     * Create a fallback SecurityDTO when external service is unavailable or security not found
     */
    private SecurityDTO createFallbackSecurity(String ticker) {
        String safeTicker = ticker != null ? ticker.trim() : "UNKNOWN";
        return new SecurityDTO(safeTicker, safeTicker);
    }
    
    /**
     * Manually invalidate cache entry for a ticker
     */
    public void invalidate(String ticker) {
        if (ticker != null) {
            String normalizedTicker = ticker.trim().toUpperCase();
            securityCache.invalidate(normalizedTicker);
            logger.debug("Invalidated cache entry for ticker: {}", normalizedTicker);
        }
    }
    
    /**
     * Clear all cache entries
     */
    public void invalidateAll() {
        securityCache.invalidateAll();
        logger.info("Cleared all security cache entries");
    }
    
    /**
     * Get cache statistics for monitoring
     */
    public CacheStats getCacheStats() {
        return securityCache.stats();
    }
    
    /**
     * Get cache size
     */
    public long getCacheSize() {
        return securityCache.estimatedSize();
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
        logger.info("Security Cache Stats - Size: {}, Hit Rate: {:.2f}%, Hits: {}, Misses: {}, Evictions: {}",
                getCacheSize(),
                getCacheHitRate(),
                stats.hitCount(),
                stats.missCount(),
                stats.evictionCount());
    }
} 