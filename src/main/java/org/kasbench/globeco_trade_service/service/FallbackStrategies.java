package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.dto.PortfolioDTO;
import org.kasbench.globeco_trade_service.dto.SecurityDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FallbackStrategies {
    
    private static final Logger logger = LoggerFactory.getLogger(FallbackStrategies.class);
    
    /**
     * Fallback strategy for security service when circuit breaker is open
     */
    public SecurityDTO getSecurityFallback(String securityId, Exception ex) {
        logger.warn("Security service fallback triggered for ID: {}", securityId, ex);
        return new SecurityDTO(securityId, "UNKNOWN");
    }
    
    /**
     * Fallback strategy for security service by ticker when circuit breaker is open
     */
    public SecurityDTO getSecurityByTickerFallback(String ticker, Exception ex) {
        logger.warn("Security service fallback triggered for ticker: {}", ticker, ex);
        return new SecurityDTO("UNKNOWN", ticker);
    }
    
    /**
     * Fallback strategy for portfolio service when circuit breaker is open
     */
    public PortfolioDTO getPortfolioFallback(String portfolioId, Exception ex) {
        logger.warn("Portfolio service fallback triggered for ID: {}", portfolioId, ex);
        return new PortfolioDTO(portfolioId, "Service Unavailable");
    }
    
    /**
     * Fallback strategy for portfolio service by name when circuit breaker is open
     */
    public PortfolioDTO getPortfolioByNameFallback(String name, Exception ex) {
        logger.warn("Portfolio service fallback triggered for name: {}", name, ex);
        return new PortfolioDTO("UNKNOWN", name);
    }
}