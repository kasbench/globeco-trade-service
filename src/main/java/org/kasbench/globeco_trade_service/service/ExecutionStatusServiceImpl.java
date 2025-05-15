package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.entity.ExecutionStatus;
import org.kasbench.globeco_trade_service.repository.ExecutionStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ExecutionStatusServiceImpl implements ExecutionStatusService {
    private final ExecutionStatusRepository executionStatusRepository;

    @Autowired
    public ExecutionStatusServiceImpl(ExecutionStatusRepository executionStatusRepository) {
        this.executionStatusRepository = executionStatusRepository;
    }

    @Override
    @Cacheable(value = "executionStatuses", cacheManager = "cacheManager")
    public List<ExecutionStatus> getAllExecutionStatuses() {
        return executionStatusRepository.findAll();
    }

    @Override
    @Cacheable(value = "executionStatuses", key = "#id", cacheManager = "cacheManager")
    public Optional<ExecutionStatus> getExecutionStatusById(Integer id) {
        return executionStatusRepository.findById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "executionStatuses", allEntries = true, cacheManager = "cacheManager")
    public ExecutionStatus createExecutionStatus(ExecutionStatus executionStatus) {
        executionStatus.setId(null); // Ensure ID is not set for new entity
        return executionStatusRepository.save(executionStatus);
    }

    @Override
    @Transactional
    @CacheEvict(value = "executionStatuses", allEntries = true, cacheManager = "cacheManager")
    public ExecutionStatus updateExecutionStatus(Integer id, ExecutionStatus executionStatus) {
        ExecutionStatus existing = executionStatusRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ExecutionStatus not found: " + id));
        existing.setAbbreviation(executionStatus.getAbbreviation());
        existing.setDescription(executionStatus.getDescription());
        return executionStatusRepository.save(existing);
    }

    @Override
    @Transactional
    @CacheEvict(value = "executionStatuses", allEntries = true, cacheManager = "cacheManager")
    public void deleteExecutionStatus(Integer id, Integer version) {
        ExecutionStatus existing = executionStatusRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ExecutionStatus not found: " + id));
        if (!existing.getVersion().equals(version)) {
            throw new IllegalArgumentException("Version mismatch for executionStatus: " + id);
        }
        executionStatusRepository.deleteById(id);
    }
} 