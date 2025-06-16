package org.kasbench.globeco_trade_service.repository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.kasbench.globeco_trade_service.entity.Blotter;
import org.kasbench.globeco_trade_service.entity.TradeOrder;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TradeOrderSpecification {
    
    /**
     * Filter by trade order ID
     */
    public static Specification<TradeOrder> hasId(Integer id) {
        return (root, query, criteriaBuilder) -> {
            if (id == null) return null;
            return criteriaBuilder.equal(root.get("id"), id);
        };
    }
    
    /**
     * Filter by order ID
     */
    public static Specification<TradeOrder> hasOrderId(Integer orderId) {
        return (root, query, criteriaBuilder) -> {
            if (orderId == null) return null;
            return criteriaBuilder.equal(root.get("orderId"), orderId);
        };
    }
    
    /**
     * Filter by order type (supports comma-separated values for OR condition)
     */
    public static Specification<TradeOrder> hasOrderType(String orderTypes) {
        return (root, query, criteriaBuilder) -> {
            if (orderTypes == null || orderTypes.trim().isEmpty()) return null;
            
            String[] types = orderTypes.split(",");
            if (types.length == 1) {
                return criteriaBuilder.equal(root.get("orderType"), types[0].trim());
            } else {
                List<String> typeList = Arrays.stream(types)
                    .map(String::trim)
                    .toList();
                return root.get("orderType").in(typeList);
            }
        };
    }
    
    /**
     * Filter by portfolio ID (supports comma-separated values for OR condition)
     */
    public static Specification<TradeOrder> hasPortfolioId(String portfolioIds) {
        return (root, query, criteriaBuilder) -> {
            if (portfolioIds == null || portfolioIds.trim().isEmpty()) return null;
            
            String[] ids = portfolioIds.split(",");
            if (ids.length == 1) {
                return criteriaBuilder.equal(root.get("portfolioId"), ids[0].trim());
            } else {
                List<String> idList = Arrays.stream(ids)
                    .map(String::trim)
                    .toList();
                return root.get("portfolioId").in(idList);
            }
        };
    }
    
    /**
     * Filter by security ID (supports comma-separated values for OR condition)
     */
    public static Specification<TradeOrder> hasSecurityId(String securityIds) {
        return (root, query, criteriaBuilder) -> {
            if (securityIds == null || securityIds.trim().isEmpty()) return null;
            
            String[] ids = securityIds.split(",");
            if (ids.length == 1) {
                return criteriaBuilder.equal(root.get("securityId"), ids[0].trim());
            } else {
                List<String> idList = Arrays.stream(ids)
                    .map(String::trim)
                    .toList();
                return root.get("securityId").in(idList);
            }
        };
    }
    
    /**
     * Filter by minimum quantity
     */
    public static Specification<TradeOrder> hasQuantityGreaterThanOrEqual(BigDecimal minQuantity) {
        return (root, query, criteriaBuilder) -> {
            if (minQuantity == null) return null;
            return criteriaBuilder.greaterThanOrEqualTo(root.get("quantity"), minQuantity);
        };
    }
    
    /**
     * Filter by maximum quantity
     */
    public static Specification<TradeOrder> hasQuantityLessThanOrEqual(BigDecimal maxQuantity) {
        return (root, query, criteriaBuilder) -> {
            if (maxQuantity == null) return null;
            return criteriaBuilder.lessThanOrEqualTo(root.get("quantity"), maxQuantity);
        };
    }
    
    /**
     * Filter by minimum quantity sent
     */
    public static Specification<TradeOrder> hasQuantitySentGreaterThanOrEqual(BigDecimal minQuantitySent) {
        return (root, query, criteriaBuilder) -> {
            if (minQuantitySent == null) return null;
            return criteriaBuilder.greaterThanOrEqualTo(root.get("quantitySent"), minQuantitySent);
        };
    }
    
    /**
     * Filter by maximum quantity sent
     */
    public static Specification<TradeOrder> hasQuantitySentLessThanOrEqual(BigDecimal maxQuantitySent) {
        return (root, query, criteriaBuilder) -> {
            if (maxQuantitySent == null) return null;
            return criteriaBuilder.lessThanOrEqualTo(root.get("quantitySent"), maxQuantitySent);
        };
    }
    
    /**
     * Filter by blotter abbreviation (supports comma-separated values for OR condition)
     */
    public static Specification<TradeOrder> hasBlotterAbbreviation(String abbreviations) {
        return (root, query, criteriaBuilder) -> {
            if (abbreviations == null || abbreviations.trim().isEmpty()) return null;
            
            Join<TradeOrder, Blotter> blotterJoin = root.join("blotter", JoinType.INNER);
            
            String[] abbrevs = abbreviations.split(",");
            if (abbrevs.length == 1) {
                return criteriaBuilder.equal(blotterJoin.get("abbreviation"), abbrevs[0].trim());
            } else {
                List<String> abbrevList = Arrays.stream(abbrevs)
                    .map(String::trim)
                    .toList();
                return blotterJoin.get("abbreviation").in(abbrevList);
            }
        };
    }
    
    /**
     * Filter by submitted status
     */
    public static Specification<TradeOrder> hasSubmitted(Boolean submitted) {
        return (root, query, criteriaBuilder) -> {
            if (submitted == null) return null;
            return criteriaBuilder.equal(root.get("submitted"), submitted);
        };
    }
    
    /**
     * Combine multiple specifications with AND logic
     */
    public static Specification<TradeOrder> buildSpecification(
            Integer id,
            Integer orderId,
            String orderType,
            String portfolioId,
            String securityId,
            BigDecimal quantityMin,
            BigDecimal quantityMax,
            BigDecimal quantitySentMin,
            BigDecimal quantitySentMax,
            String blotterAbbreviation,
            Boolean submitted) {
        
        return Specification.where(hasId(id))
                .and(hasOrderId(orderId))
                .and(hasOrderType(orderType))
                .and(hasPortfolioId(portfolioId))
                .and(hasSecurityId(securityId))
                .and(hasQuantityGreaterThanOrEqual(quantityMin))
                .and(hasQuantityLessThanOrEqual(quantityMax))
                .and(hasQuantitySentGreaterThanOrEqual(quantitySentMin))
                .and(hasQuantitySentLessThanOrEqual(quantitySentMax))
                .and(hasBlotterAbbreviation(blotterAbbreviation))
                .and(hasSubmitted(submitted));
    }
} 