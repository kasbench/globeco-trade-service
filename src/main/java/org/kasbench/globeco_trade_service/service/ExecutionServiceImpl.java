package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.entity.*;
import org.kasbench.globeco_trade_service.repository.*;
import org.kasbench.globeco_trade_service.dto.ExecutionPutFillDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@Service
public class ExecutionServiceImpl implements ExecutionService {
    private final ExecutionRepository executionRepository;
    private final ExecutionStatusRepository executionStatusRepository;
    private final BlotterRepository blotterRepository;
    private final TradeTypeRepository tradeTypeRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final DestinationRepository destinationRepository;
    private final RestTemplate restTemplate;
    @Value("${execution.service.base-url:http://globeco-execution-service:8084}")
    private String executionServiceBaseUrl;

    @Autowired
    public ExecutionServiceImpl(
            ExecutionRepository executionRepository,
            ExecutionStatusRepository executionStatusRepository,
            BlotterRepository blotterRepository,
            TradeTypeRepository tradeTypeRepository,
            TradeOrderRepository tradeOrderRepository,
            DestinationRepository destinationRepository,
            RestTemplate restTemplate) {
        this.executionRepository = executionRepository;
        this.executionStatusRepository = executionStatusRepository;
        this.blotterRepository = blotterRepository;
        this.tradeTypeRepository = tradeTypeRepository;
        this.tradeOrderRepository = tradeOrderRepository;
        this.destinationRepository = destinationRepository;
        this.restTemplate = restTemplate;
    }

    @Override
    @Cacheable(value = "executions", cacheManager = "cacheManager")
    public List<Execution> getAllExecutions() {
        return executionRepository.findAll();
    }

    @Override
    @Cacheable(value = "executions", key = "#id", cacheManager = "cacheManager")
    public Optional<Execution> getExecutionById(Integer id) {
        return executionRepository.findById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "executions", allEntries = true, cacheManager = "cacheManager")
    public Execution createExecution(Execution execution) {
        execution.setId(null); // Ensure ID is not set for new entity
        resolveRelationships(execution);
        // Set executionServiceId if provided
        if (execution.getExecutionServiceId() != null) {
            execution.setExecutionServiceId(execution.getExecutionServiceId());
        }
        return executionRepository.save(execution);
    }

    @Override
    @Transactional
    @CacheEvict(value = "executions", allEntries = true, cacheManager = "cacheManager")
    public Execution updateExecution(Integer id, Execution execution) {
        Execution existing = executionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + id));
        existing.setExecutionTimestamp(execution.getExecutionTimestamp());
        existing.setQuantityOrdered(execution.getQuantityOrdered());
        existing.setQuantityPlaced(execution.getQuantityPlaced());
        existing.setQuantityFilled(execution.getQuantityFilled());
        existing.setLimitPrice(execution.getLimitPrice());
        // Set executionServiceId if provided
        existing.setExecutionServiceId(execution.getExecutionServiceId());
        resolveRelationshipsForUpdate(existing, execution);
        return executionRepository.save(existing);
    }

    @Override
    @Transactional
    @CacheEvict(value = "executions", allEntries = true, cacheManager = "cacheManager")
    public void deleteExecution(Integer id, Integer version) {
        Execution existing = executionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + id));
        if (!existing.getVersion().equals(version)) {
            throw new IllegalArgumentException("Version mismatch for execution: " + id);
        }
        executionRepository.deleteById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "executions", allEntries = true, cacheManager = "cacheManager")
    public Execution fillExecution(Integer id, ExecutionPutFillDTO fillDTO) {
        // Find the execution
        Execution existing = executionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found with id: " + id));
        
        // Check version for optimistic locking
        if (!existing.getVersion().equals(fillDTO.getVersion())) {
            throw new IllegalArgumentException("Version mismatch. Expected version: " + existing.getVersion() + ", provided: " + fillDTO.getVersion());
        }
        
        // Validate execution status
        ExecutionStatus newStatus = executionStatusRepository.findByAbbreviation(fillDTO.getExecutionStatus())
                .orElseThrow(() -> new IllegalArgumentException("Invalid execution status: " + fillDTO.getExecutionStatus() + ". Valid values are: NEW, SENT, PART, FILL, CANC"));
        
        // Validate quantity filled
        if (fillDTO.getQuantityFilled().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity filled cannot be negative");
        }
        
        if (fillDTO.getQuantityFilled().compareTo(existing.getQuantityPlaced()) > 0) {
            throw new IllegalArgumentException("Quantity filled (" + fillDTO.getQuantityFilled() + ") cannot exceed quantity placed (" + existing.getQuantityPlaced() + ")");
        }
        
        // Update only the specified fields
        existing.setQuantityFilled(fillDTO.getQuantityFilled());
        existing.setExecutionStatus(newStatus);
        
        // Save and return
        return executionRepository.save(existing);
    }

    @Override
    @Transactional
    public SubmitResult submitExecution(Integer id) {
        Optional<Execution> opt = executionRepository.findById(id);
        if (opt.isEmpty()) {
            return new SubmitResult(null, "Execution not found");
        }
        Execution execution = opt.get();
        // Build DTO for external service
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("executionStatus", execution.getExecutionStatus().getAbbreviation());
        payload.put("tradeType", execution.getTradeType().getAbbreviation());
        payload.put("destination", execution.getDestination().getAbbreviation());
        payload.put("securityId", execution.getTradeOrder().getSecurityId());
        payload.put("quantity", execution.getQuantityOrdered());
        payload.put("limitPrice", execution.getLimitPrice());
        payload.put("tradeServiceExecutionId", execution.getId());
        payload.put("version", 1);
        String url = executionServiceBaseUrl + "/api/v1/executions";
        try {
            ResponseEntity<java.util.Map> response = restTemplate.postForEntity(url, payload, java.util.Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().get("id") != null) {
                Integer extId = (Integer) response.getBody().get("id");
                execution.setExecutionServiceId(extId);
                // Set quantityPlaced to quantityOrdered
                execution.setQuantityPlaced(execution.getQuantityOrdered());
                // Set status to SENT (id=2)
                ExecutionStatus sentStatus = executionStatusRepository.findById(2).orElse(null);
                if (sentStatus != null) {
                    execution.setExecutionStatus(sentStatus);
                }
                executionRepository.save(execution);
                return new SubmitResult("submitted", null);
            } else {
                return new SubmitResult(null, "Unexpected response from execution service");
            }
        } catch (HttpStatusCodeException ex) {
            org.springframework.http.HttpStatus status = org.springframework.http.HttpStatus.valueOf(ex.getRawStatusCode());
            if (status.is4xxClientError()) {
                return new SubmitResult(null, "Client error: " + ex.getResponseBodyAsString());
            } else if (status.is5xxServerError()) {
                return new SubmitResult(null, "Failed to submit execution: execution service unavailable");
            } else {
                return new SubmitResult(null, "Error: " + ex.getMessage());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return new SubmitResult(null, "Error: " + ex.getMessage());
        }
    }

    private void resolveRelationships(Execution execution) {
        if (execution.getExecutionStatus() != null && execution.getExecutionStatus().getId() != null) {
            ExecutionStatus status = executionStatusRepository.findById(execution.getExecutionStatus().getId())
                    .orElseThrow(() -> new IllegalArgumentException("ExecutionStatus not found: " + execution.getExecutionStatus().getId()));
            execution.setExecutionStatus(status);
        } else {
            throw new IllegalArgumentException("ExecutionStatus is required");
        }
        if (execution.getBlotter() != null && execution.getBlotter().getId() != null) {
            Blotter blotter = blotterRepository.findById(execution.getBlotter().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Blotter not found: " + execution.getBlotter().getId()));
            execution.setBlotter(blotter);
        } else {
            execution.setBlotter(null);
        }
        if (execution.getTradeType() != null && execution.getTradeType().getId() != null) {
            TradeType tradeType = tradeTypeRepository.findById(execution.getTradeType().getId())
                    .orElseThrow(() -> new IllegalArgumentException("TradeType not found: " + execution.getTradeType().getId()));
            execution.setTradeType(tradeType);
        } else {
            execution.setTradeType(null);
        }
        if (execution.getTradeOrder() != null && execution.getTradeOrder().getId() != null) {
            TradeOrder tradeOrder = tradeOrderRepository.findById(execution.getTradeOrder().getId())
                    .orElseThrow(() -> new IllegalArgumentException("TradeOrder not found: " + execution.getTradeOrder().getId()));
            execution.setTradeOrder(tradeOrder);
        } else {
            throw new IllegalArgumentException("TradeOrder is required");
        }
        if (execution.getDestination() != null && execution.getDestination().getId() != null) {
            Destination destination = destinationRepository.findById(execution.getDestination().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Destination not found: " + execution.getDestination().getId()));
            execution.setDestination(destination);
        } else {
            throw new IllegalArgumentException("Destination is required");
        }
    }

    private void resolveRelationshipsForUpdate(Execution existing, Execution incoming) {
        if (incoming.getExecutionStatus() != null && incoming.getExecutionStatus().getId() != null) {
            ExecutionStatus status = executionStatusRepository.findById(incoming.getExecutionStatus().getId())
                    .orElseThrow(() -> new IllegalArgumentException("ExecutionStatus not found: " + incoming.getExecutionStatus().getId()));
            existing.setExecutionStatus(status);
        }
        if (incoming.getBlotter() != null && incoming.getBlotter().getId() != null) {
            Blotter blotter = blotterRepository.findById(incoming.getBlotter().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Blotter not found: " + incoming.getBlotter().getId()));
            existing.setBlotter(blotter);
        } else {
            existing.setBlotter(null);
        }
        if (incoming.getTradeType() != null && incoming.getTradeType().getId() != null) {
            TradeType tradeType = tradeTypeRepository.findById(incoming.getTradeType().getId())
                    .orElseThrow(() -> new IllegalArgumentException("TradeType not found: " + incoming.getTradeType().getId()));
            existing.setTradeType(tradeType);
        } else {
            existing.setTradeType(null);
        }
        if (incoming.getTradeOrder() != null && incoming.getTradeOrder().getId() != null) {
            TradeOrder tradeOrder = tradeOrderRepository.findById(incoming.getTradeOrder().getId())
                    .orElseThrow(() -> new IllegalArgumentException("TradeOrder not found: " + incoming.getTradeOrder().getId()));
            existing.setTradeOrder(tradeOrder);
        }
        if (incoming.getDestination() != null && incoming.getDestination().getId() != null) {
            Destination destination = destinationRepository.findById(incoming.getDestination().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Destination not found: " + incoming.getDestination().getId()));
            existing.setDestination(destination);
        }
    }
} 