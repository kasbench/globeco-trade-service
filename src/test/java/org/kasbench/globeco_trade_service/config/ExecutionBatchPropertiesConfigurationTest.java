package org.kasbench.globeco_trade_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionBatchPropertiesConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @EnableConfigurationProperties(ExecutionBatchProperties.class)
    static class TestConfiguration {
    }

    @Test
    void testDefaultConfiguration() {
        contextRunner.run(context -> {
            ExecutionBatchProperties properties = context.getBean(ExecutionBatchProperties.class);
            
            assertThat(properties.getSize()).isEqualTo(50);
            assertThat(properties.getMaxSize()).isEqualTo(100);
            assertThat(properties.isEnableBatching()).isTrue();
            assertThat(properties.getRetryFailedIndividually()).isEqualTo(3);
            assertThat(properties.isMetricsEnabled()).isTrue();
            assertThat(properties.getMaxRetryDelayMs()).isEqualTo(30000);
            assertThat(properties.getInitialRetryDelayMs()).isEqualTo(2000);
            assertThat(properties.getRetryMultiplier()).isEqualTo(2.0);
        });
    }

    @Test
    void testCustomConfiguration() {
        contextRunner
                .withPropertyValues(
                        "execution.service.batch.size=25",
                        "execution.service.batch.max-size=80",
                        "execution.service.batch.enable-batching=false",
                        "execution.service.batch.retry-failed-individually=5",
                        "execution.service.batch.metrics-enabled=false",
                        "execution.service.batch.max-retry-delay-ms=60000",
                        "execution.service.batch.initial-retry-delay-ms=3000",
                        "execution.service.batch.retry-multiplier=1.5"
                )
                .run(context -> {
                    ExecutionBatchProperties properties = context.getBean(ExecutionBatchProperties.class);
                    
                    assertThat(properties.getSize()).isEqualTo(25);
                    assertThat(properties.getMaxSize()).isEqualTo(80);
                    assertThat(properties.isEnableBatching()).isFalse();
                    assertThat(properties.getRetryFailedIndividually()).isEqualTo(5);
                    assertThat(properties.isMetricsEnabled()).isFalse();
                    assertThat(properties.getMaxRetryDelayMs()).isEqualTo(60000);
                    assertThat(properties.getInitialRetryDelayMs()).isEqualTo(3000);
                    assertThat(properties.getRetryMultiplier()).isEqualTo(1.5);
                    
                    // Test effective values with valid configuration
                    assertThat(properties.getEffectiveBatchSize()).isEqualTo(25);
                    assertThat(properties.getEffectiveMaxSize()).isEqualTo(80);
                    assertThat(properties.isValid()).isTrue();
                });
    }

    @Test
    void testInvalidConfigurationHandling() {
        contextRunner
                .withPropertyValues(
                        "execution.service.batch.size=0",
                        "execution.service.batch.max-size=150",
                        "execution.service.batch.retry-failed-individually=-1"
                )
                .run(context -> {
                    // Context should fail to start due to validation errors
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(org.springframework.boot.context.properties.ConfigurationPropertiesBindException.class);
                });
    }

    @Test
    void testBoundaryValidValues() {
        contextRunner
                .withPropertyValues(
                        "execution.service.batch.size=1",
                        "execution.service.batch.max-size=100",
                        "execution.service.batch.retry-failed-individually=0"
                )
                .run(context -> {
                    ExecutionBatchProperties properties = context.getBean(ExecutionBatchProperties.class);
                    
                    assertThat(properties.getSize()).isEqualTo(1);
                    assertThat(properties.getMaxSize()).isEqualTo(100);
                    assertThat(properties.getRetryFailedIndividually()).isEqualTo(0);
                    assertThat(properties.isValid()).isTrue();
                });
    }
}