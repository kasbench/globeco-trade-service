package org.kasbench.globeco_trade_service;

import org.kasbench.globeco_trade_service.dto.BlotterPostDTO;
import org.kasbench.globeco_trade_service.dto.BlotterPutDTO;
import org.kasbench.globeco_trade_service.dto.BlotterResponseDTO;
import org.kasbench.globeco_trade_service.entity.Blotter;
import org.kasbench.globeco_trade_service.service.BlotterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/blotters")
public class BlotterController {
    private final BlotterService blotterService;

    @Autowired
    public BlotterController(BlotterService blotterService) {
        this.blotterService = blotterService;
    }

    @GetMapping
    public List<BlotterResponseDTO> getAllBlotters() {
        return blotterService.getAllBlotters().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BlotterResponseDTO> getBlotterById(@PathVariable Integer id) {
        Optional<Blotter> blotter = blotterService.getBlotterById(id);
        return blotter.map(b -> ResponseEntity.ok(toResponseDTO(b)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<BlotterResponseDTO> createBlotter(@RequestBody BlotterPostDTO dto) {
        Blotter blotter = new Blotter();
        blotter.setAbbreviation(dto.getAbbreviation());
        blotter.setName(dto.getName());
        Blotter created = blotterService.createBlotter(blotter);
        return new ResponseEntity<>(toResponseDTO(created), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BlotterResponseDTO> updateBlotter(@PathVariable Integer id, @RequestBody BlotterPutDTO dto) {
        Blotter blotter = new Blotter();
        blotter.setId(dto.getId());
        blotter.setAbbreviation(dto.getAbbreviation());
        blotter.setName(dto.getName());
        blotter.setVersion(dto.getVersion());
        try {
            Blotter updated = blotterService.updateBlotter(id, blotter);
            return ResponseEntity.ok(toResponseDTO(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBlotter(@PathVariable Integer id, @RequestParam Integer version) {
        try {
            blotterService.deleteBlotter(id, version);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private BlotterResponseDTO toResponseDTO(Blotter blotter) {
        BlotterResponseDTO dto = new BlotterResponseDTO();
        dto.setId(blotter.getId());
        dto.setAbbreviation(blotter.getAbbreviation());
        dto.setName(blotter.getName());
        dto.setVersion(blotter.getVersion());
        return dto;
    }
} 