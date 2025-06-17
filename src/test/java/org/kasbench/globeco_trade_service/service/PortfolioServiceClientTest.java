package org.kasbench.globeco_trade_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kasbench.globeco_trade_service.client.PortfolioServiceClient;
import org.kasbench.globeco_trade_service.dto.PortfolioDTO;
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
class PortfolioServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    private PortfolioServiceClient portfolioServiceClient;

    @BeforeEach
    void setUp() {
        portfolioServiceClient = new PortfolioServiceClient(restTemplate);
    }

    @Test
    void testFindPortfolioByName_Success() {
        // Arrange
        String name = "Growth Fund";
        PortfolioServiceClient.PortfolioSearchResponse mockResponse = new PortfolioServiceClient.PortfolioSearchResponse();
        PortfolioDTO mockPortfolio = new PortfolioDTO();
        mockPortfolio.setPortfolioId("PORT123");
        mockPortfolio.setName("Growth Fund");
        mockResponse.setPortfolios(List.of(mockPortfolio));

        when(restTemplate.getForEntity(anyString(), eq(PortfolioServiceClient.PortfolioSearchResponse.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // Act
        Optional<PortfolioDTO> result = portfolioServiceClient.findPortfolioByName(name);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("PORT123", result.get().getPortfolioId());
        assertEquals("Growth Fund", result.get().getName());
        verify(restTemplate).getForEntity(
                contains("name=Growth%20Fund"),
                eq(PortfolioServiceClient.PortfolioSearchResponse.class)
        );
    }

    @Test
    void testFindPortfolioByName_NotFound() {
        // Arrange
        String name = "Unknown Fund";
        PortfolioServiceClient.PortfolioSearchResponse mockResponse = new PortfolioServiceClient.PortfolioSearchResponse();
        mockResponse.setPortfolios(List.of());

        when(restTemplate.getForEntity(anyString(), eq(PortfolioServiceClient.PortfolioSearchResponse.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // Act
        Optional<PortfolioDTO> result = portfolioServiceClient.findPortfolioByName(name);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testFindPortfolioByName_NullResponse() {
        // Arrange
        String name = "Growth Fund";
        when(restTemplate.getForEntity(anyString(), eq(PortfolioServiceClient.PortfolioSearchResponse.class)))
                .thenReturn(ResponseEntity.ok(null));

        // Act
        Optional<PortfolioDTO> result = portfolioServiceClient.findPortfolioByName(name);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testFindPortfolioByName_RestClientException() {
        // Arrange
        String name = "Growth Fund";
        when(restTemplate.getForEntity(anyString(), eq(PortfolioServiceClient.PortfolioSearchResponse.class)))
                .thenThrow(new RestClientException("Connection failed"));

        // Act
        Optional<PortfolioDTO> result = portfolioServiceClient.findPortfolioByName(name);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testFindPortfolioByName_Timeout() {
        // Arrange
        String name = "Growth Fund";
        when(restTemplate.getForEntity(anyString(), eq(PortfolioServiceClient.PortfolioSearchResponse.class)))
                .thenThrow(new ResourceAccessException("Read timeout"));

        // Act & Assert
        assertThrows(ResourceAccessException.class, () -> {
            portfolioServiceClient.findPortfolioByName(name);
        });
    }

    @Test
    void testFindPortfolioByName_NullName() {
        // Act
        Optional<PortfolioDTO> result = portfolioServiceClient.findPortfolioByName(null);

        // Assert
        assertFalse(result.isPresent());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void testFindPortfolioByName_EmptyName() {
        // Act
        Optional<PortfolioDTO> result = portfolioServiceClient.findPortfolioByName("");

        // Assert
        assertFalse(result.isPresent());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void testFindPortfolioByName_BlankName() {
        // Act
        Optional<PortfolioDTO> result = portfolioServiceClient.findPortfolioByName("   ");

        // Assert
        assertFalse(result.isPresent());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void testMultiplePortfolios_ReturnsFirst() {
        // Arrange
        String name = "Growth Fund";
        PortfolioServiceClient.PortfolioSearchResponse mockResponse = new PortfolioServiceClient.PortfolioSearchResponse();
        
        PortfolioDTO mockPortfolio1 = new PortfolioDTO();
        mockPortfolio1.setPortfolioId("PORT123");
        mockPortfolio1.setName("Growth Fund");
        
        PortfolioDTO mockPortfolio2 = new PortfolioDTO();
        mockPortfolio2.setPortfolioId("PORT456");
        mockPortfolio2.setName("Growth Fund");
        
        mockResponse.setPortfolios(List.of(mockPortfolio1, mockPortfolio2));

        when(restTemplate.getForEntity(anyString(), eq(PortfolioServiceClient.PortfolioSearchResponse.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // Act
        Optional<PortfolioDTO> result = portfolioServiceClient.findPortfolioByName(name);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("PORT123", result.get().getPortfolioId());
    }

    @Test
    void testFindPortfoliosByNameLike_Success() {
        // Arrange
        String pattern = "Growth";
        PortfolioServiceClient.PortfolioSearchResponse mockResponse = new PortfolioServiceClient.PortfolioSearchResponse();
        
        PortfolioDTO mockPortfolio1 = new PortfolioDTO();
        mockPortfolio1.setPortfolioId("PORT123");
        mockPortfolio1.setName("Growth Fund");
        
        PortfolioDTO mockPortfolio2 = new PortfolioDTO();
        mockPortfolio2.setPortfolioId("PORT456");
        mockPortfolio2.setName("Growth Income Fund");
        
        mockResponse.setPortfolios(List.of(mockPortfolio1, mockPortfolio2));

        when(restTemplate.getForEntity(anyString(), eq(PortfolioServiceClient.PortfolioSearchResponse.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // Act
        List<PortfolioDTO> result = portfolioServiceClient.findPortfoliosByNameLike(pattern);

        // Assert
        assertEquals(2, result.size());
        assertEquals("Growth Fund", result.get(0).getName());
        assertEquals("Growth Income Fund", result.get(1).getName());
    }

    @Test
    void testNameWithSpaces_UrlEncoding() {
        // Arrange
        String name = "Growth Technology Fund";
        PortfolioServiceClient.PortfolioSearchResponse mockResponse = new PortfolioServiceClient.PortfolioSearchResponse();
        mockResponse.setPortfolios(List.of());

        when(restTemplate.getForEntity(anyString(), eq(PortfolioServiceClient.PortfolioSearchResponse.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // Act
        portfolioServiceClient.findPortfolioByName(name);

        // Assert
        verify(restTemplate).getForEntity(
                contains("name=Growth%20Technology%20Fund"),
                eq(PortfolioServiceClient.PortfolioSearchResponse.class)
        );
    }

    @Test
    void testFindPortfolioById_Success() {
        // Arrange
        String portfolioId = "684f1250c19ad4fb89bbfed7";
        PortfolioDTO mockPortfolio = new PortfolioDTO();
        mockPortfolio.setPortfolioId("684f1250c19ad4fb89bbfed7");
        mockPortfolio.setName("Growth Fund");

        when(restTemplate.getForEntity(anyString(), eq(PortfolioDTO.class)))
                .thenReturn(ResponseEntity.ok(mockPortfolio));

        // Act
        Optional<PortfolioDTO> result = portfolioServiceClient.findPortfolioById(portfolioId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("684f1250c19ad4fb89bbfed7", result.get().getPortfolioId());
        assertEquals("Growth Fund", result.get().getName());
        verify(restTemplate).getForEntity(
                contains("/api/v1/portfolio/684f1250c19ad4fb89bbfed7"),
                eq(PortfolioDTO.class)
        );
    }

    @Test
    void testFindPortfolioById_NotFound() {
        // Arrange
        String portfolioId = "unknown";
        when(restTemplate.getForEntity(anyString(), eq(PortfolioDTO.class)))
                .thenReturn(ResponseEntity.notFound().build());

        // Act
        Optional<PortfolioDTO> result = portfolioServiceClient.findPortfolioById(portfolioId);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testFindPortfolioById_NullId() {
        // Act
        Optional<PortfolioDTO> result = portfolioServiceClient.findPortfolioById(null);

        // Assert
        assertFalse(result.isPresent());
        verifyNoInteractions(restTemplate);
    }
} 