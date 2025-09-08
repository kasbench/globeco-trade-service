package org.kasbench.globeco_trade_service.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class for OptimizedHttpMetricsFilter.
 * Verifies async metrics recording, path normalization caching, and performance optimizations.
 */
@ExtendWith(MockitoExtension.class)
class OptimizedHttpMetricsFilterTest {

    private OptimizedHttpMetricsFilter filter;
    private MeterRegistry meterRegistry;
    private AtomicInteger inFlightRequestsCounter;
    
    @Mock
    private TaskExecutor metricsRecordingExecutor;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        inFlightRequestsCounter = new AtomicInteger(0);
        filter = new OptimizedHttpMetricsFilter(meterRegistry, inFlightRequestsCounter, metricsRecordingExecutor);
    }

    @Test
    void testDoFilter_RecordsMetricsAsynchronously() throws IOException, ServletException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        MockFilterChain filterChain = new MockFilterChain();
        
        // Mock the executor to run tasks synchronously for testing
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(metricsRecordingExecutor).execute(any(Runnable.class));

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        // Verify that the executor was called for async metrics recording
        verify(metricsRecordingExecutor, times(1)).execute(any(Runnable.class));
        
        // Verify metrics were recorded
        Counter counter = meterRegistry.find("http_requests_total").counter();
        assertNotNull(counter, "Counter should be registered");
        assertEquals(1.0, counter.count(), "Counter should be incremented");
        
        Timer timer = meterRegistry.find("http_request_duration").timer();
        assertNotNull(timer, "Timer should be registered");
        assertEquals(1, timer.count(), "Timer should record one measurement");
    }

    @Test
    void testDoFilter_HandlesInFlightCounter() throws IOException, ServletException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);
        MockFilterChain filterChain = new MockFilterChain();

        // Verify initial state
        assertEquals(0, inFlightRequestsCounter.get());

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        // Counter should be back to 0 after request completion
        assertEquals(0, inFlightRequestsCounter.get());
    }

    @Test
    void testDoFilter_HandlesNonHttpRequests() throws IOException, ServletException {
        // Arrange
        MockFilterChain filterChain = new MockFilterChain();
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> {
            filter.doFilter(mock(jakarta.servlet.ServletRequest.class), 
                           mock(jakarta.servlet.ServletResponse.class), 
                           filterChain);
        });
        
        // Verify no metrics executor calls for non-HTTP requests
        verify(metricsRecordingExecutor, never()).execute(any(Runnable.class));
    }

    @Test
    void testPathNormalization_CachesResults() throws IOException, ServletException {
        // Arrange
        MockHttpServletRequest request1 = new MockHttpServletRequest("GET", "/api/orders/123");
        MockHttpServletRequest request2 = new MockHttpServletRequest("GET", "/api/orders/123");
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        response1.setStatus(200);
        response2.setStatus(200);
        MockFilterChain filterChain1 = new MockFilterChain();
        MockFilterChain filterChain2 = new MockFilterChain();
        
        // Mock the executor to run tasks synchronously for testing
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(metricsRecordingExecutor).execute(any(Runnable.class));

        // Act - process same path twice
        filter.doFilter(request1, response1, filterChain1);
        filter.doFilter(request2, response2, filterChain2);

        // Assert
        // Both requests should result in metrics with normalized path
        Counter counter = meterRegistry.find("http_requests_total")
                .tag("path", "/api/orders/{id}")
                .counter();
        assertNotNull(counter, "Counter with normalized path should exist");
        assertEquals(2.0, counter.count(), "Counter should be incremented twice");
    }

    @Test
    void testMetricsRecording_WithDifferentStatusCodes() throws IOException, ServletException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(404);
        MockFilterChain filterChain = new MockFilterChain();
        
        // Mock the executor to run tasks synchronously for testing
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(metricsRecordingExecutor).execute(any(Runnable.class));

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        Counter counter = meterRegistry.find("http_requests_total")
                .tag("method", "GET")
                .tag("status", "404")
                .counter();
        assertNotNull(counter, "Counter with 404 status should exist");
        assertEquals(1.0, counter.count(), "Counter should be incremented");
    }

    @Test
    void testMetricsRecording_WithDifferentHttpMethods() throws IOException, ServletException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);
        MockFilterChain filterChain = new MockFilterChain();
        
        // Mock the executor to run tasks synchronously for testing
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(metricsRecordingExecutor).execute(any(Runnable.class));

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        Counter counter = meterRegistry.find("http_requests_total")
                .tag("method", "POST")
                .tag("status", "201")
                .counter();
        assertNotNull(counter, "Counter with POST method should exist");
        assertEquals(1.0, counter.count(), "Counter should be incremented");
        
        Timer timer = meterRegistry.find("http_request_duration")
                .tag("method", "POST")
                .tag("status", "201")
                .timer();
        assertNotNull(timer, "Timer with POST method should exist");
        assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.NANOSECONDS) > 0, 
                  "Timer should record positive duration");
    }
}