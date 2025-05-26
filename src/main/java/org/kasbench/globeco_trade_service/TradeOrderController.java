package org.kasbench.globeco_trade_service;

import org.kasbench.globeco_trade_service.dto.TradeOrderPostDTO;
import org.kasbench.globeco_trade_service.dto.TradeOrderPutDTO;
import org.kasbench.globeco_trade_service.dto.TradeOrderResponseDTO;
import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.kasbench.globeco_trade_service.entity.Blotter;
import org.kasbench.globeco_trade_service.service.TradeOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tradeOrders")
public class TradeOrderController {
    private final TradeOrderService tradeOrderService;

    @Autowired
    public TradeOrderController(TradeOrderService tradeOrderService) {
        this.tradeOrderService = tradeOrderService;
    }

    @GetMapping
    public List<TradeOrderResponseDTO> getAllTradeOrders() {
        return tradeOrderService.getAllTradeOrders().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
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

    private TradeOrderResponseDTO toResponseDTO(TradeOrder tradeOrder) {
        TradeOrderResponseDTO dto = new TradeOrderResponseDTO();
        dto.setId(tradeOrder.getId());
        dto.setOrderId(tradeOrder.getOrderId());
        dto.setPortfolioId(tradeOrder.getPortfolioId());
        dto.setOrderType(tradeOrder.getOrderType());
        dto.setSecurityId(tradeOrder.getSecurityId());
        dto.setQuantity(tradeOrder.getQuantity());
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
        tradeOrder.setVersion(dto.getVersion());
        if (dto.getBlotterId() != null) {
            Blotter blotter = new Blotter();
            blotter.setId(dto.getBlotterId());
            tradeOrder.setBlotter(blotter);
        }
        return tradeOrder;
    }
} 