package org.kasbench.globeco_trade_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@Configuration
@EnableRetry
public class RetryConfig {
    
    /**
     * RetryTemplate for external service calls with exponential backoff
     * - Maximum 3 attempts
     * - Initial delay: 1 second
     * - Multiplier: 2
     * - Maximum delay: 10 seconds
     * - Retries on network timeouts, connection failures, and 5xx server errors
     */
    @Bean
    public RetryTemplate retryTemplate() {
        return RetryTemplate.builder()
            .maxAttempts(3)
            .exponentialBackoff(1000, 2, 10000)
            .retryOn(ResourceAccessException.class)
            .retryOn(HttpServerErrorException.class)
            .build();
    }
} 