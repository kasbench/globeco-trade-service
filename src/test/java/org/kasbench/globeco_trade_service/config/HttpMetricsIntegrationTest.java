package org.kasbench.globeco_trade_service.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify that HTTP metrics are properly registered in the Spring application context
 * and can be found in the MeterRegistry.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
class HttpMetricsIntegrationTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private Counter httpRequestsTotal;

    @Autowired
    private Timer httpRequestDuration;

    @Autowired
    private Gauge httpRequestsInFlight;

    @Autowired
    private AtomicInteger inFlightRequestsCounter;

    @Test
    void httpMetrics_ShouldBeRegisteredAsSpringBeans() {
        // Verify that all metrics beans are properly injected
        assertNotNull(httpRequestsTotal);
        assertNotNull(httpRequestDuration);
        assertNotNull(httpRequestsInFlight);
        assertNotNull(inFlightRequestsCounter);
    }

    @Test
    void httpMetrics_ShouldBeFoundInMeterRegistry() {
        // Verify that all metrics are registered in the MeterRegistry
        Counter foundCounter = meterRegistry.find("http_requests_total").counter();
        Timer foundTimer = meterRegistry.find("http_request_duration_seconds").timer();
        Gauge foundGauge = meterRegistry.find("http_requests_in_flight").gauge();

        assertNotNull(foundCounter);
        assertNotNull(foundTimer);
        assertNotNull(foundGauge);
    }

    @Test
    void httpMetrics_ShouldHaveCorrectDescriptions() {
        // Verify metric descriptions
        assertEquals("Total number of HTTP requests", 
                    meterRegistry.find("http_requests_total").counter().getId().getDescription());
        assertEquals("Duration of HTTP requests in seconds", 
                    meterRegistry.find("http_request_duration_seconds").timer().getId().getDescription());
        assertEquals("Number of HTTP requests currently being processed", 
                    meterRegistry.find("http_requests_in_flight").gauge().getId().getDescription());
    }

    @Test
    void inFlightCounter_ShouldBeSharedBetweenGaugeAndBean() {
        // Verify that the same AtomicInteger instance is used by both the gauge and the bean
        int initialValue = inFlightRequestsCounter.get();
        double initialGaugeValue = httpRequestsInFlight.value();

        assertEquals(initialValue, (int) initialGaugeValue);

        // Modify the counter and verify gauge reflects the change
        inFlightRequestsCounter.incrementAndGet();
        assertEquals(initialValue + 1, (int) httpRequestsInFlight.value());

        inFlightRequestsCounter.decrementAndGet();
        assertEquals(initialValue, (int) httpRequestsInFlight.value());
    }
}