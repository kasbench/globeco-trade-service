package org.kasbench.globeco_trade_service.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.kasbench.globeco_trade_service.client.PortfolioServiceClient;
import org.kasbench.globeco_trade_service.client.SecurityServiceClient;
import org.kasbench.globeco_trade_service.dto.PortfolioDTO;
import org.kasbench.globeco_trade_service.dto.SecurityDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Optimized cache configuration with improved settings for performance optimization.
 * Implements requirements 7.1, 7.2, and 7.4 from the performance optimization spec.
 */
@Configuration
public class OptimizedCacheConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(OptimizedCacheConfig.class);
    
    private final SecurityServiceClient securityServiceClient;
    private final PortfolioServiceClient portfolioServiceClient;
    
    public OptimizedCacheConfig(SecurityServiceClient securityServiceClient, 
                               PortfolioServiceClient portfolioServiceClient) {
        this.securityServiceClient = securityServiceClient;
        this.portfolioServiceClient = portfolioServiceClient;
    }
    
    /**
     * Optimized security cache with increased size (2000) and TTL (10 minutes)
     * Requirement 7.1: Configure security cache with increased size and TTL
     */
    @Bean("optimizedSecurityCache")
    public Cache<String, SecurityDTO> optimizedSecurityCache() {
        logger.info("Initializing optimized security cache with size=2000, TTL=10min");
        
        return Caffeine.newBuilder()
                .maximumSize(2000)                          // Increased from 1000
                .expireAfterWrite(Duration.ofMinutes(10))   // Increased from 5 minutes
                .recordStats()                              // Enable statistics collection
                .removalListener(createSecurityRemovalListener())
                .build();
    }
    
    /**
     * Optimized portfolio cache with extended TTL (15 minutes)
     * Requirement 7.2: Configure portfolio cache with extended TTL
     */
    @Bean("optimizedPortfolioCache")
    public Cache<String, PortfolioDTO> optimizedPortfolioCache() {
        logger.info("Initializing optimized portfolio cache with size=1000, TTL=15min");
        
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(15))   // Increased from 5 minutes
                .recordStats()                              // Enable statistics collection
                .removalListener(createPortfolioRemovalListener())
                .build();
    }
    
    /**
     * Cache warming strategy on application startup
     * Requirement 7.4: Implement cache warming strategy on application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmupCaches() {
        logger.info("Starting cache warmup process...");
        
        CompletableFuture<Void> securityWarmup = CompletableFuture.runAsync(this::warmupSecurityCache);
        CompletableFuture<Void> portfolioWarmup = CompletableFuture.runAsync(this::warmupPortfolioCache);
        
        CompletableFuture.allOf(securityWarmup, portfolioWarmup)
                .thenRun(() -> logger.info("Cache warmup completed successfully"))
                .exceptionally(throwable -> {
                    logger.error("Cache warmup failed", throwable);
                    return null;
                });
    }
    
    /**
     * Warm up security cache with frequently accessed securities
     */
    private void warmupSecurityCache() {
        try {
            logger.info("Warming up security cache...");
            Cache<String, SecurityDTO> securityCache = optimizedSecurityCache();
            
            // Common securities that are frequently accessed
            List<String> commonSecurities = Arrays.asList(
                "AAPL", "GOOGL", "MSFT", "AMZN", "TSLA", "META", "NVDA", "NFLX", "AMD", "INTC",
                "JPM", "BAC", "WFC", "GS", "MS", "C", "USB", "PNC", "TFC", "COF",
                "SPY", "QQQ", "IWM", "VTI", "VOO", "VEA", "VWO", "AGG", "BND", "TLT"
            );
            
            int warmedCount = 0;
            for (String ticker : commonSecurities) {
                try {
                    Optional<SecurityDTO> security = securityServiceClient.findSecurityByTicker(ticker);
                    if (security.isPresent()) {
                        securityCache.put(ticker, security.get());
                        warmedCount++;
                        logger.debug("Warmed security cache for ticker: {}", ticker);
                    }
                } catch (Exception e) {
                    logger.debug("Failed to warm cache for security {}: {}", ticker, e.getMessage());
                }
                
                // Small delay to avoid overwhelming the external service
                Thread.sleep(10);
            }
            
            logger.info("Security cache warmup completed. Warmed {} securities", warmedCount);
            
        } catch (Exception e) {
            logger.error("Security cache warmup failed", e);
        }
    }
    
    /**
     * Warm up portfolio cache with frequently accessed portfolios
     */
    private void warmupPortfolioCache() {
        try {
            logger.info("Warming up portfolio cache...");
            Cache<String, PortfolioDTO> portfolioCache = optimizedPortfolioCache();
            
            // Common portfolio names that are frequently accessed
            List<String> commonPortfolios = Arrays.asList(
                "MAIN", "TRADING", "HEDGE", "EQUITY", "FIXED_INCOME", "ALTERNATIVES",
                "GROWTH", "VALUE", "INCOME", "BALANCED", "CONSERVATIVE", "AGGRESSIVE",
                "TECH", "HEALTHCARE", "FINANCIAL", "ENERGY", "CONSUMER", "INDUSTRIAL"
            );
            
            int warmedCount = 0;
            for (String portfolioName : commonPortfolios) {
                try {
                    Optional<PortfolioDTO> portfolio = portfolioServiceClient.findPortfolioByName(portfolioName);
                    if (portfolio.isPresent()) {
                        portfolioCache.put(portfolioName, portfolio.get());
                        warmedCount++;
                        logger.debug("Warmed portfolio cache for name: {}", portfolioName);
                    }
                } catch (Exception e) {
                    logger.debug("Failed to warm cache for portfolio {}: {}", portfolioName, e.getMessage());
                }
                
                // Small delay to avoid overwhelming the external service
                Thread.sleep(10);
            }
            
            logger.info("Portfolio cache warmup completed. Warmed {} portfolios", warmedCount);
            
        } catch (Exception e) {
            logger.error("Portfolio cache warmup failed", e);
        }
    }
    
    /**
     * Create removal listener for security cache to log evictions
     */
    private RemovalListener<String, SecurityDTO> createSecurityRemovalListener() {
        return (key, value, cause) -> {
            if (cause.wasEvicted()) {
                logger.debug("Security cache entry evicted: key={}, cause={}", key, cause);
            }
        };
    }
    
    /**
     * Create removal listener for portfolio cache to log evictions
     */
    private RemovalListener<String, PortfolioDTO> createPortfolioRemovalListener() {
        return (key, value, cause) -> {
            if (cause.wasEvicted()) {
                logger.debug("Portfolio cache entry evicted: key={}, cause={}", key, cause);
            }
        };
    }
}