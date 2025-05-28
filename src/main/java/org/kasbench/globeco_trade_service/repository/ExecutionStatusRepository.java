package org.kasbench.globeco_trade_service.repository;

import org.kasbench.globeco_trade_service.entity.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExecutionStatusRepository extends JpaRepository<ExecutionStatus, Integer> {
    Optional<ExecutionStatus> findByAbbreviation(String abbreviation);
} 