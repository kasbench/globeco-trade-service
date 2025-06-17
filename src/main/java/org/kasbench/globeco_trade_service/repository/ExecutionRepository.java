package org.kasbench.globeco_trade_service.repository;

import org.kasbench.globeco_trade_service.entity.Execution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExecutionRepository extends JpaRepository<Execution, Integer>, JpaSpecificationExecutor<Execution> {
    
    /**
     * Find execution by ID with all related entities eagerly fetched to avoid lazy loading issues
     */
    @Query("SELECT e FROM Execution e " +
           "LEFT JOIN FETCH e.executionStatus " +
           "LEFT JOIN FETCH e.blotter " +
           "LEFT JOIN FETCH e.tradeType " +
           "LEFT JOIN FETCH e.tradeOrder " +
           "LEFT JOIN FETCH e.destination " +
           "WHERE e.id = :id")
    Optional<Execution> findByIdWithAllRelations(@Param("id") Integer id);
} 