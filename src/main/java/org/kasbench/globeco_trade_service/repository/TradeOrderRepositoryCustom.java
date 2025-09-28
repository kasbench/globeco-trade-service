package org.kasbench.globeco_trade_service.repository;

import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface TradeOrderRepositoryCustom {
    
    /**
     * Find all trade orders with specification and eager fetch blotter
     */
    Page<TradeOrder> findAllWithBlotterAndSpecification(Specification<TradeOrder> spec, Pageable pageable);
}