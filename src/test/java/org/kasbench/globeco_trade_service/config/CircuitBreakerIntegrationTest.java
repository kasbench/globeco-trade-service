package org.kasbench.globeco_trade_service.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CircuitBreakerConfig.class)
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class CircuitBreakerIntegrationTest {

    @Autowired
    private CircuitBreaker securityServiceCircuitBreaker;
    
    @Autowired
    private CircuitBreaker portfolioServiceCircuitBreaker;
    
    @Test
    void circuitBreakers_ShouldBeConfiguredAndInjected() {
        assertNotNull(securityServiceCircuitBreaker);
        assertNotNull(portfolioServiceCircuitBreaker);
        
        assertEquals("securityService", securityServiceCircuitBreaker.getName());
        assertEquals("portfolioService", portfolioServiceCircuitBreaker.getName());
        
        assertEquals(CircuitBreaker.State.CLOSED, securityServiceCircuitBreaker.getState());
        assertEquals(CircuitBreaker.State.CLOSED, portfolioServiceCircuitBreaker.getState());
    }
    
    @Test
    void circuitBreakers_ShouldHaveCorrectConfiguration() {
        var securityConfig = securityServiceCircuitBreaker.getCircuitBreakerConfig();
        var portfolioConfig = portfolioServiceCircuitBreaker.getCircuitBreakerConfig();
        
        assertEquals(50.0f, securityConfig.getFailureRateThreshold());
        assertEquals(10, securityConfig.getSlidingWindowSize());
        assertEquals(5, securityConfig.getMinimumNumberOfCalls());
        
        assertEquals(50.0f, portfolioConfig.getFailureRateThreshold());
        assertEquals(10, portfolioConfig.getSlidingWindowSize());
        assertEquals(5, portfolioConfig.getMinimumNumberOfCalls());
    }
}