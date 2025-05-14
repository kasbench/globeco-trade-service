package org.kasbench.globeco_trade_service.repository;

import org.kasbench.globeco_trade_service.entity.TradeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeTypeRepository extends JpaRepository<TradeType, Integer> {
} 