package org.kasbench.globeco_trade_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kasbench.globeco_trade_service.entity.Destination;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class DestinationServiceImplTest extends org.kasbench.globeco_trade_service.AbstractH2Test {
    @Autowired
    private DestinationService destinationService;
    @Autowired
    private CacheManager cacheManager;

    private Destination buildDestination() {
        Destination destination = new Destination();
        destination.setAbbreviation("ML");
        destination.setDescription("Merrill Lynch");
        return destination;
    }

    @BeforeEach
    void clearCache() {
        var cache = cacheManager.getCache("destinations");
        assertNotNull(cache, "'destinations' cache is not configured in CacheManager");
        cache.clear();
    }

    @Test
    void testCreateAndGetDestination() {
        Destination created = destinationService.createDestination(buildDestination());
        assertNotNull(created.getId());
        Optional<Destination> found = destinationService.getDestinationById(created.getId());
        assertTrue(found.isPresent());
        assertEquals(created.getId(), found.get().getId());
    }

    @Test
    void testUpdateDestination() {
        Destination created = destinationService.createDestination(buildDestination());
        created.setDescription("Updated");
        Destination updated = destinationService.updateDestination(created.getId(), created);
        assertEquals("Updated", updated.getDescription());
    }

    @Test
    void testDeleteDestination() {
        Destination created = destinationService.createDestination(buildDestination());
        destinationService.deleteDestination(created.getId(), created.getVersion());
        assertFalse(destinationService.getDestinationById(created.getId()).isPresent());
    }

    @Test
    void testDeleteDestinationVersionMismatch() {
        Destination created = destinationService.createDestination(buildDestination());
        assertThrows(IllegalArgumentException.class, () -> destinationService.deleteDestination(created.getId(), created.getVersion() + 1));
    }

    @SuppressWarnings("null")
    @Test
    void testGetAllDestinationsUsesCache() {
        destinationService.createDestination(buildDestination());
        // First call populates cache
        destinationService.getAllDestinations();
        // Second call should hit cache
        destinationService.getAllDestinations();
        assertNotNull(cacheManager.getCache("destinations").get(SimpleKey.EMPTY));
    }

    @SuppressWarnings("null")
    @Test
    void testGetDestinationByIdUsesCache() {
        Destination created = destinationService.createDestination(buildDestination());
        // First call populates cache
        destinationService.getDestinationById(created.getId());
        // Second call should hit cache
        destinationService.getDestinationById(created.getId());
        assertNotNull(cacheManager.getCache("destinations").get(created.getId()));
    }

    @SuppressWarnings("null")
    @Test
    void testCacheEvictedOnCreateUpdateDelete() {
        destinationService.createDestination(buildDestination());
        destinationService.getAllDestinations();
        assertNotNull(cacheManager.getCache("destinations").get(SimpleKey.EMPTY));
        // Update
        Destination created = destinationService.getAllDestinations().get(0);
        created.setDescription("Updated");
        destinationService.updateDestination(created.getId(), created);
        assertNull(cacheManager.getCache("destinations").get(SimpleKey.EMPTY));
        // Create again
        destinationService.createDestination(buildDestination());
        destinationService.getAllDestinations();
        assertNotNull(cacheManager.getCache("destinations").get(SimpleKey.EMPTY));
        // Delete
        Destination toDelete = destinationService.getAllDestinations().get(0);
        destinationService.deleteDestination(toDelete.getId(), toDelete.getVersion());
        assertNull(cacheManager.getCache("destinations").get(SimpleKey.EMPTY));
    }
} 