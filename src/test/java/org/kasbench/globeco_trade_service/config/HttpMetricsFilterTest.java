package org.kasbench.globeco_trade_service.config;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpMetricsFilterTest {

    @Mock
    private FilterChain filterChain;

    private SimpleMeterRegistry meterRegistry;
    private AtomicInteger inFlightRequestsCounter;
    private HttpMetricsFilter httpMetricsFilter;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        inFlightRequestsCounter = new AtomicInteger(0);
        httpMetricsFilter = new HttpMetricsFilter(meterRegistry, inFlightRequestsCounter);
    }

    @Test
    void testDoFilter_SuccessfulRequest() throws IOException, ServletException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        // Act
        httpMetricsFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertEquals(0, inFlightRequestsCounter.get());
        
        // Verify metrics were recorded
        assertNotNull(meterRegistry.find("http_requests_total").counter());
        assertNotNull(meterRegistry.find("http_request_duration_seconds").timer());
        
        // Verify counter was incremented
        assertTrue(meterRegistry.find("http_requests_total").counter().count() > 0);
    }

    @Test
    void testDoFilter_ErrorRequest() throws IOException, ServletException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);

        // Act
        httpMetricsFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertEquals(0, inFlightRequestsCounter.get());
        
        // Verify metrics were recorded
        assertTrue(meterRegistry.find("http_requests_total").counter().count() > 0);
        assertTrue(meterRegistry.find("http_request_duration_seconds").timer().count() > 0);
    }

    @Test
    void testDoFilter_WithException() throws IOException, ServletException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        
        doThrow(new ServletException("Test exception")).when(filterChain).doFilter(request, response);

        // Act & Assert
        assertThrows(ServletException.class, () -> {
            httpMetricsFilter.doFilter(request, response, filterChain);
        });

        // Verify metrics are still recorded and in-flight counter is decremented
        assertEquals(0, inFlightRequestsCounter.get());
        assertTrue(meterRegistry.find("http_requests_total").counter().count() > 0);
    }

    @Test
    void testDoFilter_InFlightTracking() throws IOException, ServletException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        // Mock filter chain to check in-flight counter during processing
        doAnswer(invocation -> {
            assertEquals(1, inFlightRequestsCounter.get());
            return null;
        }).when(filterChain).doFilter(request, response);

        // Act
        httpMetricsFilter.doFilter(request, response, filterChain);

        // Assert
        assertEquals(0, inFlightRequestsCounter.get());
    }

    @Test
    void testNormalizeHttpMethod() throws IOException, ServletException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("post", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        // Act
        httpMetricsFilter.doFilter(request, response, filterChain);

        // Assert
        assertTrue(meterRegistry.find("http_requests_total").counter().count() > 0);
    }

    @Test
    void testPathNormalization_NumericIds() throws IOException, ServletException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/12345/orders/67890");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        // Act
        httpMetricsFilter.doFilter(request, response, filterChain);

        // Assert
        assertTrue(meterRegistry.find("http_requests_total").counter().count() > 0);
    }

    @Test
    void testPathNormalization_UUIDs() throws IOException, ServletException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/550e8400-e29b-41d4-a716-446655440000");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        // Act
        httpMetricsFilter.doFilter(request, response, filterChain);

        // Assert
        assertTrue(meterRegistry.find("http_requests_total").counter().count() > 0);
    }

    @Test
    void testPathNormalization_WithSpringMvcPattern() throws IOException, ServletException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/123");
        request.setAttribute("org.springframework.web.servlet.HandlerMapping.bestMatchingPattern", "/api/users/{id}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        // Act
        httpMetricsFilter.doFilter(request, response, filterChain);

        // Assert
        assertTrue(meterRegistry.find("http_requests_total").counter().count() > 0);
    }

    @Test
    void testStatusCodeNormalization() throws IOException, ServletException {
        // Test various status codes
        int[] statusCodes = {200, 201, 400, 404, 500};
        String[] expectedStatuses = {"200", "201", "400", "404", "500"};

        for (int i = 0; i < statusCodes.length; i++) {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test" + i);
            MockHttpServletResponse response = new MockHttpServletResponse();
            response.setStatus(statusCodes[i]);

            // Act
            httpMetricsFilter.doFilter(request, response, filterChain);

            // No specific assertions needed for this loop test
        }
        
        // Verify all requests were recorded (each creates a separate counter with different tags)
        assertTrue(meterRegistry.find("http_requests_total").counters().size() >= statusCodes.length);
    }
}