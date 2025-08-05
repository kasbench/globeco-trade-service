package org.kasbench.globeco_trade_service.config;

import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Configuration
public class MicrometerConfig {

    private static final Logger logger = LoggerFactory.getLogger(MicrometerConfig.class);


    private final MeterRegistry meterRegistry;

    public MicrometerConfig(MeterRegistry meterRegistry) {
        logger.info("MicrometerConfig constructor called.");
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void customizeMetrics() {
        logger.info("Customizing metrics...");
        meterRegistry.config().meterFilter(
            MeterFilter.commonTags(Tags.of("unit", "seconds"))
        );

        meterRegistry.config().meterFilter(new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                if (id.getName().equals("http_request_duration_seconds")) {
                    logger.info("Success - Mapping metric: {}", id.getName());
                    return id.withBaseUnit("seconds");
                }
                if (id.getName().equals("http_request_duration")) {
                    logger.info("Success - Mapping metric: {}", id.getName());
                    return id.withBaseUnit("seconds");
                }
                logger.info("Other - Mapping metric: {}", id.getName());
                return id;
            }
        });
    }

    @Bean
    public CommandLineRunner micrometerDebug() {
        return args -> logger.info("MicrometerConfig active and MeterRegistry available: {}", meterRegistry.getClass());
}
}