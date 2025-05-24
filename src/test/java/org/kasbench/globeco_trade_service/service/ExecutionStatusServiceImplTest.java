package org.kasbench.globeco_trade_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kasbench.globeco_trade_service.entity.ExecutionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ExecutionStatusServiceImplTest extends org.kasbench.globeco_trade_service.AbstractPostgresContainerTest {
    @Autowired
    private ExecutionStatusService executionStatusService;
    @Autowired
    private CacheManager cacheManager;

    private ExecutionStatus buildExecutionStatus() {
        ExecutionStatus status = new ExecutionStatus();
        status.setAbbreviation("NEW" + System.nanoTime());
        status.setDescription("New");
        return status;
    }

    @BeforeEach
    void clearCache() {
        var cache = cacheManager.getCache("executionStatuses");
        assertNotNull(cache, "'executionStatuses' cache is not configured in CacheManager");
        cache.clear();
    }

    @Test
    void testCreateAndGetExecutionStatus() {
        ExecutionStatus created = executionStatusService.createExecutionStatus(buildExecutionStatus());
        assertNotNull(created.getId());
        Optional<ExecutionStatus> found = executionStatusService.getExecutionStatusById(created.getId());
        assertTrue(found.isPresent());
        assertEquals(created.getId(), found.get().getId());
    }

    @Test
    void testUpdateExecutionStatus() {
        ExecutionStatus created = executionStatusService.createExecutionStatus(buildExecutionStatus());
        created.setDescription("Updated");
        ExecutionStatus updated = executionStatusService.updateExecutionStatus(created.getId(), created);
        assertEquals("Updated", updated.getDescription());
    }

    @Test
    void testDeleteExecutionStatus() {
        ExecutionStatus created = executionStatusService.createExecutionStatus(buildExecutionStatus());
        executionStatusService.deleteExecutionStatus(created.getId(), created.getVersion());
        assertFalse(executionStatusService.getExecutionStatusById(created.getId()).isPresent());
    }

    @Test
    void testDeleteExecutionStatusVersionMismatch() {
        ExecutionStatus created = executionStatusService.createExecutionStatus(buildExecutionStatus());
        assertThrows(IllegalArgumentException.class, () -> executionStatusService.deleteExecutionStatus(created.getId(), created.getVersion() + 1));
    }

    @SuppressWarnings("null")
    @Test
    void testGetAllExecutionStatusesUsesCache() {
        executionStatusService.createExecutionStatus(buildExecutionStatus());
        // First call populates cache
        executionStatusService.getAllExecutionStatuses();
        // Second call should hit cache
        executionStatusService.getAllExecutionStatuses();
        assertNotNull(cacheManager.getCache("executionStatuses").get(org.springframework.cache.interceptor.SimpleKey.EMPTY));
    }

    @SuppressWarnings("null")
    @Test
    void testGetExecutionStatusByIdUsesCache() {
        ExecutionStatus created = executionStatusService.createExecutionStatus(buildExecutionStatus());
        // First call populates cache
        executionStatusService.getExecutionStatusById(created.getId());
        // Second call should hit cache
        executionStatusService.getExecutionStatusById(created.getId());
        assertNotNull(cacheManager.getCache("executionStatuses").get(created.getId()));
    }

    @SuppressWarnings("null")
    @Test
    void testCacheEvictedOnCreateUpdateDelete() {
        executionStatusService.createExecutionStatus(buildExecutionStatus());
        executionStatusService.getAllExecutionStatuses();
        assertNotNull(cacheManager.getCache("executionStatuses").get(org.springframework.cache.interceptor.SimpleKey.EMPTY));
        // Update
        ExecutionStatus created = executionStatusService.getAllExecutionStatuses().get(0);
        created.setDescription("Updated");
        executionStatusService.updateExecutionStatus(created.getId(), created);
        assertNull(cacheManager.getCache("executionStatuses").get(org.springframework.cache.interceptor.SimpleKey.EMPTY));
        // Create again
        executionStatusService.createExecutionStatus(buildExecutionStatus());
        executionStatusService.getAllExecutionStatuses();
        assertNotNull(cacheManager.getCache("executionStatuses").get(org.springframework.cache.interceptor.SimpleKey.EMPTY));
        // Delete
        ExecutionStatus toDelete = executionStatusService.getAllExecutionStatuses().get(0);
        executionStatusService.deleteExecutionStatus(toDelete.getId(), toDelete.getVersion());
        assertNull(cacheManager.getCache("executionStatuses").get(org.springframework.cache.interceptor.SimpleKey.EMPTY));
    }
} 