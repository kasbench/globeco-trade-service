package org.kasbench.globeco_trade_service.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kasbench.globeco_trade_service.client.PortfolioServiceClient;
import org.kasbench.globeco_trade_service.client.SecurityServiceClient;
import org.kasbench.globeco_trade_service.dto.PortfolioDTO;
import org.kasbench.globeco_trade_service.dto.SecurityDTO;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResilientExternalServiceClientTest {

    @Mock
    private SecurityServiceClient securityServiceClient;
    
    @Mock
    private PortfolioServiceClient portfolioServiceClient;
    
    @Mock
    private CircuitBreaker securityServiceCircuitBreaker;
    
    @Mock
    private CircuitBreaker portfolioServiceCircuitBreaker;
    
    @Mock
    private SecurityCacheService securityCacheService;
    
    @Mock
    private PortfolioCacheService portfolioCacheService;
    
    private ResilientExternalServiceClient resilientClient;
    
    @BeforeEach
    void setUp() {
        resilientClient = new ResilientExternalServiceClient(
            securityServiceClient,
            portfolioServiceClient,
            securityServiceCircuitBreaker,
            portfolioServiceCircuitBreaker,
            securityCacheService,
            portfolioCacheService
        );
    }
    
    @Test
    void getSecurityById_Success() {
        // Given
        String securityId = "SEC123";
        SecurityDTO expectedSecurity = new SecurityDTO(securityId, "AAPL");
        
        when(securityServiceClient.findSecurityById(securityId))
            .thenReturn(Optional.of(expectedSecurity));
        when(securityServiceCircuitBreaker.executeSupplier(any()))
            .thenAnswer(invocation -> {
                var supplier = invocation.getArgument(0, java.util.function.Supplier.class);
                return supplier.get();
            });
        
        // When
        Optional<SecurityDTO> result = resilientClient.getSecurityById(securityId);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(expectedSecurity.getSecurityId(), result.get().getSecurityId());
        assertEquals(expectedSecurity.getTicker(), result.get().getTicker());
        verify(securityServiceClient).findSecurityById(securityId);
    }
    
    @Test
    void getPortfolioById_Success() {
        // Given
        String portfolioId = "PORT123";
        PortfolioDTO expectedPortfolio = new PortfolioDTO(portfolioId, "Test Portfolio");
        
        when(portfolioServiceClient.findPortfolioById(portfolioId))
            .thenReturn(Optional.of(expectedPortfolio));
        when(portfolioServiceCircuitBreaker.executeSupplier(any()))
            .thenAnswer(invocation -> {
                var supplier = invocation.getArgument(0, java.util.function.Supplier.class);
                return supplier.get();
            });
        
        // When
        Optional<PortfolioDTO> result = resilientClient.getPortfolioById(portfolioId);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(expectedPortfolio.getPortfolioId(), result.get().getPortfolioId());
        assertEquals(expectedPortfolio.getName(), result.get().getName());
        verify(portfolioServiceClient).findPortfolioById(portfolioId);
    }
    
    @Test
    void getCircuitBreakerStates() {
        // Given
        when(securityServiceCircuitBreaker.getState())
            .thenReturn(CircuitBreaker.State.CLOSED);
        when(portfolioServiceCircuitBreaker.getState())
            .thenReturn(CircuitBreaker.State.OPEN);
        
        // When
        String securityState = resilientClient.getSecurityServiceCircuitBreakerState();
        String portfolioState = resilientClient.getPortfolioServiceCircuitBreakerState();
        
        // Then
        assertEquals("CLOSED", securityState);
        assertEquals("OPEN", portfolioState);
    }
}