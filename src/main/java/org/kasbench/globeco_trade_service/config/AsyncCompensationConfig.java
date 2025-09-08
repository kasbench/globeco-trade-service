package org.kasbench.globeco_trade_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for asynchronous compensation processing.
 * This configuration provides dedicated thread pools for compensation operations
 * to ensure they don't block the main application threads.
 */
@Configuration
@EnableAsync
public class AsyncCompensationConfig {
    
    /**
     * Creates a dedicated executor for compensation operations.
     * This executor is configured with appropriate pool sizes and rejection policies
     * to handle compensation operations asynchronously.
     * 
     * @return The compensation executor
     */
    @Bean("compensationExecutor")
    public Executor compensationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size - number of threads to keep alive
        executor.setCorePoolSize(2);
        
        // Maximum pool size - maximum number of threads
        executor.setMaxPoolSize(5);
        
        // Queue capacity - number of tasks to queue before creating new threads
        executor.setQueueCapacity(100);
        
        // Thread name prefix for easier debugging
        executor.setThreadNamePrefix("compensation-");
        
        // Rejection policy - what to do when pool and queue are full
        // CallerRunsPolicy ensures the task is executed by the calling thread
        // This provides backpressure and prevents task loss
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // Allow core threads to timeout when idle
        executor.setAllowCoreThreadTimeOut(true);
        
        // Keep alive time for idle threads (in seconds)
        executor.setKeepAliveSeconds(60);
        
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // Maximum time to wait for tasks to complete on shutdown (in seconds)
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        return executor;
    }
}