package org.kasbench.globeco_trade_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Configuration properties for bulk execution submission settings.
 * Provides configurable batch size, max size, and retry settings for the bulk execution API.
 */
@Component
@ConfigurationProperties(prefix = "execution.service.batch")
@Validated
public class ExecutionBatchProperties {

    /**
     * Default batch size for execution submissions.
     * Must be between 1 and 100 (API limit).
     */
    @Min(value = 1, message = "Batch size must be at least 1")
    @Max(value = 100, message = "Batch size cannot exceed 100 (API limit)")
    private int size = 50;

    /**
     * Maximum allowed batch size.
     * Enforces the API limit of 100 executions per batch.
     */
    @Min(value = 1, message = "Max batch size must be at least 1")
    @Max(value = 100, message = "Max batch size cannot exceed 100 (API limit)")
    private int maxSize = 100;

    /**
     * Whether bulk batching is enabled.
     * When disabled, falls back to individual submissions.
     */
    private boolean enableBatching = true;

    /**
     * Number of retry attempts for failed individual executions within a batch.
     */
    @Min(value = 0, message = "Retry attempts cannot be negative")
    @Max(value = 10, message = "Retry attempts cannot exceed 10")
    private int retryFailedIndividually = 3;

    /**
     * Whether to enable metrics collection for bulk operations.
     */
    private boolean metricsEnabled = true;

    /**
     * Maximum delay between retry attempts in milliseconds.
     */
    @Min(value = 1000, message = "Max retry delay must be at least 1000ms")
    private long maxRetryDelayMs = 30000;

    /**
     * Initial delay for retry attempts in milliseconds.
     */
    @Min(value = 100, message = "Initial retry delay must be at least 100ms")
    private long initialRetryDelayMs = 2000;

    /**
     * Multiplier for exponential backoff retry strategy.
     */
    @Min(value = 1, message = "Retry multiplier must be at least 1")
    private double retryMultiplier = 2.0;

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public boolean isEnableBatching() {
        return enableBatching;
    }

    public void setEnableBatching(boolean enableBatching) {
        this.enableBatching = enableBatching;
    }

    public int getRetryFailedIndividually() {
        return retryFailedIndividually;
    }

    public void setRetryFailedIndividually(int retryFailedIndividually) {
        this.retryFailedIndividually = retryFailedIndividually;
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }

    public long getMaxRetryDelayMs() {
        return maxRetryDelayMs;
    }

    public void setMaxRetryDelayMs(long maxRetryDelayMs) {
        this.maxRetryDelayMs = maxRetryDelayMs;
    }

    public long getInitialRetryDelayMs() {
        return initialRetryDelayMs;
    }

    public void setInitialRetryDelayMs(long initialRetryDelayMs) {
        this.initialRetryDelayMs = initialRetryDelayMs;
    }

    public double getRetryMultiplier() {
        return retryMultiplier;
    }

    public void setRetryMultiplier(double retryMultiplier) {
        this.retryMultiplier = retryMultiplier;
    }

    /**
     * Validates that the current configuration is valid.
     * @return true if configuration is valid, false otherwise
     */
    public boolean isValid() {
        return size >= 1 && size <= maxSize && maxSize <= 100 && 
               retryFailedIndividually >= 0 && retryFailedIndividually <= 10;
    }

    /**
     * Gets the effective batch size, ensuring it doesn't exceed the maximum and enforces safe defaults.
     * @return the effective batch size to use
     */
    public int getEffectiveBatchSize() {
        int safeSize = size;
        int safeMaxSize = maxSize;
        
        // Enforce API limit of 100
        if (safeMaxSize > 100 || safeMaxSize < 1) {
            safeMaxSize = 100;
        }
        
        // Enforce minimum size and cap at maxSize
        if (safeSize < 1) {
            safeSize = 50; // Default fallback
        } else if (safeSize > safeMaxSize) {
            safeSize = safeMaxSize;
        }
        
        return safeSize;
    }
    
    /**
     * Gets the effective max size, ensuring it doesn't exceed API limits.
     * @return the effective max size to use
     */
    public int getEffectiveMaxSize() {
        if (maxSize > 100 || maxSize < 1) {
            return 100;
        }
        return maxSize;
    }
}