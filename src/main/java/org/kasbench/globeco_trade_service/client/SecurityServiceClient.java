package org.kasbench.globeco_trade_service.client;

import org.kasbench.globeco_trade_service.dto.SecurityDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Component
public class SecurityServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(SecurityServiceClient.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${external.security-service.base-url:http://globeco-security-service:8000}")
    private String securityServiceBaseUrl;
    
    @Value("${external.security-service.timeout:5000}")
    private int timeoutMs;
    
    public SecurityServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Search for securities by ticker using the v2 API
     * @param ticker The security ticker to search for
     * @return Optional containing SecurityDTO if found, empty if not found or service unavailable
     */
    @Retryable(
        value = {ResourceAccessException.class, HttpServerErrorException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Optional<SecurityDTO> findSecurityByTicker(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            logger.warn("findSecurityByTicker called with null or empty ticker");
            return Optional.empty();
        }
        
        try {
            String url = securityServiceBaseUrl + "/api/v2/securities?ticker=" + ticker.trim();
            logger.debug("Calling Security Service: {}", url);
            
            ResponseEntity<SecuritySearchResponse> response = restTemplate.getForEntity(
                url, SecuritySearchResponse.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                SecuritySearchResponse searchResponse = response.getBody();
                if (searchResponse.getSecurities() != null && !searchResponse.getSecurities().isEmpty()) {
                    SecurityDTO security = searchResponse.getSecurities().get(0);
                    logger.debug("Found security: {} -> {}", ticker, security.getTicker());
                    return Optional.of(security);
                } else {
                    logger.debug("No securities found for ticker: {}", ticker);
                    return Optional.empty();
                }
            } else {
                logger.warn("Unexpected response from Security Service: {}", response.getStatusCode());
                return Optional.empty();
            }
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                logger.debug("Security not found for ticker: {}", ticker);
                return Optional.empty();
            } else {
                logger.error("Client error calling Security Service for ticker {}: {} - {}", 
                    ticker, e.getStatusCode(), e.getResponseBodyAsString());
                return Optional.empty();
            }
        } catch (HttpServerErrorException e) {
            logger.error("Server error calling Security Service for ticker {}: {} - {}", 
                ticker, e.getStatusCode(), e.getResponseBodyAsString());
            throw e; // Will trigger retry
        } catch (ResourceAccessException e) {
            logger.error("Network error calling Security Service for ticker {}: {}", ticker, e.getMessage());
            throw e; // Will trigger retry
        } catch (Exception e) {
            logger.error("Unexpected error calling Security Service for ticker {}: {}", ticker, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Search for securities by partial ticker match using the v2 API
     * @param tickerPattern The partial ticker pattern to search for
     * @return List of matching SecurityDTOs, empty list if none found or service unavailable
     */
    @Retryable(
        value = {ResourceAccessException.class, HttpServerErrorException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<SecurityDTO> findSecuritiesByTickerLike(String tickerPattern) {
        if (tickerPattern == null || tickerPattern.trim().isEmpty()) {
            logger.warn("findSecuritiesByTickerLike called with null or empty pattern");
            return List.of();
        }
        
        try {
            String url = securityServiceBaseUrl + "/api/v2/securities?ticker_like=" + tickerPattern.trim();
            logger.debug("Calling Security Service: {}", url);
            
            ResponseEntity<SecuritySearchResponse> response = restTemplate.getForEntity(
                url, SecuritySearchResponse.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                SecuritySearchResponse searchResponse = response.getBody();
                if (searchResponse.getSecurities() != null) {
                    logger.debug("Found {} securities for pattern: {}", 
                        searchResponse.getSecurities().size(), tickerPattern);
                    return searchResponse.getSecurities();
                } else {
                    logger.debug("No securities found for pattern: {}", tickerPattern);
                    return List.of();
                }
            } else {
                logger.warn("Unexpected response from Security Service: {}", response.getStatusCode());
                return List.of();
            }
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                logger.debug("No securities found for pattern: {}", tickerPattern);
                return List.of();
            } else {
                logger.error("Client error calling Security Service for pattern {}: {} - {}", 
                    tickerPattern, e.getStatusCode(), e.getResponseBodyAsString());
                return List.of();
            }
        } catch (HttpServerErrorException e) {
            logger.error("Server error calling Security Service for pattern {}: {} - {}", 
                tickerPattern, e.getStatusCode(), e.getResponseBodyAsString());
            throw e; // Will trigger retry
        } catch (ResourceAccessException e) {
            logger.error("Network error calling Security Service for pattern {}: {}", tickerPattern, e.getMessage());
            throw e; // Will trigger retry
        } catch (Exception e) {
            logger.error("Unexpected error calling Security Service for pattern {}: {}", tickerPattern, e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * Response wrapper for Security Service v2 API
     */
    public static class SecuritySearchResponse {
        private List<SecurityDTO> securities;
        private PaginationInfo pagination;
        
        public List<SecurityDTO> getSecurities() {
            return securities;
        }
        
        public void setSecurities(List<SecurityDTO> securities) {
            this.securities = securities;
        }
        
        public PaginationInfo getPagination() {
            return pagination;
        }
        
        public void setPagination(PaginationInfo pagination) {
            this.pagination = pagination;
        }
    }
    
    /**
     * Pagination information from Security Service response
     */
    public static class PaginationInfo {
        private int totalElements;
        private int totalPages;
        private int currentPage;
        private int pageSize;
        private boolean hasNext;
        private boolean hasPrevious;
        
        // Getters and setters
        public int getTotalElements() { return totalElements; }
        public void setTotalElements(int totalElements) { this.totalElements = totalElements; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }
        public boolean isHasNext() { return hasNext; }
        public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
        public boolean isHasPrevious() { return hasPrevious; }
        public void setHasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; }
    }
} 