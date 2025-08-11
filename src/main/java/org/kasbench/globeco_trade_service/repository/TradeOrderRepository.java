package org.kasbench.globeco_trade_service.repository;

import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeOrderRepository extends JpaRepository<TradeOrder, Integer>, JpaSpecificationExecutor<TradeOrder> {
    
    /**
     * Find trade order by ID with blotter eagerly fetched
     */
    @Query("SELECT t FROM TradeOrder t LEFT JOIN FETCH t.blotter WHERE t.id = :id")
    Optional<TradeOrder> findByIdWithBlotter(@Param("id") Integer id);
    
    /**
     * Find trade orders by order_id
     */
    List<TradeOrder> findByOrderId(Integer orderId);
    
    /**
     * Find trade orders by order_id with pagination
     */
    Page<TradeOrder> findByOrderId(Integer orderId, Pageable pageable);
} 