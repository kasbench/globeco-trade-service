package org.kasbench.globeco_trade_service.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CircuitBreakerConfig.class)
@TestPropertySource(properties = {
    "resilience4j.circuitbreaker.instances.securityService.failure-rate-threshold=50",
    "resilience4j.circuitbreaker.instances.securityService.wait-duration-in-open-state=30s",
    "resilience4j.circuitbreaker.instances.portfolioService.failure-rate-threshold=50",
    "resilience4j.circuitbreaker.instances.portfolioService.wait-duration-in-open-state=30s"
})
class CircuitBreakerConfigTest {

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Autowired
    private CircuitBreaker securityServiceCircuitBreaker;
    
    @Autowired
    private CircuitBreaker portfolioServiceCircuitBreaker;
    
    @Test
    void circuitBreakerRegistry_ShouldBeConfigured() {
        assertNotNull(circuitBreakerRegistry);
    }
    
    @Test
    void securityServiceCircuitBreaker_ShouldBeConfigured() {
        assertNotNull(securityServiceCircuitBreaker);
        assertEquals("securityService", securityServiceCircuitBreaker.getName());
        assertEquals(CircuitBreaker.State.CLOSED, securityServiceCircuitBreaker.getState());
        
        // Verify configuration
        var config = securityServiceCircuitBreaker.getCircuitBreakerConfig();
        assertEquals(50.0f, config.getFailureRateThreshold());
        assertEquals(10, config.getSlidingWindowSize());
        assertEquals(5, config.getMinimumNumberOfCalls());
    }
    
    @Test
    void portfolioServiceCircuitBreaker_ShouldBeConfigured() {
        assertNotNull(portfolioServiceCircuitBreaker);
        assertEquals("portfolioService", portfolioServiceCircuitBreaker.getName());
        assertEquals(CircuitBreaker.State.CLOSED, portfolioServiceCircuitBreaker.getState());
        
        // Verify configuration
        var config = portfolioServiceCircuitBreaker.getCircuitBreakerConfig();
        assertEquals(50.0f, config.getFailureRateThreshold());
        assertEquals(10, config.getSlidingWindowSize());
        assertEquals(5, config.getMinimumNumberOfCalls());
    }
    
    @Test
    void circuitBreakers_ShouldBeRegisteredInRegistry() {
        assertTrue(circuitBreakerRegistry.getAllCircuitBreakers().stream()
            .anyMatch(cb -> "securityService".equals(cb.getName())));
        assertTrue(circuitBreakerRegistry.getAllCircuitBreakers().stream()
            .anyMatch(cb -> "portfolioService".equals(cb.getName())));
    }
}