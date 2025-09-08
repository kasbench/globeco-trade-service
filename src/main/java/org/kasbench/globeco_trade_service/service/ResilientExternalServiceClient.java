package org.kasbench.globeco_trade_service.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.kasbench.globeco_trade_service.client.PortfolioServiceClient;
import org.kasbench.globeco_trade_service.client.SecurityServiceClient;
import org.kasbench.globeco_trade_service.dto.PortfolioDTO;
import org.kasbench.globeco_trade_service.dto.SecurityDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class ResilientExternalServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(ResilientExternalServiceClient.class);
    
    private final SecurityServiceClient securityServiceClient;
    private final PortfolioServiceClient portfolioServiceClient;
    private final CircuitBreaker securityServiceCircuitBreaker;
    private final CircuitBreaker portfolioServiceCircuitBreaker;
    private final SecurityCacheService securityCacheService;
    private final PortfolioCacheService portfolioCacheService;
    
    public ResilientExternalServiceClient(
            SecurityServiceClient securityServiceClient,
            PortfolioServiceClient portfolioServiceClient,
            CircuitBreaker securityServiceCircuitBreaker,
            CircuitBreaker portfolioServiceCircuitBreaker,
            SecurityCacheService securityCacheService,
            PortfolioCacheService portfolioCacheService) {
        this.securityServiceClient = securityServiceClient;
        this.portfolioServiceClient = portfolioServiceClient;
        this.securityServiceCircuitBreaker = securityServiceCircuitBreaker;
        this.portfolioServiceCircuitBreaker = portfolioServiceCircuitBreaker;
        this.securityCacheService = securityCacheService;
        this.portfolioCacheService = portfolioCacheService;
    }
    
    /**
     * Get security by ID with circuit breaker protection and cache fallback
     */
    public Optional<SecurityDTO> getSecurityById(String securityId) {
        try {
            Supplier<Optional<SecurityDTO>> securitySupplier = () -> 
                securityServiceClient.findSecurityById(securityId);
            
            return securityServiceCircuitBreaker.executeSupplier(securitySupplier);
        } catch (CallNotPermittedException e) {
            logger.warn("Security service circuit breaker is open for ID: {}, attempting cache fallback", securityId);
            return getSecurityFallback(securityId, e);
        } catch (Exception e) {
            logger.warn("Security service call failed for ID: {}, attempting cache fallback", securityId, e);
            return getSecurityFallback(securityId, e);
        }
    }
    
    /**
     * Get security by ticker with circuit breaker protection and cache fallback
     */
    public Optional<SecurityDTO> getSecurityByTicker(String ticker) {
        try {
            Supplier<Optional<SecurityDTO>> securitySupplier = () -> 
                securityServiceClient.findSecurityByTicker(ticker);
            
            return securityServiceCircuitBreaker.executeSupplier(securitySupplier);
        } catch (CallNotPermittedException e) {
            logger.warn("Security service circuit breaker is open for ticker: {}, attempting cache fallback", ticker);
            return getSecurityByTickerFallback(ticker, e);
        } catch (Exception e) {
            logger.warn("Security service call failed for ticker: {}, attempting cache fallback", ticker, e);
            return getSecurityByTickerFallback(ticker, e);
        }
    }
    
    /**
     * Search securities by ticker pattern with circuit breaker protection
     */
    public List<SecurityDTO> getSecuritiesByTickerLike(String tickerPattern) {
        try {
            Supplier<List<SecurityDTO>> securitySupplier = () -> 
                securityServiceClient.findSecuritiesByTickerLike(tickerPattern);
            
            return securityServiceCircuitBreaker.executeSupplier(securitySupplier);
        } catch (CallNotPermittedException e) {
            logger.warn("Security service circuit breaker is open for pattern: {}, returning empty list", tickerPattern);
            return List.of();
        } catch (Exception e) {
            logger.warn("Security service call failed for pattern: {}, returning empty list", tickerPattern, e);
            return List.of();
        }
    }
    
    /**
     * Get portfolio by ID with circuit breaker protection and cache fallback
     */
    public Optional<PortfolioDTO> getPortfolioById(String portfolioId) {
        try {
            Supplier<Optional<PortfolioDTO>> portfolioSupplier = () -> 
                portfolioServiceClient.findPortfolioById(portfolioId);
            
            return portfolioServiceCircuitBreaker.executeSupplier(portfolioSupplier);
        } catch (CallNotPermittedException e) {
            logger.warn("Portfolio service circuit breaker is open for ID: {}, attempting cache fallback", portfolioId);
            return getPortfolioFallback(portfolioId, e);
        } catch (Exception e) {
            logger.warn("Portfolio service call failed for ID: {}, attempting cache fallback", portfolioId, e);
            return getPortfolioFallback(portfolioId, e);
        }
    }
    
    /**
     * Get portfolio by name with circuit breaker protection and cache fallback
     */
    public Optional<PortfolioDTO> getPortfolioByName(String name) {
        try {
            Supplier<Optional<PortfolioDTO>> portfolioSupplier = () -> 
                portfolioServiceClient.findPortfolioByName(name);
            
            return portfolioServiceCircuitBreaker.executeSupplier(portfolioSupplier);
        } catch (CallNotPermittedException e) {
            logger.warn("Portfolio service circuit breaker is open for name: {}, attempting cache fallback", name);
            return getPortfolioByNameFallback(name, e);
        } catch (Exception e) {
            logger.warn("Portfolio service call failed for name: {}, attempting cache fallback", name, e);
            return getPortfolioByNameFallback(name, e);
        }
    }
    
    /**
     * Search portfolios by name pattern with circuit breaker protection
     */
    public List<PortfolioDTO> getPortfoliosByNameLike(String namePattern) {
        try {
            Supplier<List<PortfolioDTO>> portfolioSupplier = () -> 
                portfolioServiceClient.findPortfoliosByNameLike(namePattern);
            
            return portfolioServiceCircuitBreaker.executeSupplier(portfolioSupplier);
        } catch (CallNotPermittedException e) {
            logger.warn("Portfolio service circuit breaker is open for pattern: {}, returning empty list", namePattern);
            return List.of();
        } catch (Exception e) {
            logger.warn("Portfolio service call failed for pattern: {}, returning empty list", namePattern, e);
            return List.of();
        }
    }
    
    /**
     * Fallback strategy for security service by ID - try cache first, then return placeholder
     */
    private Optional<SecurityDTO> getSecurityFallback(String securityId, Throwable ex) {
        try {
            // Try to get from cache first
            SecurityDTO cachedSecurity = securityCacheService.getSecurityById(securityId);
            if (cachedSecurity != null) {
                logger.info("Security service fallback: found cached security for ID: {}", securityId);
                return Optional.of(cachedSecurity);
            }
        } catch (Exception cacheEx) {
            logger.warn("Cache fallback also failed for security ID: {}", securityId, cacheEx);
        }
        
        // Return placeholder security if cache also fails
        logger.warn("Security service and cache fallback failed for ID: {}, returning placeholder", securityId);
        return Optional.of(createPlaceholderSecurity(securityId, "UNKNOWN"));
    }
    
    /**
     * Fallback strategy for security service by ticker - try cache lookup by ticker
     */
    private Optional<SecurityDTO> getSecurityByTickerFallback(String ticker, Throwable ex) {
        try {
            // For ticker-based lookup, we can't easily fallback to cache since cache is ID-based
            // In a real implementation, you might maintain a ticker->ID mapping cache
            logger.warn("Security service fallback: no cache fallback available for ticker-based lookup: {}", ticker);
        } catch (Exception cacheEx) {
            logger.warn("Cache fallback failed for security ticker: {}", ticker, cacheEx);
        }
        
        // Return placeholder security
        logger.warn("Security service fallback for ticker: {}, returning placeholder", ticker);
        return Optional.of(createPlaceholderSecurity("UNKNOWN", ticker));
    }
    
    /**
     * Fallback strategy for portfolio service by ID - try cache first, then return placeholder
     */
    private Optional<PortfolioDTO> getPortfolioFallback(String portfolioId, Throwable ex) {
        try {
            // Try to get from cache first
            PortfolioDTO cachedPortfolio = portfolioCacheService.getPortfolioById(portfolioId);
            if (cachedPortfolio != null) {
                logger.info("Portfolio service fallback: found cached portfolio for ID: {}", portfolioId);
                return Optional.of(cachedPortfolio);
            }
        } catch (Exception cacheEx) {
            logger.warn("Cache fallback also failed for portfolio ID: {}", portfolioId, cacheEx);
        }
        
        // Return placeholder portfolio if cache also fails
        logger.warn("Portfolio service and cache fallback failed for ID: {}, returning placeholder", portfolioId);
        return Optional.of(createPlaceholderPortfolio(portfolioId, "Service Unavailable"));
    }
    
    /**
     * Fallback strategy for portfolio service by name - no cache fallback available
     */
    private Optional<PortfolioDTO> getPortfolioByNameFallback(String name, Throwable ex) {
        // For name-based lookup, we can't easily fallback to cache since cache is ID-based
        logger.warn("Portfolio service fallback: no cache fallback available for name-based lookup: {}", name);
        return Optional.of(createPlaceholderPortfolio("UNKNOWN", name));
    }
    
    /**
     * Create a placeholder security for fallback scenarios
     */
    private SecurityDTO createPlaceholderSecurity(String securityId, String ticker) {
        return new SecurityDTO(securityId, ticker);
    }
    
    /**
     * Create a placeholder portfolio for fallback scenarios
     */
    private PortfolioDTO createPlaceholderPortfolio(String portfolioId, String name) {
        return new PortfolioDTO(portfolioId, "Service Unavailable - " + name);
    }
    
    /**
     * Get circuit breaker state for monitoring
     */
    public String getSecurityServiceCircuitBreakerState() {
        return securityServiceCircuitBreaker.getState().toString();
    }
    
    /**
     * Get circuit breaker state for monitoring
     */
    public String getPortfolioServiceCircuitBreakerState() {
        return portfolioServiceCircuitBreaker.getState().toString();
    }
}