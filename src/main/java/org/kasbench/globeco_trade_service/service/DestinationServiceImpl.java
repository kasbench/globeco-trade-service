package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.entity.Destination;
import org.kasbench.globeco_trade_service.repository.DestinationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DestinationServiceImpl implements DestinationService {
    private final DestinationRepository destinationRepository;

    @Autowired
    public DestinationServiceImpl(DestinationRepository destinationRepository) {
        this.destinationRepository = destinationRepository;
    }

    @Override
    @Cacheable(value = "destinations", cacheManager = "cacheManager")
    public List<Destination> getAllDestinations() {
        return destinationRepository.findAll();
    }

    @Override
    @Cacheable(value = "destinations", key = "#id", cacheManager = "cacheManager")
    public Optional<Destination> getDestinationById(Integer id) {
        return destinationRepository.findById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "destinations", allEntries = true, cacheManager = "cacheManager")
    public Destination createDestination(Destination destination) {
        destination.setId(null); // Ensure ID is not set for new entity
        return destinationRepository.save(destination);
    }

    @Override
    @Transactional
    @CacheEvict(value = "destinations", allEntries = true, cacheManager = "cacheManager")
    public Destination updateDestination(Integer id, Destination destination) {
        Destination existing = destinationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Destination not found: " + id));
        existing.setAbbreviation(destination.getAbbreviation());
        existing.setDescription(destination.getDescription());
        return destinationRepository.save(existing);
    }

    @Override
    @Transactional
    @CacheEvict(value = "destinations", allEntries = true, cacheManager = "cacheManager")
    public void deleteDestination(Integer id, Integer version) {
        Destination existing = destinationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Destination not found: " + id));
        if (!existing.getVersion().equals(version)) {
            throw new IllegalArgumentException("Version mismatch for destination: " + id);
        }
        destinationRepository.deleteById(id);
    }
} 