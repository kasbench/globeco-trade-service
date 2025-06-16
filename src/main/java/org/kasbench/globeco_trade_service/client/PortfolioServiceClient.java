package org.kasbench.globeco_trade_service.client;

import org.kasbench.globeco_trade_service.dto.PortfolioDTO;
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
public class PortfolioServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(PortfolioServiceClient.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${external.portfolio-service.base-url:http://globeco-portfolio-service:8000}")
    private String portfolioServiceBaseUrl;
    
    @Value("${external.portfolio-service.timeout:5000}")
    private int timeoutMs;
    
    public PortfolioServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Search for portfolios by name using the Portfolio Service API
     * @param name The portfolio name to search for
     * @return Optional containing PortfolioDTO if found, empty if not found or service unavailable
     */
    @Retryable(
        value = {ResourceAccessException.class, HttpServerErrorException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Optional<PortfolioDTO> findPortfolioByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            logger.warn("findPortfolioByName called with null or empty name");
            return Optional.empty();
        }
        
        try {
            String url = portfolioServiceBaseUrl + "/api/v1/portfolios?name=" + name.trim();
            logger.debug("Calling Portfolio Service: {}", url);
            
            ResponseEntity<PortfolioSearchResponse> response = restTemplate.getForEntity(
                url, PortfolioSearchResponse.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                PortfolioSearchResponse searchResponse = response.getBody();
                if (searchResponse.getPortfolios() != null && !searchResponse.getPortfolios().isEmpty()) {
                    PortfolioDTO portfolio = searchResponse.getPortfolios().get(0);
                    logger.debug("Found portfolio: {} -> {}", name, portfolio.getName());
                    return Optional.of(portfolio);
                } else {
                    logger.debug("No portfolios found for name: {}", name);
                    return Optional.empty();
                }
            } else {
                logger.warn("Unexpected response from Portfolio Service: {}", response.getStatusCode());
                return Optional.empty();
            }
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                logger.debug("Portfolio not found for name: {}", name);
                return Optional.empty();
            } else {
                logger.error("Client error calling Portfolio Service for name {}: {} - {}", 
                    name, e.getStatusCode(), e.getResponseBodyAsString());
                return Optional.empty();
            }
        } catch (HttpServerErrorException e) {
            logger.error("Server error calling Portfolio Service for name {}: {} - {}", 
                name, e.getStatusCode(), e.getResponseBodyAsString());
            throw e; // Will trigger retry
        } catch (ResourceAccessException e) {
            logger.error("Network error calling Portfolio Service for name {}: {}", name, e.getMessage());
            throw e; // Will trigger retry
        } catch (Exception e) {
            logger.error("Unexpected error calling Portfolio Service for name {}: {}", name, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Search for portfolios by partial name match using the Portfolio Service API
     * @param namePattern The partial name pattern to search for
     * @return List of matching PortfolioDTOs, empty list if none found or service unavailable
     */
    @Retryable(
        value = {ResourceAccessException.class, HttpServerErrorException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<PortfolioDTO> findPortfoliosByNameLike(String namePattern) {
        if (namePattern == null || namePattern.trim().isEmpty()) {
            logger.warn("findPortfoliosByNameLike called with null or empty pattern");
            return List.of();
        }
        
        try {
            String url = portfolioServiceBaseUrl + "/api/v1/portfolios?name_like=" + namePattern.trim();
            logger.debug("Calling Portfolio Service: {}", url);
            
            ResponseEntity<PortfolioSearchResponse> response = restTemplate.getForEntity(
                url, PortfolioSearchResponse.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                PortfolioSearchResponse searchResponse = response.getBody();
                if (searchResponse.getPortfolios() != null) {
                    logger.debug("Found {} portfolios for pattern: {}", 
                        searchResponse.getPortfolios().size(), namePattern);
                    return searchResponse.getPortfolios();
                } else {
                    logger.debug("No portfolios found for pattern: {}", namePattern);
                    return List.of();
                }
            } else {
                logger.warn("Unexpected response from Portfolio Service: {}", response.getStatusCode());
                return List.of();
            }
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                logger.debug("No portfolios found for pattern: {}", namePattern);
                return List.of();
            } else {
                logger.error("Client error calling Portfolio Service for pattern {}: {} - {}", 
                    namePattern, e.getStatusCode(), e.getResponseBodyAsString());
                return List.of();
            }
        } catch (HttpServerErrorException e) {
            logger.error("Server error calling Portfolio Service for pattern {}: {} - {}", 
                namePattern, e.getStatusCode(), e.getResponseBodyAsString());
            throw e; // Will trigger retry
        } catch (ResourceAccessException e) {
            logger.error("Network error calling Portfolio Service for pattern {}: {}", namePattern, e.getMessage());
            throw e; // Will trigger retry
        } catch (Exception e) {
            logger.error("Unexpected error calling Portfolio Service for pattern {}: {}", namePattern, e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * Response wrapper for Portfolio Service API
     */
    public static class PortfolioSearchResponse {
        private List<PortfolioDTO> portfolios;
        private PaginationInfo pagination;
        
        public List<PortfolioDTO> getPortfolios() {
            return portfolios;
        }
        
        public void setPortfolios(List<PortfolioDTO> portfolios) {
            this.portfolios = portfolios;
        }
        
        public PaginationInfo getPagination() {
            return pagination;
        }
        
        public void setPagination(PaginationInfo pagination) {
            this.pagination = pagination;
        }
    }
    
    /**
     * Pagination information from Portfolio Service response
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