package org.kasbench.globeco_trade_service;

import org.kasbench.globeco_trade_service.dto.DestinationPostDTO;
import org.kasbench.globeco_trade_service.dto.DestinationPutDTO;
import org.kasbench.globeco_trade_service.dto.DestinationResponseDTO;
import org.kasbench.globeco_trade_service.entity.Destination;
import org.kasbench.globeco_trade_service.service.DestinationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/destinations")
public class DestinationController {
    private final DestinationService destinationService;

    @Autowired
    public DestinationController(DestinationService destinationService) {
        this.destinationService = destinationService;
    }

    @GetMapping
    public List<DestinationResponseDTO> getAllDestinations() {
        return destinationService.getAllDestinations().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DestinationResponseDTO> getDestinationById(@PathVariable Integer id) {
        Optional<Destination> destination = destinationService.getDestinationById(id);
        return destination.map(d -> ResponseEntity.ok(toResponseDTO(d)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<DestinationResponseDTO> createDestination(@RequestBody DestinationPostDTO dto) {
        Destination destination = fromPostDTO(dto);
        Destination created = destinationService.createDestination(destination);
        return new ResponseEntity<>(toResponseDTO(created), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DestinationResponseDTO> updateDestination(@PathVariable Integer id, @RequestBody DestinationPutDTO dto) {
        Destination destination = fromPutDTO(dto);
        try {
            Destination updated = destinationService.updateDestination(id, destination);
            return ResponseEntity.ok(toResponseDTO(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDestination(@PathVariable Integer id, @RequestParam Integer version) {
        try {
            destinationService.deleteDestination(id, version);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private DestinationResponseDTO toResponseDTO(Destination destination) {
        DestinationResponseDTO dto = new DestinationResponseDTO();
        dto.setId(destination.getId());
        dto.setAbbreviation(destination.getAbbreviation());
        dto.setDescription(destination.getDescription());
        dto.setVersion(destination.getVersion());
        return dto;
    }

    private Destination fromPostDTO(DestinationPostDTO dto) {
        Destination destination = new Destination();
        destination.setAbbreviation(dto.getAbbreviation());
        destination.setDescription(dto.getDescription());
        return destination;
    }

    private Destination fromPutDTO(DestinationPutDTO dto) {
        Destination destination = new Destination();
        destination.setId(dto.getId());
        destination.setAbbreviation(dto.getAbbreviation());
        destination.setDescription(dto.getDescription());
        destination.setVersion(dto.getVersion());
        return destination;
    }
} 