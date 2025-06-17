package org.kasbench.globeco_trade_service.repository;

import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TradeOrderRepository extends JpaRepository<TradeOrder, Integer>, JpaSpecificationExecutor<TradeOrder> {
    
    /**
     * Find trade order by ID with blotter eagerly fetched
     */
    @Query("SELECT t FROM TradeOrder t LEFT JOIN FETCH t.blotter WHERE t.id = :id")
    Optional<TradeOrder> findByIdWithBlotter(@Param("id") Integer id);
} 