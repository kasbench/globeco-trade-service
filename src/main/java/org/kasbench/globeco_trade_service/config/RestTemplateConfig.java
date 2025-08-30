package org.kasbench.globeco_trade_service.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {
    
    @Value("${external.service.connect-timeout:5000}")
    private int connectTimeoutMs = 5000;
    
    @Value("${external.service.read-timeout:5000}")
    private int readTimeoutMs = 5000;
    
    @Value("${execution.service.connect-timeout:15000}")
    private int executionConnectTimeoutMs = 15000;
    
    @Value("${execution.service.read-timeout:30000}")
    private int executionReadTimeoutMs = 30000;
    
    @Bean
    @Primary
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(name = "restTemplate")
    public RestTemplate restTemplate(@org.springframework.beans.factory.annotation.Autowired(required = false) RestTemplateBuilder builder) {
        if (builder != null) {
            return builder
                    .requestFactory(this::createRequestFactory)
                    .build();
        } else {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.setRequestFactory(createRequestFactory());
            return restTemplate;
        }
    }
    
    @Bean
    @Qualifier("executionServiceRestTemplate")
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(name = "executionServiceRestTemplate")
    public RestTemplate executionServiceRestTemplate(@org.springframework.beans.factory.annotation.Autowired(required = false) RestTemplateBuilder builder) {
        if (builder != null) {
            return builder
                    .requestFactory(this::createExecutionServiceRequestFactory)
                    .build();
        } else {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.setRequestFactory(createExecutionServiceRequestFactory());
            return restTemplate;
        }
    }
    
    private ClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return factory;
    }
    
    private ClientHttpRequestFactory createExecutionServiceRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(executionConnectTimeoutMs);
        factory.setReadTimeout(executionReadTimeoutMs);
        return factory;
    }
} 