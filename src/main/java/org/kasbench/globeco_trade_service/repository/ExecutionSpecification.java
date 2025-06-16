package org.kasbench.globeco_trade_service.repository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.kasbench.globeco_trade_service.entity.Blotter;
import org.kasbench.globeco_trade_service.entity.Destination;
import org.kasbench.globeco_trade_service.entity.Execution;
import org.kasbench.globeco_trade_service.entity.ExecutionStatus;
import org.kasbench.globeco_trade_service.entity.TradeType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class ExecutionSpecification {
    
    /**
     * Filter by execution ID
     */
    public static Specification<Execution> hasId(Integer id) {
        return (root, query, criteriaBuilder) -> {
            if (id == null) return null;
            return criteriaBuilder.equal(root.get("id"), id);
        };
    }
    
    /**
     * Filter by execution status abbreviation (supports comma-separated values for OR condition)
     */
    public static Specification<Execution> hasExecutionStatusAbbreviation(String abbreviations) {
        return (root, query, criteriaBuilder) -> {
            if (abbreviations == null || abbreviations.trim().isEmpty()) return null;
            
            Join<Execution, ExecutionStatus> statusJoin = root.join("executionStatus", JoinType.INNER);
            
            String[] abbrevs = abbreviations.split(",");
            if (abbrevs.length == 1) {
                return criteriaBuilder.equal(statusJoin.get("abbreviation"), abbrevs[0].trim());
            } else {
                List<String> abbrevList = Arrays.stream(abbrevs)
                    .map(String::trim)
                    .toList();
                return statusJoin.get("abbreviation").in(abbrevList);
            }
        };
    }
    
    /**
     * Filter by blotter abbreviation (supports comma-separated values for OR condition)
     */
    public static Specification<Execution> hasBlotterAbbreviation(String abbreviations) {
        return (root, query, criteriaBuilder) -> {
            if (abbreviations == null || abbreviations.trim().isEmpty()) return null;
            
            Join<Execution, Blotter> blotterJoin = root.join("blotter", JoinType.INNER);
            
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
     * Filter by trade type abbreviation (supports comma-separated values for OR condition)
     */
    public static Specification<Execution> hasTradeTypeAbbreviation(String abbreviations) {
        return (root, query, criteriaBuilder) -> {
            if (abbreviations == null || abbreviations.trim().isEmpty()) return null;
            
            Join<Execution, TradeType> tradeTypeJoin = root.join("tradeType", JoinType.INNER);
            
            String[] abbrevs = abbreviations.split(",");
            if (abbrevs.length == 1) {
                return criteriaBuilder.equal(tradeTypeJoin.get("abbreviation"), abbrevs[0].trim());
            } else {
                List<String> abbrevList = Arrays.stream(abbrevs)
                    .map(String::trim)
                    .toList();
                return tradeTypeJoin.get("abbreviation").in(abbrevList);
            }
        };
    }
    
    /**
     * Filter by trade order ID
     */
    public static Specification<Execution> hasTradeOrderId(Integer tradeOrderId) {
        return (root, query, criteriaBuilder) -> {
            if (tradeOrderId == null) return null;
            return criteriaBuilder.equal(root.get("tradeOrderId"), tradeOrderId);
        };
    }
    
    /**
     * Filter by destination abbreviation (supports comma-separated values for OR condition)
     */
    public static Specification<Execution> hasDestinationAbbreviation(String abbreviations) {
        return (root, query, criteriaBuilder) -> {
            if (abbreviations == null || abbreviations.trim().isEmpty()) return null;
            
            Join<Execution, Destination> destinationJoin = root.join("destination", JoinType.INNER);
            
            String[] abbrevs = abbreviations.split(",");
            if (abbrevs.length == 1) {
                return criteriaBuilder.equal(destinationJoin.get("abbreviation"), abbrevs[0].trim());
            } else {
                List<String> abbrevList = Arrays.stream(abbrevs)
                    .map(String::trim)
                    .toList();
                return destinationJoin.get("abbreviation").in(abbrevList);
            }
        };
    }
    
    /**
     * Filter by minimum quantity ordered
     */
    public static Specification<Execution> hasQuantityOrderedGreaterThanOrEqual(BigDecimal minQuantityOrdered) {
        return (root, query, criteriaBuilder) -> {
            if (minQuantityOrdered == null) return null;
            return criteriaBuilder.greaterThanOrEqualTo(root.get("quantityOrdered"), minQuantityOrdered);
        };
    }
    
    /**
     * Filter by maximum quantity ordered
     */
    public static Specification<Execution> hasQuantityOrderedLessThanOrEqual(BigDecimal maxQuantityOrdered) {
        return (root, query, criteriaBuilder) -> {
            if (maxQuantityOrdered == null) return null;
            return criteriaBuilder.lessThanOrEqualTo(root.get("quantityOrdered"), maxQuantityOrdered);
        };
    }
    
    /**
     * Filter by minimum quantity placed
     */
    public static Specification<Execution> hasQuantityPlacedGreaterThanOrEqual(BigDecimal minQuantityPlaced) {
        return (root, query, criteriaBuilder) -> {
            if (minQuantityPlaced == null) return null;
            return criteriaBuilder.greaterThanOrEqualTo(root.get("quantityPlaced"), minQuantityPlaced);
        };
    }
    
    /**
     * Filter by maximum quantity placed
     */
    public static Specification<Execution> hasQuantityPlacedLessThanOrEqual(BigDecimal maxQuantityPlaced) {
        return (root, query, criteriaBuilder) -> {
            if (maxQuantityPlaced == null) return null;
            return criteriaBuilder.lessThanOrEqualTo(root.get("quantityPlaced"), maxQuantityPlaced);
        };
    }
    
    /**
     * Filter by minimum quantity filled
     */
    public static Specification<Execution> hasQuantityFilledGreaterThanOrEqual(BigDecimal minQuantityFilled) {
        return (root, query, criteriaBuilder) -> {
            if (minQuantityFilled == null) return null;
            return criteriaBuilder.greaterThanOrEqualTo(root.get("quantityFilled"), minQuantityFilled);
        };
    }
    
    /**
     * Filter by maximum quantity filled
     */
    public static Specification<Execution> hasQuantityFilledLessThanOrEqual(BigDecimal maxQuantityFilled) {
        return (root, query, criteriaBuilder) -> {
            if (maxQuantityFilled == null) return null;
            return criteriaBuilder.lessThanOrEqualTo(root.get("quantityFilled"), maxQuantityFilled);
        };
    }
    
    /**
     * Combine multiple specifications with AND logic
     */
    public static Specification<Execution> buildSpecification(
            Integer id,
            String executionStatusAbbreviation,
            String blotterAbbreviation,
            String tradeTypeAbbreviation,
            Integer tradeOrderId,
            String destinationAbbreviation,
            BigDecimal quantityOrderedMin,
            BigDecimal quantityOrderedMax,
            BigDecimal quantityPlacedMin,
            BigDecimal quantityPlacedMax,
            BigDecimal quantityFilledMin,
            BigDecimal quantityFilledMax) {
        
        return Specification.where(hasId(id))
                .and(hasExecutionStatusAbbreviation(executionStatusAbbreviation))
                .and(hasBlotterAbbreviation(blotterAbbreviation))
                .and(hasTradeTypeAbbreviation(tradeTypeAbbreviation))
                .and(hasTradeOrderId(tradeOrderId))
                .and(hasDestinationAbbreviation(destinationAbbreviation))
                .and(hasQuantityOrderedGreaterThanOrEqual(quantityOrderedMin))
                .and(hasQuantityOrderedLessThanOrEqual(quantityOrderedMax))
                .and(hasQuantityPlacedGreaterThanOrEqual(quantityPlacedMin))
                .and(hasQuantityPlacedLessThanOrEqual(quantityPlacedMax))
                .and(hasQuantityFilledGreaterThanOrEqual(quantityFilledMin))
                .and(hasQuantityFilledLessThanOrEqual(quantityFilledMax));
    }
} 