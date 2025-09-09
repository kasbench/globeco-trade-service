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
import java.util.ArrayList;
import java.math.BigDecimal;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

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
            @org.springframework.beans.factory.annotation.Qualifier("executionServiceRetryTemplate") org.springframework.retry.support.RetryTemplate retryTemplate,
            BulkExecutionSubmissionService bulkExecutionSubmissionService) {
        this.executionRepository = executionRepository;
        this.executionStatusRepository = executionStatusRepository;
        this.blotterRepository = blotterRepository;
        this.tradeTypeRepository = tradeTypeRepository;
        this.tradeOrderRepository = tradeOrderRepository;
        this.destinationRepository = destinationRepository;
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
        this.bulkExecutionSubmissionService = bulkExecutionSubmissionService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeExecutionStatusCache() {
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

        logger.debug("Routing single execution {} through bulk processor", id);

        try {
            // Route through bulk submission with batch size 1 for consistency
            BulkSubmitResult bulkResult = submitExecutions(List.of(id));
            
            // Convert bulk result to single result
            if (bulkResult.getResults().isEmpty()) {
                return new SubmitResult(null, "No results returned from bulk submission");
            }
            
            ExecutionSubmitResult singleResult = bulkResult.getResults().get(0);
            
            if ("SUCCESS".equals(singleResult.getStatus()) || "COMPLETED".equals(singleResult.getStatus())) {
                return new SubmitResult("submitted", null);
            } else {
                return new SubmitResult(null, singleResult.getMessage());
            }
            
        } catch (Exception ex) {
            logger.error("Error routing single execution {} through bulk processor: {}", id, ex.getMessage(), ex);
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

    /**
     * Splits a list of execution IDs into batches of the specified size.
     */
    private List<List<Integer>> splitExecutionIds(List<Integer> executionIds, int batchSize) {
        List<List<Integer>> batches = new ArrayList<>();
        
        for (int i = 0; i < executionIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, executionIds.size());
            List<Integer> batch = executionIds.subList(i, endIndex);
            batches.add(new ArrayList<>(batch)); // Create new list to avoid sublist issues
        }
        
        return batches;
    }

    /**
     * Determines the overall status based on success/failure counts.
     */
    private String determineOverallStatus(int successful, int failed, int total) {
        if (failed == 0) {
            return "SUCCESS";
        } else if (successful == 0) {
            return "FAILED";
        } else {
            return "PARTIAL_SUCCESS";
        }
    }

    // Bulk execution submission service
    private final BulkExecutionSubmissionService bulkExecutionSubmissionService;

    @Override
    @Transactional
    public BulkSubmitResult submitExecutions(List<Integer> executionIds) {
        long startTime = System.currentTimeMillis();
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ExecutionServiceImpl.class);
        
        logger.info("Starting bulk execution submission for {} execution IDs", executionIds.size());
        
        try {
            // Use the bulk submission service to handle the request
            org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.BulkSubmitResult bulkResult = 
                bulkExecutionSubmissionService.submitExecutionsBulk(executionIds);
            
            // Convert from ExecutionBatchProcessor.BulkSubmitResult to ExecutionService.BulkSubmitResult
            List<ExecutionSubmitResult> convertedResults = new ArrayList<>();
            for (org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.ExecutionSubmitResult result : bulkResult.getResults()) {
                convertedResults.add(new ExecutionSubmitResult(
                    result.getExecutionId(),
                    result.getStatus(),
                    result.getMessage(),
                    result.getExecutionServiceId()
                ));
            }
            
            BulkSubmitResult serviceResult = new BulkSubmitResult(
                bulkResult.getTotalRequested(),
                bulkResult.getSuccessful(),
                bulkResult.getFailed(),
                convertedResults,
                bulkResult.getOverallStatus(),
                bulkResult.getMessage()
            );
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Bulk execution submission completed in {} ms: {} total, {} successful, {} failed", 
                       duration, serviceResult.getTotalRequested(), 
                       serviceResult.getSuccessful(), serviceResult.getFailed());
            
            return serviceResult;
            
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Bulk execution submission failed after {} ms for {} executions: {}", 
                        duration, executionIds.size(), ex.getMessage(), ex);
            
            // Create failure result
            List<ExecutionSubmitResult> failureResults = new ArrayList<>();
            for (Integer id : executionIds) {
                failureResults.add(new ExecutionSubmitResult(id, "FAILED", ex.getMessage()));
            }
            
            return new BulkSubmitResult(
                executionIds.size(), 0, executionIds.size(),
                failureResults, "FAILED", "Bulk submission failed: " + ex.getMessage()
            );
        }
    }

    @Override
    @Transactional
    public BulkSubmitResult submitExecutionsBatch(List<Integer> executionIds, int batchSize) {
        long startTime = System.currentTimeMillis();
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ExecutionServiceImpl.class);
        
        logger.info("Starting bulk execution submission with custom batch size {} for {} execution IDs", 
                   batchSize, executionIds.size());
        
        try {
            // Validate batch size
            if (batchSize <= 0) {
                throw new IllegalArgumentException("Batch size must be greater than 0");
            }
            
            // For custom batch size, we need to split the executions ourselves
            // and call the bulk service for each batch
            List<List<Integer>> batches = splitExecutionIds(executionIds, batchSize);
            
            List<ExecutionSubmitResult> allResults = new ArrayList<>();
            int totalSuccessful = 0;
            int totalFailed = 0;
            
            for (int i = 0; i < batches.size(); i++) {
                List<Integer> batch = batches.get(i);
                logger.debug("Processing batch {} of {} with {} executions", 
                            i + 1, batches.size(), batch.size());
                
                try {
                    org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.BulkSubmitResult batchResult = 
                        bulkExecutionSubmissionService.submitExecutionsBulk(batch);
                    
                    // Convert and aggregate results
                    for (org.kasbench.globeco_trade_service.service.ExecutionBatchProcessor.ExecutionSubmitResult result : batchResult.getResults()) {
                        allResults.add(new ExecutionSubmitResult(
                            result.getExecutionId(),
                            result.getStatus(),
                            result.getMessage(),
                            result.getExecutionServiceId()
                        ));
                    }
                    
                    totalSuccessful += batchResult.getSuccessful();
                    totalFailed += batchResult.getFailed();
                    
                } catch (Exception ex) {
                    logger.error("Batch {} failed: {}", i + 1, ex.getMessage());
                    
                    // Add failure results for this batch
                    for (Integer id : batch) {
                        allResults.add(new ExecutionSubmitResult(id, "FAILED", "Batch failed: " + ex.getMessage()));
                        totalFailed++;
                    }
                }
            }
            
            String overallStatus = determineOverallStatus(totalSuccessful, totalFailed, executionIds.size());
            String message = String.format("Processed %d batches with size %d: %d successful, %d failed", 
                                          batches.size(), batchSize, totalSuccessful, totalFailed);
            
            BulkSubmitResult result = new BulkSubmitResult(
                executionIds.size(), totalSuccessful, totalFailed,
                allResults, overallStatus, message
            );
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Bulk execution submission with custom batch size completed in {} ms: {} total, {} successful, {} failed", 
                       duration, result.getTotalRequested(), result.getSuccessful(), result.getFailed());
            
            return result;
            
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Bulk execution submission with custom batch size failed after {} ms for {} executions: {}", 
                        duration, executionIds.size(), ex.getMessage(), ex);
            
            // Create failure result
            List<ExecutionSubmitResult> failureResults = new ArrayList<>();
            for (Integer id : executionIds) {
                failureResults.add(new ExecutionSubmitResult(id, "FAILED", ex.getMessage()));
            }
            
            return new BulkSubmitResult(
                executionIds.size(), 0, executionIds.size(),
                failureResults, "FAILED", "Bulk submission with custom batch size failed: " + ex.getMessage()
            );
        }
    }
}