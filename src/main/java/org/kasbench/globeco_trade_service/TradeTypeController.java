package org.kasbench.globeco_trade_service;

import org.kasbench.globeco_trade_service.dto.TradeTypePostDTO;
import org.kasbench.globeco_trade_service.dto.TradeTypePutDTO;
import org.kasbench.globeco_trade_service.dto.TradeTypeResponseDTO;
import org.kasbench.globeco_trade_service.entity.TradeType;
import org.kasbench.globeco_trade_service.service.TradeTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class TradeTypeController {
    private final TradeTypeService tradeTypeService;

    @Autowired
    public TradeTypeController(TradeTypeService tradeTypeService) {
        this.tradeTypeService = tradeTypeService;
    }

    @GetMapping("/tradeTypes")
    public List<TradeTypeResponseDTO> getAllTradeTypes() {
        return tradeTypeService.getAllTradeTypes().stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @GetMapping("/tradeType/{id}")
    public ResponseEntity<TradeTypeResponseDTO> getTradeTypeById(@PathVariable Integer id) {
        Optional<TradeType> tradeType = tradeTypeService.getTradeTypeById(id);
        return tradeType.map(type -> ResponseEntity.ok(toResponseDTO(type)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/tradeTypes")
    public ResponseEntity<TradeTypeResponseDTO> createTradeType(@RequestBody TradeTypePostDTO dto) {
        TradeType tradeType = new TradeType();
        tradeType.setAbbreviation(dto.getAbbreviation());
        tradeType.setDescription(dto.getDescription());
        TradeType created = tradeTypeService.createTradeType(tradeType);
        return new ResponseEntity<>(toResponseDTO(created), HttpStatus.CREATED);
    }

    @PutMapping("/tradeType/{id}")
    public ResponseEntity<TradeTypeResponseDTO> updateTradeType(@PathVariable Integer id, @RequestBody TradeTypePutDTO dto) {
        TradeType tradeType = new TradeType();
        tradeType.setId(dto.getId());
        tradeType.setAbbreviation(dto.getAbbreviation());
        tradeType.setDescription(dto.getDescription());
        tradeType.setVersion(dto.getVersion());
        try {
            TradeType updated = tradeTypeService.updateTradeType(id, tradeType);
            return ResponseEntity.ok(toResponseDTO(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/tradeType/{id}")
    public ResponseEntity<Void> deleteTradeType(@PathVariable Integer id, @RequestParam Integer version) {
        try {
            tradeTypeService.deleteTradeType(id, version);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private TradeTypeResponseDTO toResponseDTO(TradeType tradeType) {
        TradeTypeResponseDTO dto = new TradeTypeResponseDTO();
        dto.setId(tradeType.getId());
        dto.setAbbreviation(tradeType.getAbbreviation());
        dto.setDescription(tradeType.getDescription());
        dto.setVersion(tradeType.getVersion());
        return dto;
    }
} 