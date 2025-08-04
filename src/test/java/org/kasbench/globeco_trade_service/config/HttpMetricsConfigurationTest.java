package org.kasbench.globeco_trade_service.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class HttpMetricsConfigurationTest {

    private HttpMetricsConfiguration configuration;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        configuration = new HttpMetricsConfiguration();
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    void httpRequestsTotal_ShouldCreateCounterWithCorrectNameAndDescription() {
        // When
        Counter counter = configuration.httpRequestsTotal(meterRegistry);

        // Then
        assertNotNull(counter);
        assertEquals("http_requests_total", counter.getId().getName());
        assertEquals("Total number of HTTP requests", counter.getId().getDescription());
        assertEquals(0.0, counter.count());
    }

    @Test
    void httpDurationMeterFilter_ShouldBeCreated() {
        // When
        var meterFilter = configuration.httpDurationMeterFilter();

        // Then
        assertNotNull(meterFilter);
    }

    @Test
    void httpRequestsInFlight_ShouldCreateGaugeWithCorrectNameAndDescription() {
        // When
        Gauge gauge = configuration.httpRequestsInFlight(meterRegistry);

        // Then
        assertNotNull(gauge);
        assertEquals("http_requests_in_flight", gauge.getId().getName());
        assertEquals("Number of HTTP requests currently being processed", gauge.getId().getDescription());
        assertEquals(0.0, gauge.value());
    }

    @Test
    void inFlightRequestsCounter_ShouldReturnAtomicInteger() {
        // When
        AtomicInteger counter = configuration.inFlightRequestsCounter();

        // Then
        assertNotNull(counter);
        assertEquals(0, counter.get());
    }

    @Test
    void inFlightGauge_ShouldReflectCounterChanges() {
        // Given
        Gauge gauge = configuration.httpRequestsInFlight(meterRegistry);
        AtomicInteger counter = configuration.inFlightRequestsCounter();

        // When
        counter.incrementAndGet();

        // Then
        assertEquals(1.0, gauge.value());

        // When
        counter.decrementAndGet();

        // Then
        assertEquals(0.0, gauge.value());
    }

    @Test
    void basicMetrics_ShouldBeRegisteredInMeterRegistry() {
        // When
        configuration.httpRequestsTotal(meterRegistry);
        configuration.httpRequestsInFlight(meterRegistry);

        // Then
        assertNotNull(meterRegistry.find("http_requests_total").counter());
        assertNotNull(meterRegistry.find("http_requests_in_flight").gauge());
    }

    @Test
    void counter_ShouldIncrementCorrectly() {
        // Given
        Counter counter = configuration.httpRequestsTotal(meterRegistry);

        // When
        counter.increment();
        counter.increment();

        // Then
        assertEquals(2.0, counter.count());
    }
}