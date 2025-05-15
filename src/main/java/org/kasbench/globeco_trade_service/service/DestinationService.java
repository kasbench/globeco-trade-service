package org.kasbench.globeco_trade_service.service;

import org.kasbench.globeco_trade_service.entity.Destination;
import java.util.List;
import java.util.Optional;

public interface DestinationService {
    List<Destination> getAllDestinations();
    Optional<Destination> getDestinationById(Integer id);
    Destination createDestination(Destination destination);
    Destination updateDestination(Integer id, Destination destination);
    void deleteDestination(Integer id, Integer version);
} 