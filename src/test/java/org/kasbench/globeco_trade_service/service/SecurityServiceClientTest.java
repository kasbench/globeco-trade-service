package org.kasbench.globeco_trade_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kasbench.globeco_trade_service.client.SecurityServiceClient;
import org.kasbench.globeco_trade_service.dto.SecurityDTO;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    private SecurityServiceClient securityServiceClient;

    @BeforeEach
    void setUp() {
        securityServiceClient = new SecurityServiceClient(restTemplate);
    }

    @Test
    void testFindSecurityByTicker_Success() {
        // Arrange
        String ticker = "AAPL";
        SecurityServiceClient.SecuritySearchResponse mockResponse = new SecurityServiceClient.SecuritySearchResponse();
        SecurityDTO mockSecurity = new SecurityDTO();
        mockSecurity.setSecurityId("SEC123");
        mockSecurity.setTicker("AAPL");
        mockResponse.setSecurities(List.of(mockSecurity));

        when(restTemplate.getForEntity(anyString(), eq(SecurityServiceClient.SecuritySearchResponse.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // Act
        Optional<SecurityDTO> result = securityServiceClient.findSecurityByTicker(ticker);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("SEC123", result.get().getSecurityId());
        assertEquals("AAPL", result.get().getTicker());
        verify(restTemplate).getForEntity(
                contains("ticker=AAPL"),
                eq(SecurityServiceClient.SecuritySearchResponse.class)
        );
    }

    @Test
    void testFindSecurityByTicker_NotFound() {
        // Arrange
        String ticker = "UNKNOWN";
        SecurityServiceClient.SecuritySearchResponse mockResponse = new SecurityServiceClient.SecuritySearchResponse();
        mockResponse.setSecurities(List.of());

        when(restTemplate.getForEntity(anyString(), eq(SecurityServiceClient.SecuritySearchResponse.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // Act
        Optional<SecurityDTO> result = securityServiceClient.findSecurityByTicker(ticker);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testFindSecurityByTicker_NullResponse() {
        // Arrange
        String ticker = "AAPL";
        when(restTemplate.getForEntity(anyString(), eq(SecurityServiceClient.SecuritySearchResponse.class)))
                .thenReturn(ResponseEntity.ok(null));

        // Act
        Optional<SecurityDTO> result = securityServiceClient.findSecurityByTicker(ticker);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testFindSecurityByTicker_RestClientException() {
        // Arrange
        String ticker = "AAPL";
        when(restTemplate.getForEntity(anyString(), eq(SecurityServiceClient.SecuritySearchResponse.class)))
                .thenThrow(new RestClientException("Connection failed"));

        // Act
        Optional<SecurityDTO> result = securityServiceClient.findSecurityByTicker(ticker);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testFindSecurityByTicker_Timeout() {
        // Arrange
        String ticker = "AAPL";
        when(restTemplate.getForEntity(anyString(), eq(SecurityServiceClient.SecuritySearchResponse.class)))
                .thenThrow(new ResourceAccessException("Read timeout"));

        // Act & Assert
        assertThrows(ResourceAccessException.class, () -> {
            securityServiceClient.findSecurityByTicker(ticker);
        });
    }

    @Test
    void testFindSecurityByTicker_NullTicker() {
        // Act
        Optional<SecurityDTO> result = securityServiceClient.findSecurityByTicker(null);

        // Assert
        assertFalse(result.isPresent());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void testFindSecurityByTicker_EmptyTicker() {
        // Act
        Optional<SecurityDTO> result = securityServiceClient.findSecurityByTicker("");

        // Assert
        assertFalse(result.isPresent());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void testFindSecurityByTicker_BlankTicker() {
        // Act
        Optional<SecurityDTO> result = securityServiceClient.findSecurityByTicker("   ");

        // Assert
        assertFalse(result.isPresent());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void testMultipleSecurities_ReturnsFirst() {
        // Arrange
        String ticker = "AAPL";
        SecurityServiceClient.SecuritySearchResponse mockResponse = new SecurityServiceClient.SecuritySearchResponse();
        
        SecurityDTO mockSecurity1 = new SecurityDTO();
        mockSecurity1.setSecurityId("SEC123");
        mockSecurity1.setTicker("AAPL");
        
        SecurityDTO mockSecurity2 = new SecurityDTO();
        mockSecurity2.setSecurityId("SEC456");
        mockSecurity2.setTicker("AAPL");
        
        mockResponse.setSecurities(List.of(mockSecurity1, mockSecurity2));

        when(restTemplate.getForEntity(anyString(), eq(SecurityServiceClient.SecuritySearchResponse.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // Act
        Optional<SecurityDTO> result = securityServiceClient.findSecurityByTicker(ticker);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("SEC123", result.get().getSecurityId());
    }

    @Test
    void testFindSecuritiesByTickerLike_Success() {
        // Arrange
        String pattern = "AAP";
        SecurityServiceClient.SecuritySearchResponse mockResponse = new SecurityServiceClient.SecuritySearchResponse();
        
        SecurityDTO mockSecurity1 = new SecurityDTO();
        mockSecurity1.setSecurityId("SEC123");
        mockSecurity1.setTicker("AAPL");
        
        SecurityDTO mockSecurity2 = new SecurityDTO();
        mockSecurity2.setSecurityId("SEC456");
        mockSecurity2.setTicker("AAPLC");
        
        mockResponse.setSecurities(List.of(mockSecurity1, mockSecurity2));

        when(restTemplate.getForEntity(anyString(), eq(SecurityServiceClient.SecuritySearchResponse.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // Act
        List<SecurityDTO> result = securityServiceClient.findSecuritiesByTickerLike(pattern);

        // Assert
        assertEquals(2, result.size());
        assertEquals("AAPL", result.get(0).getTicker());
        assertEquals("AAPLC", result.get(1).getTicker());
    }

    @Test
    void testFindSecurityById_Success() {
        // Arrange
        String securityId = "684F122BEEA39200E562918C";
        SecurityDTO mockSecurity = new SecurityDTO();
        mockSecurity.setSecurityId("684F122BEEA39200E562918C");
        mockSecurity.setTicker("AAPL");

        when(restTemplate.getForEntity(anyString(), eq(SecurityDTO.class)))
                .thenReturn(ResponseEntity.ok(mockSecurity));

        // Act
        Optional<SecurityDTO> result = securityServiceClient.findSecurityById(securityId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("684F122BEEA39200E562918C", result.get().getSecurityId());
        assertEquals("AAPL", result.get().getTicker());
        verify(restTemplate).getForEntity(
                contains("/api/v1/security/684F122BEEA39200E562918C"),
                eq(SecurityDTO.class)
        );
    }

    @Test
    void testFindSecurityById_NotFound() {
        // Arrange
        String securityId = "UNKNOWN";
        when(restTemplate.getForEntity(anyString(), eq(SecurityDTO.class)))
                .thenReturn(ResponseEntity.notFound().build());

        // Act
        Optional<SecurityDTO> result = securityServiceClient.findSecurityById(securityId);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testFindSecurityById_NullId() {
        // Act
        Optional<SecurityDTO> result = securityServiceClient.findSecurityById(null);

        // Assert
        assertFalse(result.isPresent());
        verifyNoInteractions(restTemplate);
    }
} 