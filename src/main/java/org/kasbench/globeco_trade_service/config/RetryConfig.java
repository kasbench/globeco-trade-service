package org.kasbench.globeco_trade_service.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

@Configuration
@EnableRetry
public class RetryConfig {
    
    @Value("${execution.service.retry.max-attempts:5}")
    private int executionMaxAttempts = 5;
    
    @Value("${execution.service.retry.initial-delay:1000}")
    private long executionInitialDelay = 1000;
    
    @Value("${execution.service.retry.multiplier:2}")
    private int executionMultiplier = 2;
    
    @Value("${execution.service.retry.max-delay:30000}")
    private long executionMaxDelay = 30000;
    
    /**
     * Default RetryTemplate for general external service calls
     * - Maximum 3 attempts
     * - Initial delay: 1 second
     * - Multiplier: 2
     * - Maximum delay: 10 seconds
     * - Retries on network timeouts, connection failures, and 5xx server errors
     */
    @Bean
    @Primary
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(name = "retryTemplate")
    public RetryTemplate retryTemplate() {
        return RetryTemplate.builder()
            .maxAttempts(3)
            .exponentialBackoff(1000, 2, 10000)
            .retryOn(ResourceAccessException.class)
            .retryOn(HttpServerErrorException.class)
            .build();
    }
    
    /**
     * Enhanced RetryTemplate specifically for execution service calls
     * - Configurable maximum attempts (default: 5)
     * - Configurable exponential backoff (default: 1s initial, 2x multiplier, 30s max)
     * - Retries on various network and timeout exceptions
     * - Retries on 5xx server errors but not 4xx client errors
     */
    @Bean
    @Qualifier("executionServiceRetryTemplate")
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(name = "executionServiceRetryTemplate")
    public RetryTemplate executionServiceRetryTemplate() {
        return RetryTemplate.builder()
            .maxAttempts(executionMaxAttempts)
            .exponentialBackoff(executionInitialDelay, executionMultiplier, executionMaxDelay)
            .retryOn(ResourceAccessException.class)           // Network timeouts, connection refused
            .retryOn(SocketTimeoutException.class)            // Socket timeouts
            .retryOn(ConnectException.class)                  // Connection failures
            .retryOn(HttpServerErrorException.class)          // 5xx server errors
            .build();
    }
} 