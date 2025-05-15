package org.kasbench.globeco_trade_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Assertions;
import org.kasbench.globeco_trade_service.entity.Blotter;
import org.kasbench.globeco_trade_service.repository.BlotterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.CacheManager;
import java.util.Optional;

@SpringBootTest
public class BlotterServiceImplTest extends org.kasbench.globeco_trade_service.AbstractPostgresContainerTest {
    @Autowired
    private BlotterService blotterService;
    @Autowired
    private BlotterRepository blotterRepository;
    @Autowired
    private CacheManager cacheManager;

    @Test
    @Transactional
    void testCreateAndGet() {
        Blotter blotter = new Blotter();
        blotter.setAbbreviation("EQ");
        blotter.setName("Equity");
        Blotter created = blotterService.createBlotter(blotter);
        Assertions.assertNotNull(created.getId());
        Blotter found = blotterService.getBlotterById(created.getId()).orElseThrow();
        Assertions.assertEquals("EQ", found.getAbbreviation());
    }

    @Test
    @Transactional
    void testUpdate() {
        Blotter blotter = new Blotter();
        blotter.setAbbreviation("EQ");
        blotter.setName("Equity");
        Blotter created = blotterService.createBlotter(blotter);
        created.setName("Equity Updated");
        Blotter updated = blotterService.updateBlotter(created.getId(), created);
        Assertions.assertEquals("Equity Updated", updated.getName());
    }

    @Test
    @Transactional
    void testDelete() {
        Blotter blotter = new Blotter();
        blotter.setAbbreviation("EQ");
        blotter.setName("Equity");
        Blotter created = blotterService.createBlotter(blotter);
        blotterService.deleteBlotter(created.getId(), created.getVersion());
        Assertions.assertTrue(blotterService.getBlotterById(created.getId()).isEmpty());
    }

    @Test
    void testGetBlotterByIdUsesCache() {
        Blotter blotter = new Blotter();
        blotter.setAbbreviation("EQ");
        blotter.setName("Equity");
        Blotter created = blotterService.createBlotter(blotter);
        Integer id = created.getId();

        // First call: should hit DB
        Optional<Blotter> found1 = blotterService.getBlotterById(id);
        Assertions.assertTrue(found1.isPresent());

        // Remove from DB directly
        blotterRepository.deleteById(id);

        // Second call: should hit cache, still returns the entity
        Optional<Blotter> found2 = blotterService.getBlotterById(id);
        Assertions.assertTrue(found2.isPresent());
    }

    @Test
    void testGetAllBlottersUsesCache() {
        Blotter blotter = new Blotter();
        blotter.setAbbreviation("EQ");
        blotter.setName("Equity");
        blotterService.createBlotter(blotter);

        // First call: should hit DB
        Assertions.assertFalse(blotterService.getAllBlotters().isEmpty());

        // Remove all from DB directly
        blotterRepository.deleteAll();

        // Second call: should hit cache, still returns the entity
        Assertions.assertFalse(blotterService.getAllBlotters().isEmpty());
    }

    @SuppressWarnings("null")
    @Test
    void testCacheEvictedOnCreateUpdateDelete() {
        Blotter blotter = new Blotter();
        blotter.setAbbreviation("EQ");
        blotter.setName("Equity");
        Blotter created = blotterService.createBlotter(blotter);
        Integer id = created.getId();

        // Prime the cache
        blotterService.getBlotterById(id);
        Assertions.assertNotNull(cacheManager.getCache("blotters").get(id));

        // Update should evict cache
        created.setName("Updated");
        blotterService.updateBlotter(id, created);
        Assertions.assertNull(cacheManager.getCache("blotters").get(id));

        // Prime the cache again
        blotterService.getBlotterById(id);
        Assertions.assertNotNull(cacheManager.getCache("blotters").get(id));

        // Reload the entity to get the latest version
        Blotter updated = blotterRepository.findById(id).orElseThrow();

        // Delete should evict cache
        blotterService.deleteBlotter(id, updated.getVersion());
        Assertions.assertNull(cacheManager.getCache("blotters").get(id));
    }
} 