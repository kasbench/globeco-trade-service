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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.math.BigDecimal;
import jakarta.annotation.PostConstruct;

@Service
public class ExecutionServiceImpl implements ExecutionService {
    private final ExecutionRepository executionRepository;
    private final ExecutionStatusRepository executionStatusRepository;
    private final BlotterRepository blotterRepository;
    private final TradeTypeRepository tradeTypeRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final DestinationRepository destinationRepository;
    private final RestTemplate restTemplate;
    private final org.springframework.retry.support.RetryTemplate retryTemplate;
    @Value("${execution.service.base-url:http://globeco-execution-service:8084}")
    private String executionServiceBaseUrl;

    // Cache for execution statuses - loaded once at startup
    private final Map<Integer, ExecutionStatus> executionStatusCache = new ConcurrentHashMap<>();
    private final Map<String, ExecutionStatus> executionStatusByAbbreviationCache = new ConcurrentHashMap<>();

    @Autowired
    public ExecutionServiceImpl(
            ExecutionRepository executionRepository,
            ExecutionStatusRepository executionStatusRepository,
            BlotterRepository blotterRepository,
            TradeTypeRepository tradeTypeRepository,
            TradeOrderRepository tradeOrderRepository,
            DestinationRepository destinationRepository,
            @org.springframework.beans.factory.annotation.Qualifier("executionServiceRestTemplate") RestTemplate restTemplate,
            @org.springframework.beans.factory.annotation.Qualifier("executionServiceRetryTemplate") org.springframework.retry.support.RetryTemplate retryTemplate) {
        this.executionRepository = executionRepository;
        this.executionStatusRepository = executionStatusRepository;
        this.blotterRepository = blotterRepository;
        this.tradeTypeRepository = tradeTypeRepository;
        this.tradeOrderRepository = tradeOrderRepository;
        this.destinationRepository = destinationRepository;
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
    }

    @PostConstruct
    private void initializeExecutionStatusCache() {
        List<ExecutionStatus> statuses = executionStatusRepository.findAll();
        for (ExecutionStatus status : statuses) {
            executionStatusCache.put(status.getId(), status);
            executionStatusByAbbreviationCache.put(status.getAbbreviation(), status);
        }
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ExecutionServiceImpl.class);
        logger.info("Initialized execution status cache with {} statuses", statuses.size());
    }

    /**
     * Get execution status by ID from cache
     */
    private ExecutionStatus getExecutionStatusById(Integer id) {
        return executionStatusCache.get(id);
    }

    /**
     * Get execution status by abbreviation from cache
     */
    private ExecutionStatus getExecutionStatusByAbbreviation(String abbreviation) {
        return executionStatusByAbbreviationCache.get(abbreviation);
    }

    @Override
    @Cacheable(value = "executions", cacheManager = "cacheManager")
    public List<Execution> getAllExecutions() {
        return executionRepository.findAll();
    }

    @Override
    @Cacheable(value = "executions", cacheManager = "cacheManager")
    public PaginatedResult<Execution> getAllExecutions(Integer limit, Integer offset) {
        if (limit == null && offset == null) {
            // No pagination requested, return all data
            List<Execution> all = executionRepository.findAll();
            return new PaginatedResult<>(all, all.size());
        }

        // Create pageable for pagination
        Pageable pageable;
        if (limit != null && offset != null) {
            pageable = PageRequest.of(offset / limit, limit);
        } else if (limit != null) {
            // Only limit provided, start from beginning
            pageable = PageRequest.of(0, limit);
        } else {
            // Only offset provided, use default page size of 50
            pageable = PageRequest.of(offset / 50, 50);
        }

        Page<Execution> page = executionRepository.findAll(pageable);
        return new PaginatedResult<>(page.getContent(), page.getTotalElements());
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
            throw new IllegalArgumentException("Version mismatch. Expected version: " + existing.getVersion()
                    + ", provided: " + fillDTO.getVersion());
        }

        // Validate execution status
        ExecutionStatus newStatus = getExecutionStatusByAbbreviation(fillDTO.getExecutionStatus());
        if (newStatus == null) {
            throw new IllegalArgumentException("Invalid execution status: "
                    + fillDTO.getExecutionStatus() + ". Valid values are: NEW, SENT, PART, FILL, CANC");
        }

        // Validate quantity filled
        if (fillDTO.getQuantityFilled().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity filled cannot be negative");
        }

        if (fillDTO.getQuantityFilled().compareTo(existing.getQuantityPlaced()) > 0) {
            throw new IllegalArgumentException("Quantity filled (" + fillDTO.getQuantityFilled()
                    + ") cannot exceed quantity placed (" + existing.getQuantityPlaced() + ")");
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
        long startTime = System.currentTimeMillis();
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ExecutionServiceImpl.class);

        try {
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

            // Use retry template with exponential backoff for external service call
            ResponseEntity<java.util.Map<String, Object>> response = retryTemplate.execute(context -> {
                logger.debug("Attempting execution service submission for execution {} (attempt {})",
                        id, context.getRetryCount() + 1);

                long apiCallStartTime = System.currentTimeMillis();
                ResponseEntity<java.util.Map<String, Object>> result = restTemplate.postForEntity(
                        url, payload, (Class<java.util.Map<String, Object>>) (Class<?>) java.util.Map.class);
                long apiCallDuration = System.currentTimeMillis() - apiCallStartTime;

                logger.info("(Execution Service) Execution service API call completed in {} ms for execution {}",
                        apiCallDuration, id);
                logger.debug("Execution service responded with status: {}", result.getStatusCode());
                return result;
            });

            if (response.getStatusCode().is2xxSuccessful() &&
                    response.getBody() != null &&
                    response.getBody().get("id") != null) {

                Integer extId = (Integer) response.getBody().get("id");
                execution.setExecutionServiceId(extId);

                // Set quantityPlaced to quantityOrdered
                execution.setQuantityPlaced(execution.getQuantityOrdered());

                // Set status to SENT (id=2)
                ExecutionStatus sentStatus = getExecutionStatusById(2);
                if (sentStatus != null) {
                    execution.setExecutionStatus(sentStatus);
                }

                executionRepository.save(execution);
                return new SubmitResult("submitted", null);
            } else {
                return new SubmitResult(null, "Unexpected response from execution service");
            }

        } catch (HttpStatusCodeException ex) {
            org.springframework.http.HttpStatusCode statusCode = ex.getStatusCode();
            if (statusCode.is4xxClientError()) {
                return new SubmitResult(null, "Client error: " + ex.getResponseBodyAsString());
            } else if (statusCode.is5xxServerError()) {
                return new SubmitResult(null,
                        "Failed to submit execution: execution service unavailable (HTTP " + statusCode.value() + ")");
            } else {
                return new SubmitResult(null, "Error: HTTP " + statusCode.value() + " - " + ex.getMessage());
            }
        } catch (org.springframework.web.client.ResourceAccessException ex) {
            // This includes timeout exceptions
            return new SubmitResult(null,
                    "Failed to submit execution: execution service timeout or connection error - " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error during execution service submission for execution {}: {}", id,
                    ex.getMessage(), ex);
            return new SubmitResult(null, "Error: " + ex.getMessage());
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.info("(Execution Service) submitExecution method completed for execution {} in {} ms", id,
                    executionTime);
        }
    }

    private void resolveRelationships(Execution execution) {
        if (execution.getExecutionStatus() != null && execution.getExecutionStatus().getId() != null) {
            ExecutionStatus status = getExecutionStatusById(execution.getExecutionStatus().getId());
            if (status == null) {
                throw new IllegalArgumentException(
                        "ExecutionStatus not found: " + execution.getExecutionStatus().getId());
            }
            execution.setExecutionStatus(status);
        } else {
            throw new IllegalArgumentException("ExecutionStatus is required");
        }
        if (execution.getBlotter() != null && execution.getBlotter().getId() != null) {
            Blotter blotter = blotterRepository.findById(execution.getBlotter().getId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Blotter not found: " + execution.getBlotter().getId()));
            execution.setBlotter(blotter);
        } else {
            execution.setBlotter(null);
        }
        if (execution.getTradeType() != null && execution.getTradeType().getId() != null) {
            TradeType tradeType = tradeTypeRepository.findById(execution.getTradeType().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "TradeType not found: " + execution.getTradeType().getId()));
            execution.setTradeType(tradeType);
        } else {
            execution.setTradeType(null);
        }
        if (execution.getTradeOrder() != null && execution.getTradeOrder().getId() != null) {
            TradeOrder tradeOrder = tradeOrderRepository.findById(execution.getTradeOrder().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "TradeOrder not found: " + execution.getTradeOrder().getId()));
            execution.setTradeOrder(tradeOrder);
        } else {
            throw new IllegalArgumentException("TradeOrder is required");
        }
        if (execution.getDestination() != null && execution.getDestination().getId() != null) {
            Destination destination = destinationRepository.findById(execution.getDestination().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Destination not found: " + execution.getDestination().getId()));
            execution.setDestination(destination);
        } else {
            throw new IllegalArgumentException("Destination is required");
        }
    }

    private void resolveRelationshipsForUpdate(Execution existing, Execution incoming) {
        if (incoming.getExecutionStatus() != null && incoming.getExecutionStatus().getId() != null) {
            ExecutionStatus status = getExecutionStatusById(incoming.getExecutionStatus().getId());
            if (status == null) {
                throw new IllegalArgumentException(
                        "ExecutionStatus not found: " + incoming.getExecutionStatus().getId());
            }
            existing.setExecutionStatus(status);
        }
        if (incoming.getBlotter() != null && incoming.getBlotter().getId() != null) {
            Blotter blotter = blotterRepository.findById(incoming.getBlotter().getId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Blotter not found: " + incoming.getBlotter().getId()));
            existing.setBlotter(blotter);
        } else {
            existing.setBlotter(null);
        }
        if (incoming.getTradeType() != null && incoming.getTradeType().getId() != null) {
            TradeType tradeType = tradeTypeRepository.findById(incoming.getTradeType().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "TradeType not found: " + incoming.getTradeType().getId()));
            existing.setTradeType(tradeType);
        } else {
            existing.setTradeType(null);
        }
        if (incoming.getTradeOrder() != null && incoming.getTradeOrder().getId() != null) {
            TradeOrder tradeOrder = tradeOrderRepository.findById(incoming.getTradeOrder().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "TradeOrder not found: " + incoming.getTradeOrder().getId()));
            existing.setTradeOrder(tradeOrder);
        }
        if (incoming.getDestination() != null && incoming.getDestination().getId() != null) {
            Destination destination = destinationRepository.findById(incoming.getDestination().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Destination not found: " + incoming.getDestination().getId()));
            existing.setDestination(destination);
        }
    }
}