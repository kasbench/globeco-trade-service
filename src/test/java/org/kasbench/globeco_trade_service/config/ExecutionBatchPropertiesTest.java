package org.kasbench.globeco_trade_service.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionBatchPropertiesTest {

    private ExecutionBatchProperties properties;
    private Validator validator;

    @BeforeEach
    void setUp() {
        properties = new ExecutionBatchProperties();
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testDefaultValues() {
        assertEquals(50, properties.getSize());
        assertEquals(100, properties.getMaxSize());
        assertTrue(properties.isEnableBatching());
        assertEquals(3, properties.getRetryFailedIndividually());
        assertTrue(properties.isMetricsEnabled());
        assertEquals(30000, properties.getMaxRetryDelayMs());
        assertEquals(2000, properties.getInitialRetryDelayMs());
        assertEquals(2.0, properties.getRetryMultiplier());
    }

    @Test
    void testValidConfiguration() {
        properties.setSize(75);
        properties.setMaxSize(100);
        properties.setRetryFailedIndividually(5);

        Set<ConstraintViolation<ExecutionBatchProperties>> violations = validator.validate(properties);
        assertTrue(violations.isEmpty());
        assertTrue(properties.isValid());
    }

    @Test
    void testBatchSizeValidation() {
        // Test minimum batch size - raw value should be stored
        properties.setSize(0);
        assertEquals(0, properties.getSize()); // Raw value stored
        assertEquals(50, properties.getEffectiveBatchSize()); // Effective value uses default

        // Test batch size exceeding max size - effective method handles this
        properties.setMaxSize(50);
        properties.setSize(75);
        assertEquals(75, properties.getSize()); // Raw value stored
        assertEquals(50, properties.getEffectiveBatchSize()); // Effective value capped at maxSize
    }

    @Test
    void testMaxSizeValidation() {
        // Test max size exceeding API limit - raw value stored
        properties.setMaxSize(150);
        assertEquals(150, properties.getMaxSize()); // Raw value stored
        assertEquals(100, properties.getEffectiveMaxSize()); // Effective value capped at 100

        // Test max size below minimum - raw value stored
        properties.setMaxSize(0);
        assertEquals(0, properties.getMaxSize()); // Raw value stored
        assertEquals(100, properties.getEffectiveMaxSize()); // Effective value uses default

        // Test effective batch size with invalid maxSize
        properties.setSize(75);
        properties.setMaxSize(50);
        assertEquals(75, properties.getSize()); // Raw size value stored
        assertEquals(50, properties.getMaxSize()); // Raw maxSize value stored
        assertEquals(50, properties.getEffectiveBatchSize()); // Effective size capped at maxSize
    }

    @Test
    void testConstraintValidations() {
        // Test batch size constraints
        properties.setSize(-1);
        Set<ConstraintViolation<ExecutionBatchProperties>> violations = validator.validate(properties);
        assertFalse(violations.isEmpty());

        properties.setSize(101);
        violations = validator.validate(properties);
        assertFalse(violations.isEmpty());

        // Test retry attempts constraints
        properties.setSize(50); // Reset to valid value
        properties.setRetryFailedIndividually(-1);
        violations = validator.validate(properties);
        assertFalse(violations.isEmpty());

        properties.setRetryFailedIndividually(11);
        violations = validator.validate(properties);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testEffectiveBatchSize() {
        properties.setSize(75);
        properties.setMaxSize(100);
        assertEquals(75, properties.getEffectiveBatchSize());

        properties.setSize(75);
        properties.setMaxSize(50);
        assertEquals(50, properties.getEffectiveBatchSize());
        
        // Test with invalid values
        properties.setSize(0);
        properties.setMaxSize(200);
        assertEquals(50, properties.getEffectiveBatchSize()); // Default size, capped by API limit
    }

    @Test
    void testConfigurationBinding() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("execution.service.batch.size", "25");
        configMap.put("execution.service.batch.max-size", "80");
        configMap.put("execution.service.batch.enable-batching", "false");
        configMap.put("execution.service.batch.retry-failed-individually", "5");
        configMap.put("execution.service.batch.metrics-enabled", "false");

        ConfigurationPropertySource source = new MapConfigurationPropertySource(configMap);
        Binder binder = new Binder(source);
        
        ExecutionBatchProperties boundProperties = binder.bind("execution.service.batch", ExecutionBatchProperties.class)
                .orElse(new ExecutionBatchProperties());

        assertEquals(25, boundProperties.getSize());
        assertEquals(80, boundProperties.getMaxSize());
        assertFalse(boundProperties.isEnableBatching());
        assertEquals(5, boundProperties.getRetryFailedIndividually());
        assertFalse(boundProperties.isMetricsEnabled());
    }

    @Test
    void testApiLimitEnforcement() {
        // Test that API limit of 100 is enforced in effective methods
        properties.setMaxSize(200);
        assertEquals(200, properties.getMaxSize()); // Raw value stored
        assertEquals(100, properties.getEffectiveMaxSize()); // Effective value enforces API limit

        properties.setSize(150);
        assertEquals(150, properties.getSize()); // Raw value stored
        assertEquals(100, properties.getEffectiveBatchSize()); // Effective value capped at API limit
    }

    @Test
    void testRetryDelayValidation() {
        properties.setMaxRetryDelayMs(500); // Below minimum
        Set<ConstraintViolation<ExecutionBatchProperties>> violations = validator.validate(properties);
        assertFalse(violations.isEmpty());

        properties.setMaxRetryDelayMs(60000); // Valid value
        violations = validator.validate(properties);
        assertTrue(violations.isEmpty());

        properties.setInitialRetryDelayMs(50); // Below minimum
        violations = validator.validate(properties);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testRetryMultiplierValidation() {
        properties.setRetryMultiplier(0.5); // Below minimum
        Set<ConstraintViolation<ExecutionBatchProperties>> violations = validator.validate(properties);
        assertFalse(violations.isEmpty());

        properties.setRetryMultiplier(2.5); // Valid value
        violations = validator.validate(properties);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testIsValidMethod() {
        assertTrue(properties.isValid());

        properties.setSize(0);
        assertFalse(properties.isValid());

        properties.setSize(50);
        properties.setMaxSize(150);
        assertFalse(properties.isValid()); // maxSize > 100

        properties.setMaxSize(100);
        properties.setRetryFailedIndividually(-1);
        assertFalse(properties.isValid());

        properties.setRetryFailedIndividually(15);
        assertFalse(properties.isValid()); // > 10

        properties.setRetryFailedIndividually(5);
        assertTrue(properties.isValid());
    }
}