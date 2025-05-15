package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.entity.TradeType;
import org.kasbench.globeco_trade_service.repository.TradeTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TradeTypeServiceImpl implements TradeTypeService {
    private final TradeTypeRepository tradeTypeRepository;

    @Autowired
    public TradeTypeServiceImpl(TradeTypeRepository tradeTypeRepository) {
        this.tradeTypeRepository = tradeTypeRepository;
    }

    @Override
    @Cacheable(value = "tradeTypes", cacheManager = "cacheManager")
    public List<TradeType> getAllTradeTypes() {
        return tradeTypeRepository.findAll();
    }

    @Override
    @Cacheable(value = "tradeTypes", key = "#id", cacheManager = "cacheManager")
    public Optional<TradeType> getTradeTypeById(Integer id) {
        return tradeTypeRepository.findById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "tradeTypes", allEntries = true, cacheManager = "cacheManager")
    public TradeType createTradeType(TradeType tradeType) {
        tradeType.setId(null); // Ensure ID is not set for new entity
        return tradeTypeRepository.save(tradeType);
    }

    @Override
    @Transactional
    @CacheEvict(value = "tradeTypes", allEntries = true, cacheManager = "cacheManager")
    public TradeType updateTradeType(Integer id, TradeType tradeType) {
        TradeType existing = tradeTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TradeType not found: " + id));
        existing.setAbbreviation(tradeType.getAbbreviation());
        existing.setDescription(tradeType.getDescription());
        return tradeTypeRepository.save(existing);
    }

    @Override
    @Transactional
    @CacheEvict(value = "tradeTypes", allEntries = true, cacheManager = "cacheManager")
    public void deleteTradeType(Integer id, Integer version) {
        TradeType existing = tradeTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TradeType not found: " + id));
        if (!existing.getVersion().equals(version)) {
            throw new IllegalArgumentException("Version mismatch for tradeType: " + id);
        }
        tradeTypeRepository.deleteById(id);
    }
} 