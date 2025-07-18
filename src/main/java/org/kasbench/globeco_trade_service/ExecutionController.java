package org.kasbench.globeco_trade_service;

import org.kasbench.globeco_trade_service.dto.*;
import org.kasbench.globeco_trade_service.entity.*;
import org.kasbench.globeco_trade_service.service.ExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/executions")
public class ExecutionController {
    private final ExecutionService executionService;

    @Autowired
    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @GetMapping
    public ResponseEntity<List<ExecutionResponseDTO>> getAllExecutions(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        
        // Validate pagination parameters
        if (limit != null && (limit < 1 || limit > 1000)) {
            throw new IllegalArgumentException("Limit must be between 1 and 1000");
        }
        if (offset != null && offset < 0) {
            throw new IllegalArgumentException("Offset must be non-negative");
        }
        
        if (limit == null && offset == null) {
            // Backward compatible: no pagination
            List<ExecutionResponseDTO> result = executionService.getAllExecutions().stream()
                    .map(this::toResponseDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } else {
            // Use paginated method
            ExecutionService.PaginatedResult<Execution> paginatedResult = 
                executionService.getAllExecutions(limit, offset);
            
            List<ExecutionResponseDTO> result = paginatedResult.getData().stream()
                    .map(this::toResponseDTO)
                    .collect(Collectors.toList());
            
            // Add X-Total-Count header for pagination metadata
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(paginatedResult.getTotalCount()))
                    .body(result);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExecutionResponseDTO> getExecutionById(@PathVariable Integer id) {
        Optional<Execution> execution = executionService.getExecutionById(id);
        return execution.map(e -> ResponseEntity.ok(toResponseDTO(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ExecutionResponseDTO> createExecution(@RequestBody ExecutionPostDTO dto) {
        Execution execution = fromPostDTO(dto);
        Execution created = executionService.createExecution(execution);
        return new ResponseEntity<>(toResponseDTO(created), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExecutionResponseDTO> updateExecution(@PathVariable Integer id, @RequestBody ExecutionPutDTO dto) {
        Execution execution = fromPutDTO(dto);
        Execution updated = executionService.updateExecution(id, execution);
        return ResponseEntity.ok(toResponseDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExecution(@PathVariable Integer id, @RequestParam Integer version) {
        executionService.deleteExecution(id, version);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/fill")
    public ResponseEntity<?> fillExecution(@PathVariable Integer id, @RequestBody ExecutionPutFillDTO fillDTO) {
        try {
            Execution updated = executionService.fillExecution(id, fillDTO);
            return ResponseEntity.ok(toResponseDTO(updated));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("Version mismatch")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(java.util.Map.of("error", e.getMessage()));
            } else {
                return ResponseEntity.badRequest()
                        .body(java.util.Map.of("error", e.getMessage()));
            }
        }
    }

    public ExecutionResponseDTO toResponseDTO(Execution execution) {
        ExecutionResponseDTO dto = new ExecutionResponseDTO();
        dto.setId(execution.getId());
        dto.setExecutionTimestamp(execution.getExecutionTimestamp());
        dto.setQuantityOrdered(execution.getQuantityOrdered());
        dto.setQuantityPlaced(execution.getQuantityPlaced());
        dto.setQuantityFilled(execution.getQuantityFilled());
        dto.setLimitPrice(execution.getLimitPrice());
        dto.setVersion(execution.getVersion());
        dto.setExecutionServiceId(execution.getExecutionServiceId());
        // Nested DTOs
        if (execution.getExecutionStatus() != null) {
            ExecutionStatusResponseDTO statusDTO = new ExecutionStatusResponseDTO();
            statusDTO.setId(execution.getExecutionStatus().getId());
            statusDTO.setAbbreviation(execution.getExecutionStatus().getAbbreviation());
            statusDTO.setDescription(execution.getExecutionStatus().getDescription());
            statusDTO.setVersion(execution.getExecutionStatus().getVersion());
            dto.setExecutionStatus(statusDTO);
        }
        if (execution.getBlotter() != null) {
            BlotterResponseDTO blotterDTO = new BlotterResponseDTO();
            blotterDTO.setId(execution.getBlotter().getId());
            blotterDTO.setAbbreviation(execution.getBlotter().getAbbreviation());
            blotterDTO.setName(execution.getBlotter().getName());
            blotterDTO.setVersion(execution.getBlotter().getVersion());
            dto.setBlotter(blotterDTO);
        }
        if (execution.getTradeType() != null) {
            TradeTypeResponseDTO tradeTypeDTO = new TradeTypeResponseDTO();
            tradeTypeDTO.setId(execution.getTradeType().getId());
            tradeTypeDTO.setAbbreviation(execution.getTradeType().getAbbreviation());
            tradeTypeDTO.setDescription(execution.getTradeType().getDescription());
            tradeTypeDTO.setVersion(execution.getTradeType().getVersion());
            dto.setTradeType(tradeTypeDTO);
        }
        if (execution.getTradeOrder() != null) {
            TradeOrderResponseDTO tradeOrderDTO = new TradeOrderResponseDTO();
            tradeOrderDTO.setId(execution.getTradeOrder().getId());
            tradeOrderDTO.setOrderId(execution.getTradeOrder().getOrderId());
            tradeOrderDTO.setPortfolioId(execution.getTradeOrder().getPortfolioId());
            tradeOrderDTO.setOrderType(execution.getTradeOrder().getOrderType());
            tradeOrderDTO.setSecurityId(execution.getTradeOrder().getSecurityId());
            tradeOrderDTO.setQuantity(execution.getTradeOrder().getQuantity());
            tradeOrderDTO.setLimitPrice(execution.getTradeOrder().getLimitPrice());
            tradeOrderDTO.setTradeTimestamp(execution.getTradeOrder().getTradeTimestamp());
            tradeOrderDTO.setVersion(execution.getTradeOrder().getVersion());
            if (execution.getTradeOrder().getBlotter() != null) {
                BlotterResponseDTO tradeOrderBlotterDTO = new BlotterResponseDTO();
                tradeOrderBlotterDTO.setId(execution.getTradeOrder().getBlotter().getId());
                tradeOrderBlotterDTO.setAbbreviation(execution.getTradeOrder().getBlotter().getAbbreviation());
                tradeOrderBlotterDTO.setName(execution.getTradeOrder().getBlotter().getName());
                tradeOrderBlotterDTO.setVersion(execution.getTradeOrder().getBlotter().getVersion());
                tradeOrderDTO.setBlotter(tradeOrderBlotterDTO);
            }
            dto.setTradeOrder(tradeOrderDTO);
        }
        if (execution.getDestination() != null) {
            DestinationResponseDTO destDTO = new DestinationResponseDTO();
            destDTO.setId(execution.getDestination().getId());
            destDTO.setAbbreviation(execution.getDestination().getAbbreviation());
            destDTO.setDescription(execution.getDestination().getDescription());
            destDTO.setVersion(execution.getDestination().getVersion());
            dto.setDestination(destDTO);
        }
        return dto;
    }

    private Execution fromPostDTO(ExecutionPostDTO dto) {
        Execution execution = new Execution();
        execution.setExecutionTimestamp(dto.getExecutionTimestamp());
        execution.setQuantityOrdered(dto.getQuantityOrdered());
        execution.setQuantityPlaced(dto.getQuantityPlaced());
        execution.setQuantityFilled(dto.getQuantityFilled());
        execution.setLimitPrice(dto.getLimitPrice());
        execution.setExecutionServiceId(dto.getExecutionServiceId());
        // Set relationships by id
        ExecutionStatus status = new ExecutionStatus();
        status.setId(dto.getExecutionStatusId());
        execution.setExecutionStatus(status);
        if (dto.getBlotterId() != null) {
            Blotter blotter = new Blotter();
            blotter.setId(dto.getBlotterId());
            execution.setBlotter(blotter);
        }
        if (dto.getTradeTypeId() != null) {
            TradeType tradeType = new TradeType();
            tradeType.setId(dto.getTradeTypeId());
            execution.setTradeType(tradeType);
        }
        TradeOrder tradeOrder = new TradeOrder();
        tradeOrder.setId(dto.getTradeOrderId());
        execution.setTradeOrder(tradeOrder);
        Destination destination = new Destination();
        destination.setId(dto.getDestinationId());
        execution.setDestination(destination);
        return execution;
    }

    private Execution fromPostDTO(ExecutionPutDTO dto) {
        Execution execution = new Execution();
        execution.setExecutionTimestamp(dto.getExecutionTimestamp());
        execution.setQuantityOrdered(dto.getQuantityOrdered());
        execution.setQuantityPlaced(dto.getQuantityPlaced());
        execution.setQuantityFilled(dto.getQuantityFilled());
        execution.setLimitPrice(dto.getLimitPrice());
        execution.setExecutionServiceId(dto.getExecutionServiceId());
        // Set relationships by id
        ExecutionStatus status = new ExecutionStatus();
        status.setId(dto.getExecutionStatusId());
        execution.setExecutionStatus(status);
        if (dto.getBlotterId() != null) {
            Blotter blotter = new Blotter();
            blotter.setId(dto.getBlotterId());
            execution.setBlotter(blotter);
        }
        if (dto.getTradeTypeId() != null) {
            TradeType tradeType = new TradeType();
            tradeType.setId(dto.getTradeTypeId());
            execution.setTradeType(tradeType);
        }
        TradeOrder tradeOrder = new TradeOrder();
        tradeOrder.setId(dto.getTradeOrderId());
        execution.setTradeOrder(tradeOrder);
        Destination destination = new Destination();
        destination.setId(dto.getDestinationId());
        execution.setDestination(destination);
        return execution;
    }

    private Execution fromPutDTO(ExecutionPutDTO dto) {
        Execution execution = fromPostDTO(dto);
        execution.setId(dto.getId());
        execution.setVersion(dto.getVersion());
        return execution;
    }
} 