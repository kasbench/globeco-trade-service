package org.kasbench.globeco_trade_service.repository;

import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeOrderRepository extends JpaRepository<TradeOrder, Integer> {
} 