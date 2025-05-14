package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.entity.Blotter;
import org.kasbench.globeco_trade_service.repository.BlotterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BlotterServiceImpl implements BlotterService {

    private final BlotterRepository blotterRepository;

    @Autowired
    public BlotterServiceImpl(BlotterRepository blotterRepository) {
        this.blotterRepository = blotterRepository;
    }

    @Override
    public List<Blotter> getAllBlotters() {
        return blotterRepository.findAll();
    }

    @Override
    public Optional<Blotter> getBlotterById(Integer id) {
        return blotterRepository.findById(id);
    }

    @Override
    @Transactional
    public Blotter createBlotter(Blotter blotter) {
        blotter.setId(null); // Ensure ID is not set for new entity
        return blotterRepository.save(blotter);
    }

    @Override
    @Transactional
    public Blotter updateBlotter(Integer id, Blotter blotter) {
        Blotter existing = blotterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Blotter not found: " + id));
        existing.setAbbreviation(blotter.getAbbreviation());
        existing.setName(blotter.getName());
        return blotterRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteBlotter(Integer id, Integer version) {
        Blotter existing = blotterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Blotter not found: " + id));
        if (!existing.getVersion().equals(version)) {
            throw new IllegalArgumentException("Version mismatch for blotter: " + id);
        }
        blotterRepository.deleteById(id);
    }
} 