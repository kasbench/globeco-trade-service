package org.kasbench.globeco_trade_service;

import org.kasbench.globeco_trade_service.dto.TradeOrderPostDTO;
import org.kasbench.globeco_trade_service.dto.TradeOrderPutDTO;
import org.kasbench.globeco_trade_service.dto.TradeOrderResponseDTO;
import org.kasbench.globeco_trade_service.dto.BulkTradeOrderRequestDTO;
import org.kasbench.globeco_trade_service.dto.BulkTradeOrderResponseDTO;
import org.kasbench.globeco_trade_service.dto.TradeOrderResultDTO;
import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.kasbench.globeco_trade_service.entity.Blotter;
import org.kasbench.globeco_trade_service.service.TradeOrderService;
import org.kasbench.globeco_trade_service.dto.TradeOrderSubmitDTO;
import org.kasbench.globeco_trade_service.entity.Execution;
import org.kasbench.globeco_trade_service.dto.ExecutionResponseDTO;
import org.kasbench.globeco_trade_service.service.ExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/v1/tradeOrders")
@Validated
public class TradeOrderController {
    private final TradeOrderService tradeOrderService;
    private final ExecutionService executionService;
    private static final Logger logger = LoggerFactory.getLogger(TradeOrderController.class);

    @Autowired
    public TradeOrderController(TradeOrderService tradeOrderService, ExecutionService executionService) {
        this.tradeOrderService = tradeOrderService;
        this.executionService = executionService;
    }

    @GetMapping
    public ResponseEntity<List<TradeOrderResponseDTO>> getAllTradeOrders(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset,
            @RequestParam(name = "order_id", required = false) Integer orderId) {
        
        // Validate pagination parameters
        if (limit != null && (limit < 1 || limit > 1000)) {
            throw new IllegalArgumentException("Limit must be between 1 and 1000");
        }
        if (offset != null && offset < 0) {
            throw new IllegalArgumentException("Offset must be non-negative");
        }
        
        if (limit == null && offset == null && orderId == null) {
            // Backward compatible: no pagination, no filtering
            List<TradeOrderResponseDTO> result = tradeOrderService.getAllTradeOrders().stream()
                    .map(this::toResponseDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } else {
            // Use enhanced method with optional filtering and pagination
            TradeOrderService.PaginatedResult<TradeOrder> paginatedResult = 
                tradeOrderService.getAllTradeOrders(limit, offset, orderId);
            
            List<TradeOrderResponseDTO> result = paginatedResult.getData().stream()
                    .map(this::toResponseDTO)
                    .collect(Collectors.toList());
            
            // Add X-Total-Count header for pagination metadata
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(paginatedResult.getTotalCount()))
                    .body(result);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<TradeOrderResponseDTO> getTradeOrderById(@PathVariable Integer id) {
        Optional<TradeOrder> tradeOrder = tradeOrderService.getTradeOrderById(id);
        return tradeOrder.map(t -> ResponseEntity.ok(toResponseDTO(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TradeOrderResponseDTO> createTradeOrder(@RequestBody TradeOrderPostDTO dto) {
        TradeOrder tradeOrder = fromPostDTO(dto);
        TradeOrder created = tradeOrderService.createTradeOrder(tradeOrder);
        return new ResponseEntity<>(toResponseDTO(created), HttpStatus.CREATED);
    }

    @PostMapping("/bulk")
    public ResponseEntity<BulkTradeOrderResponseDTO> createTradeOrdersBulk(@Valid @RequestBody BulkTradeOrderRequestDTO request) {
        logger.info("Bulk trade order creation requested with {} orders", 
                   request.getTradeOrders() != null ? request.getTradeOrders().size() : 0);
        
        try {
            // Convert DTOs to entities
            List<TradeOrder> tradeOrders = request.getTradeOrders().stream()
                    .map(this::fromPostDTO)
                    .collect(Collectors.toList());
            
            // Call service to create all trade orders in bulk
            List<TradeOrder> createdOrders = tradeOrderService.createTradeOrdersBulk(tradeOrders);
            
            // Build successful response
            List<TradeOrderResultDTO> results = IntStream.range(0, createdOrders.size())
                    .mapToObj(i -> new TradeOrderResultDTO(
                            i,
                            TradeOrderResultDTO.ResultStatus.SUCCESS,
                            "Trade order created successfully",
                            toResponseDTO(createdOrders.get(i))
                    ))
                    .collect(Collectors.toList());
            
            BulkTradeOrderResponseDTO response = new BulkTradeOrderResponseDTO(
                    BulkTradeOrderResponseDTO.BulkStatus.SUCCESS,
                    "All trade orders created successfully",
                    request.getTradeOrders().size(),
                    createdOrders.size(),
                    0,
                    results
            );
            
            logger.info("Successfully created {} trade orders in bulk operation", createdOrders.size());
            return new ResponseEntity<>(response, HttpStatus.CREATED);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Bulk trade order creation failed due to validation error: {}", e.getMessage());
            
            // Build failure response for validation errors
            BulkTradeOrderResponseDTO response = new BulkTradeOrderResponseDTO(
                    BulkTradeOrderResponseDTO.BulkStatus.FAILURE,
                    "Bulk operation failed due to validation errors: " + e.getMessage(),
                    request.getTradeOrders().size(),
                    0,
                    request.getTradeOrders().size(),
                    createFailureResults(request.getTradeOrders().size(), e.getMessage())
            );
            
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            
        } catch (DataIntegrityViolationException e) {
            logger.error("Bulk trade order creation failed due to database constraint violation: {}", e.getMessage());
            
            // Build failure response for database constraint violations
            BulkTradeOrderResponseDTO response = new BulkTradeOrderResponseDTO(
                    BulkTradeOrderResponseDTO.BulkStatus.FAILURE,
                    "Bulk operation failed due to database constraint violations",
                    request.getTradeOrders().size(),
                    0,
                    request.getTradeOrders().size(),
                    createFailureResults(request.getTradeOrders().size(), "Database constraint violation")
            );
            
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            
        } catch (TransactionException e) {
            logger.error("Bulk trade order creation failed due to transaction error: {}", e.getMessage(), e);
            
            // Build failure response for transaction errors
            BulkTradeOrderResponseDTO response = new BulkTradeOrderResponseDTO(
                    BulkTradeOrderResponseDTO.BulkStatus.FAILURE,
                    "Bulk operation failed due to transaction error",
                    request.getTradeOrders().size(),
                    0,
                    request.getTradeOrders().size(),
                    createFailureResults(request.getTradeOrders().size(), "Transaction failed")
            );
            
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            
        } catch (Exception e) {
            logger.error("Unexpected error during bulk trade order creation: {}", e.getMessage(), e);
            
            // Build failure response for unexpected errors
            BulkTradeOrderResponseDTO response = new BulkTradeOrderResponseDTO(
                    BulkTradeOrderResponseDTO.BulkStatus.FAILURE,
                    "Bulk operation failed due to unexpected error",
                    request.getTradeOrders().size(),
                    0,
                    request.getTradeOrders().size(),
                    createFailureResults(request.getTradeOrders().size(), "Internal server error")
            );
            
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TradeOrderResponseDTO> updateTradeOrder(@PathVariable Integer id, @RequestBody TradeOrderPutDTO dto) {
        TradeOrder tradeOrder = fromPutDTO(dto);
        try {
            TradeOrder updated = tradeOrderService.updateTradeOrder(id, tradeOrder);
            return ResponseEntity.ok(toResponseDTO(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTradeOrder(@PathVariable Integer id, @RequestParam Integer version) {
        try {
            tradeOrderService.deleteTradeOrder(id, version);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ExecutionResponseDTO> submitTradeOrder(
            @PathVariable Integer id, 
            @RequestBody TradeOrderSubmitDTO dto,
            @RequestParam(value = "noExecuteSubmit", required = false, defaultValue = "false") boolean noExecuteSubmit) {
        
        logger.info("submitTradeOrder called with id={}, dto={}, noExecuteSubmit={}", id, dto, noExecuteSubmit);
        
        try {
            Execution execution = tradeOrderService.submitTradeOrder(id, dto, noExecuteSubmit);
            ExecutionResponseDTO response;
            try {
                response = toExecutionResponseDTO(execution);
            } catch (Exception mappingEx) {
                logger.error("Exception during toExecutionResponseDTO: {}: {}", mappingEx.getClass().getName(), mappingEx.getMessage(), mappingEx);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            logger.info("Returning ExecutionResponseDTO: {}", response);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Execution service rejected the request")) {
                // External service validation error - return 400 with details
                logger.warn("Execution service rejected request for trade order {}: {}", id, e.getMessage());
                return ResponseEntity.badRequest().build();
            } else if (e.getMessage().contains("not found") && e.getMessage().contains("Destination")) {
                return ResponseEntity.badRequest().build();
            } else if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            } else if (e.getMessage().contains("Unknown order_type")) {
                return ResponseEntity.badRequest().build();
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Failed to submit execution to external service")) {
                // External service failure - return 500
                logger.error("External execution service failure for trade order {}: {}", id, e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            } else {
                logger.error("Runtime exception in submitTradeOrder: {}: {}", e.getClass().getName(), e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            logger.error("Exception in submitTradeOrder: {}: {}", e.getClass().getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private List<TradeOrderResultDTO> createFailureResults(int count, String errorMessage) {
        List<TradeOrderResultDTO> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            results.add(new TradeOrderResultDTO(
                    i,
                    TradeOrderResultDTO.ResultStatus.FAILURE,
                    errorMessage,
                    null
            ));
        }
        return results;
    }

    private TradeOrderResponseDTO toResponseDTO(TradeOrder tradeOrder) {
        TradeOrderResponseDTO dto = new TradeOrderResponseDTO();
        dto.setId(tradeOrder.getId());
        dto.setOrderId(tradeOrder.getOrderId());
        dto.setPortfolioId(tradeOrder.getPortfolioId());
        dto.setOrderType(tradeOrder.getOrderType());
        dto.setSecurityId(tradeOrder.getSecurityId());
        dto.setQuantity(tradeOrder.getQuantity());
        dto.setQuantitySent(tradeOrder.getQuantitySent());
        dto.setLimitPrice(tradeOrder.getLimitPrice());
        dto.setTradeTimestamp(tradeOrder.getTradeTimestamp());
        if (tradeOrder.getBlotter() != null) {
            org.kasbench.globeco_trade_service.dto.BlotterResponseDTO blotterDTO = new org.kasbench.globeco_trade_service.dto.BlotterResponseDTO();
            blotterDTO.setId(tradeOrder.getBlotter().getId());
            blotterDTO.setAbbreviation(tradeOrder.getBlotter().getAbbreviation());
            blotterDTO.setName(tradeOrder.getBlotter().getName());
            blotterDTO.setVersion(tradeOrder.getBlotter().getVersion());
            dto.setBlotter(blotterDTO);
        }
        dto.setSubmitted(tradeOrder.getSubmitted());
        dto.setVersion(tradeOrder.getVersion());
        return dto;
    }

    private TradeOrder fromPostDTO(TradeOrderPostDTO dto) {
        TradeOrder tradeOrder = new TradeOrder();
        tradeOrder.setOrderId(dto.getOrderId());
        tradeOrder.setPortfolioId(dto.getPortfolioId());
        tradeOrder.setOrderType(dto.getOrderType());
        tradeOrder.setSecurityId(dto.getSecurityId());
        tradeOrder.setQuantity(dto.getQuantity());
        tradeOrder.setLimitPrice(dto.getLimitPrice());
        tradeOrder.setTradeTimestamp(dto.getTradeTimestamp());
        tradeOrder.setQuantitySent(java.math.BigDecimal.ZERO);
        if (dto.getBlotterId() != null) {
            Blotter blotter = new Blotter();
            blotter.setId(dto.getBlotterId());
            tradeOrder.setBlotter(blotter);
        }
        return tradeOrder;
    }

    private TradeOrder fromPutDTO(TradeOrderPutDTO dto) {
        TradeOrder tradeOrder = new TradeOrder();
        tradeOrder.setId(dto.getId());
        tradeOrder.setOrderId(dto.getOrderId());
        tradeOrder.setPortfolioId(dto.getPortfolioId());
        tradeOrder.setOrderType(dto.getOrderType());
        tradeOrder.setSecurityId(dto.getSecurityId());
        tradeOrder.setQuantity(dto.getQuantity());
        tradeOrder.setLimitPrice(dto.getLimitPrice());
        tradeOrder.setTradeTimestamp(dto.getTradeTimestamp());
        tradeOrder.setQuantitySent(java.math.BigDecimal.ZERO);
        tradeOrder.setVersion(dto.getVersion());
        if (dto.getBlotterId() != null) {
            Blotter blotter = new Blotter();
            blotter.setId(dto.getBlotterId());
            tradeOrder.setBlotter(blotter);
        }
        tradeOrder.setQuantitySent(java.math.BigDecimal.ZERO);
        return tradeOrder;
    }

    private ExecutionResponseDTO toExecutionResponseDTO(Execution execution) {
        ExecutionResponseDTO dto = new ExecutionResponseDTO();
        dto.setId(execution.getId());
        dto.setExecutionTimestamp(execution.getExecutionTimestamp());
        if (execution.getExecutionStatus() != null) {
            var statusDto = new org.kasbench.globeco_trade_service.dto.ExecutionStatusResponseDTO();
            statusDto.setId(execution.getExecutionStatus().getId());
            statusDto.setAbbreviation(execution.getExecutionStatus().getAbbreviation());
            statusDto.setDescription(execution.getExecutionStatus().getDescription());
            statusDto.setVersion(execution.getExecutionStatus().getVersion());
            dto.setExecutionStatus(statusDto);
        }
        if (execution.getBlotter() != null) {
            var blotterDto = new org.kasbench.globeco_trade_service.dto.BlotterResponseDTO();
            blotterDto.setId(execution.getBlotter().getId());
            blotterDto.setAbbreviation(execution.getBlotter().getAbbreviation());
            blotterDto.setName(execution.getBlotter().getName());
            blotterDto.setVersion(execution.getBlotter().getVersion());
            dto.setBlotter(blotterDto);
        }
        if (execution.getTradeType() != null) {
            var tradeTypeDto = new org.kasbench.globeco_trade_service.dto.TradeTypeResponseDTO();
            tradeTypeDto.setId(execution.getTradeType().getId());
            tradeTypeDto.setAbbreviation(execution.getTradeType().getAbbreviation());
            tradeTypeDto.setDescription(execution.getTradeType().getDescription());
            tradeTypeDto.setVersion(execution.getTradeType().getVersion());
            dto.setTradeType(tradeTypeDto);
        }
        if (execution.getTradeOrder() != null) {
            var tradeOrderDto = new org.kasbench.globeco_trade_service.dto.TradeOrderResponseDTO();
            tradeOrderDto.setId(execution.getTradeOrder().getId());
            tradeOrderDto.setOrderId(execution.getTradeOrder().getOrderId());
            tradeOrderDto.setPortfolioId(execution.getTradeOrder().getPortfolioId());
            tradeOrderDto.setOrderType(execution.getTradeOrder().getOrderType());
            tradeOrderDto.setSecurityId(execution.getTradeOrder().getSecurityId());
            tradeOrderDto.setQuantity(execution.getTradeOrder().getQuantity());
            tradeOrderDto.setLimitPrice(execution.getTradeOrder().getLimitPrice());
            tradeOrderDto.setTradeTimestamp(execution.getTradeOrder().getTradeTimestamp());
            tradeOrderDto.setBlotter(dto.getBlotter());
            tradeOrderDto.setSubmitted(execution.getTradeOrder().getSubmitted());
            tradeOrderDto.setVersion(execution.getTradeOrder().getVersion());
            dto.setTradeOrder(tradeOrderDto);
        }
        if (execution.getDestination() != null) {
            var destDto = new org.kasbench.globeco_trade_service.dto.DestinationResponseDTO();
            destDto.setId(execution.getDestination().getId());
            destDto.setAbbreviation(execution.getDestination().getAbbreviation());
            destDto.setDescription(execution.getDestination().getDescription());
            destDto.setVersion(execution.getDestination().getVersion());
            dto.setDestination(destDto);
        }
        dto.setQuantityOrdered(execution.getQuantityOrdered());
        dto.setQuantityPlaced(execution.getQuantityPlaced());
        dto.setQuantityFilled(execution.getQuantityFilled());
        dto.setLimitPrice(execution.getLimitPrice());
        dto.setExecutionServiceId(execution.getExecutionServiceId());
        dto.setVersion(execution.getVersion());
        return dto;
    }
} 