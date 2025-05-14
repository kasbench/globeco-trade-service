package org.kasbench.globeco_trade_service.repository;

import org.kasbench.globeco_trade_service.entity.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DestinationRepository extends JpaRepository<Destination, Integer> {
} 