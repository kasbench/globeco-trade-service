package org.kasbench.globeco_trade_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for asynchronous processing with dedicated thread pools
 * for external service calls and metrics processing.
 */
@Configuration
@EnableAsync
public class AsyncCompensationConfig {

    /**
     * Thread pool executor for external service calls (execution submissions).
     * Configured with appropriate pool sizes and rejection policy for handling
     * external service integration without blocking main request threads.
     * 
     * Requirements: 8.1, 8.2
     */
    @Bean("executionSubmissionExecutor")
    public TaskExecutor executionSubmissionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size - minimum threads always alive
        executor.setCorePoolSize(10);
        
        // Maximum pool size - scales up under load
        executor.setMaxPoolSize(50);
        
        // Queue capacity - requests queued when all core threads busy
        executor.setQueueCapacity(100);
        
        // Thread naming for easier debugging
        executor.setThreadNamePrefix("execution-submit-");
        
        // Rejection policy - caller runs when pool and queue are full
        // This provides backpressure without dropping requests
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // Allow core threads to timeout when idle
        executor.setAllowCoreThreadTimeOut(true);
        
        // Keep alive time for idle threads
        executor.setKeepAliveSeconds(60);
        
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        return executor;
    }

    /**
     * Thread pool executor for metrics processing.
     * Configured with smaller pool size since metrics recording should be
     * lightweight and non-blocking to request processing.
     * 
     * Requirements: 8.2, 8.3
     */
    @Bean("metricsRecordingExecutor")
    public TaskExecutor metricsRecordingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Smaller core pool for metrics processing
        executor.setCorePoolSize(2);
        
        // Maximum pool size for metrics
        executor.setMaxPoolSize(5);
        
        // Larger queue capacity since metrics can be buffered
        executor.setQueueCapacity(1000);
        
        // Thread naming for easier debugging
        executor.setThreadNamePrefix("metrics-");
        
        // Rejection policy - discard oldest when queue is full
        // For metrics, it's acceptable to drop old metrics under extreme load
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        
        // Allow core threads to timeout when idle
        executor.setAllowCoreThreadTimeOut(true);
        
        // Keep alive time for idle threads
        executor.setKeepAliveSeconds(60);
        
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        
        executor.initialize();
        return executor;
    }
}