package org.kasbench.globeco_trade_service.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;
import org.mockito.Mockito;

@TestConfiguration
@EnableRetry
public class TestConfig {
    
    @Bean(name = "executionServiceRestTemplate")
    public RestTemplate executionServiceRestTemplate() {
        return Mockito.mock(RestTemplate.class);
    }
    
    @Bean(name = "executionServiceRetryTemplate")
    public RetryTemplate executionServiceRetryTemplate() {
        // For tests, use a RetryTemplate that doesn't retry (maxAttempts = 1)
        // This allows the mocked RestTemplate exceptions to be thrown directly
        return RetryTemplate.builder()
            .maxAttempts(1)
            .build();
    }
    
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    @Bean
    @Primary
    public RetryTemplate retryTemplate() {
        return RetryTemplate.builder()
            .maxAttempts(3)
            .exponentialBackoff(1000, 2, 10000)
            .build();
    }
}