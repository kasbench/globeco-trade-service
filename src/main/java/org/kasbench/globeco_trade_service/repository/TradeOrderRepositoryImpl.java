package org.kasbench.globeco_trade_service.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class TradeOrderRepositoryImpl implements TradeOrderRepositoryCustom {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * Runs the fetch and count in a single short, read-only transaction so the JDBC
     * connection is acquired and released tightly around the queries. The returned
     * entities are detached from the persistence context before this method returns,
     * so downstream enrichment (external HTTP calls in the service layer) never holds
     * or re-acquires a database connection. The {@code blotter} association is eagerly
     * fetched below, so it remains safe to read after detachment.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<TradeOrder> findAllWithBlotterAndSpecification(Specification<TradeOrder> spec, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        
        // Query for data with eager fetch
        CriteriaQuery<TradeOrder> query = cb.createQuery(TradeOrder.class);
        Root<TradeOrder> root = query.from(TradeOrder.class);
        
        // Eager fetch blotter to avoid lazy loading issues
        root.fetch("blotter", JoinType.LEFT);
        
        // Apply specification if provided
        if (spec != null) {
            Predicate predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
        }
        
        // Apply sorting
        if (pageable.getSort().isSorted()) {
            List<Order> orders = pageable.getSort().stream()
                .map(order -> order.isAscending() 
                    ? cb.asc(root.get(order.getProperty()))
                    : cb.desc(root.get(order.getProperty())))
                .toList();
            query.orderBy(orders);
        }
        
        // Execute query with pagination
        TypedQuery<TradeOrder> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        
        List<TradeOrder> content = typedQuery.getResultList();
        
        // Count query for total elements
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<TradeOrder> countRoot = countQuery.from(TradeOrder.class);
        countQuery.select(cb.count(countRoot));
        
        if (spec != null) {
            Predicate countPredicate = spec.toPredicate(countRoot, countQuery, cb);
            if (countPredicate != null) {
                countQuery.where(countPredicate);
            }
        }
        
        Long total = entityManager.createQuery(countQuery).getSingleResult();
        
        // Detach entities so the transaction/connection can be fully released before the
        // service performs external HTTP enrichment. The blotter association is already
        // initialized via the eager fetch above, so it stays readable after detachment.
        for (TradeOrder tradeOrder : content) {
            entityManager.detach(tradeOrder);
        }
        
        return new PageImpl<>(content, pageable, total);
    }
}