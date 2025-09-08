package org.kasbench.globeco_trade_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AsyncCompensationConfig.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AsyncCompensationConfigTest {

    @Autowired
    @Qualifier("executionSubmissionExecutor")
    private TaskExecutor executionSubmissionExecutor;

    @Autowired
    @Qualifier("metricsRecordingExecutor")
    private TaskExecutor metricsRecordingExecutor;

    @Test
    void testExecutionSubmissionExecutorConfiguration() {
        assertNotNull(executionSubmissionExecutor);
        assertTrue(executionSubmissionExecutor instanceof ThreadPoolTaskExecutor);
        
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) executionSubmissionExecutor;
        assertEquals(10, executor.getCorePoolSize());
        assertEquals(50, executor.getMaxPoolSize());
        assertEquals(100, executor.getQueueCapacity());
        assertTrue(executor.getThreadNamePrefix().startsWith("execution-submit-"));
    }

    @Test
    void testMetricsRecordingExecutorConfiguration() {
        assertNotNull(metricsRecordingExecutor);
        assertTrue(metricsRecordingExecutor instanceof ThreadPoolTaskExecutor);
        
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) metricsRecordingExecutor;
        assertEquals(2, executor.getCorePoolSize());
        assertEquals(5, executor.getMaxPoolSize());
        assertEquals(1000, executor.getQueueCapacity());
        assertTrue(executor.getThreadNamePrefix().startsWith("metrics-"));
    }

    @Test
    void testExecutionSubmissionExecutorAsyncExecution() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(5);
        AtomicInteger counter = new AtomicInteger(0);

        // Submit 5 tasks concurrently
        for (int i = 0; i < 5; i++) {
            executionSubmissionExecutor.execute(() -> {
                try {
                    Thread.sleep(100); // Simulate work
                    counter.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all tasks to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(5, counter.get());
    }

    @Test
    void testMetricsRecordingExecutorAsyncExecution() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        AtomicInteger counter = new AtomicInteger(0);

        // Submit 3 metrics recording tasks
        for (int i = 0; i < 3; i++) {
            metricsRecordingExecutor.execute(() -> {
                try {
                    Thread.sleep(50); // Simulate metrics recording
                    counter.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all tasks to complete
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals(3, counter.get());
    }

    @Test
    void testConcurrentExecutionWithBothExecutors() throws InterruptedException {
        CountDownLatch executionLatch = new CountDownLatch(2);
        CountDownLatch metricsLatch = new CountDownLatch(2);
        
        AtomicInteger executionCounter = new AtomicInteger(0);
        AtomicInteger metricsCounter = new AtomicInteger(0);

        // Submit tasks to execution executor
        for (int i = 0; i < 2; i++) {
            executionSubmissionExecutor.execute(() -> {
                try {
                    Thread.sleep(100);
                    executionCounter.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    executionLatch.countDown();
                }
            });
        }

        // Submit tasks to metrics executor
        for (int i = 0; i < 2; i++) {
            metricsRecordingExecutor.execute(() -> {
                try {
                    Thread.sleep(50);
                    metricsCounter.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    metricsLatch.countDown();
                }
            });
        }

        // Both executors should work independently
        assertTrue(executionLatch.await(3, TimeUnit.SECONDS));
        assertTrue(metricsLatch.await(3, TimeUnit.SECONDS));
        
        assertEquals(2, executionCounter.get());
        assertEquals(2, metricsCounter.get());
    }

    @Test
    void testThreadNaming() throws InterruptedException {
        CountDownLatch executionLatch = new CountDownLatch(1);
        CountDownLatch metricsLatch = new CountDownLatch(1);
        
        AtomicInteger executionThreadNameCheck = new AtomicInteger(0);
        AtomicInteger metricsThreadNameCheck = new AtomicInteger(0);

        // Test execution thread naming
        executionSubmissionExecutor.execute(() -> {
            try {
                String threadName = Thread.currentThread().getName();
                if (threadName.contains("execution-submit-")) {
                    executionThreadNameCheck.incrementAndGet();
                }
            } finally {
                executionLatch.countDown();
            }
        });

        // Test metrics thread naming
        metricsRecordingExecutor.execute(() -> {
            try {
                String threadName = Thread.currentThread().getName();
                if (threadName.contains("metrics-")) {
                    metricsThreadNameCheck.incrementAndGet();
                }
            } finally {
                metricsLatch.countDown();
            }
        });

        assertTrue(executionLatch.await(2, TimeUnit.SECONDS));
        assertTrue(metricsLatch.await(2, TimeUnit.SECONDS));
        
        assertEquals(1, executionThreadNameCheck.get());
        assertEquals(1, metricsThreadNameCheck.get());
    }
}