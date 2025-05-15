package org.kasbench.globeco_trade_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kasbench.globeco_trade_service.entity.TradeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TradeTypeServiceImplTest extends org.kasbench.globeco_trade_service.AbstractPostgresContainerTest {
    @Autowired
    private TradeTypeService tradeTypeService;
    @Autowired
    private CacheManager cacheManager;

    private TradeType buildTradeType() {
        TradeType tradeType = new TradeType();
        tradeType.setAbbreviation("BUY");
        tradeType.setDescription("Buy");
        return tradeType;
    }

    @BeforeEach
    void clearCache() {
        var cache = cacheManager.getCache("tradeTypes");
        assertNotNull(cache, "'tradeTypes' cache is not configured in CacheManager");
        cache.clear();
    }

    @Test
    void testCreateAndGetTradeType() {
        TradeType created = tradeTypeService.createTradeType(buildTradeType());
        assertNotNull(created.getId());
        Optional<TradeType> found = tradeTypeService.getTradeTypeById(created.getId());
        assertTrue(found.isPresent());
        assertEquals(created.getId(), found.get().getId());
    }

    @Test
    void testUpdateTradeType() {
        TradeType created = tradeTypeService.createTradeType(buildTradeType());
        created.setDescription("Updated");
        TradeType updated = tradeTypeService.updateTradeType(created.getId(), created);
        assertEquals("Updated", updated.getDescription());
    }

    @Test
    void testDeleteTradeType() {
        TradeType created = tradeTypeService.createTradeType(buildTradeType());
        tradeTypeService.deleteTradeType(created.getId(), created.getVersion());
        assertFalse(tradeTypeService.getTradeTypeById(created.getId()).isPresent());
    }

    @Test
    void testDeleteTradeTypeVersionMismatch() {
        TradeType created = tradeTypeService.createTradeType(buildTradeType());
        assertThrows(IllegalArgumentException.class, () -> tradeTypeService.deleteTradeType(created.getId(), created.getVersion() + 1));
    }

    @SuppressWarnings("null")
    @Test
    void testGetAllTradeTypesUsesCache() {
        tradeTypeService.createTradeType(buildTradeType());
        // First call populates cache
        tradeTypeService.getAllTradeTypes();
        // Second call should hit cache
        tradeTypeService.getAllTradeTypes();
        assertNotNull(cacheManager.getCache("tradeTypes").get(org.springframework.cache.interceptor.SimpleKey.EMPTY));
    }

    @SuppressWarnings("null")
    @Test
    void testGetTradeTypeByIdUsesCache() {
        TradeType created = tradeTypeService.createTradeType(buildTradeType());
        // First call populates cache
        tradeTypeService.getTradeTypeById(created.getId());
        // Second call should hit cache
        tradeTypeService.getTradeTypeById(created.getId());
        assertNotNull(cacheManager.getCache("tradeTypes").get(created.getId()));
    }

    @SuppressWarnings("null")
    @Test
    void testCacheEvictedOnCreateUpdateDelete() {
        tradeTypeService.createTradeType(buildTradeType());
        tradeTypeService.getAllTradeTypes();
        assertNotNull(cacheManager.getCache("tradeTypes").get(org.springframework.cache.interceptor.SimpleKey.EMPTY));
        // Update
        TradeType created = tradeTypeService.getAllTradeTypes().get(0);
        created.setDescription("Updated");
        tradeTypeService.updateTradeType(created.getId(), created);
        assertNull(cacheManager.getCache("tradeTypes").get(org.springframework.cache.interceptor.SimpleKey.EMPTY));
        // Create again
        tradeTypeService.createTradeType(buildTradeType());
        tradeTypeService.getAllTradeTypes();
        assertNotNull(cacheManager.getCache("tradeTypes").get(org.springframework.cache.interceptor.SimpleKey.EMPTY));
        // Delete
        TradeType toDelete = tradeTypeService.getAllTradeTypes().get(0);
        tradeTypeService.deleteTradeType(toDelete.getId(), toDelete.getVersion());
        assertNull(cacheManager.getCache("tradeTypes").get(org.springframework.cache.interceptor.SimpleKey.EMPTY));
    }
} 