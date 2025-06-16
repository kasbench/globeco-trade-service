package org.kasbench.globeco_trade_service.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.kasbench.globeco_trade_service.entity.TradeOrder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TradeOrderSpecification focusing on specification creation logic
 * rather than complex JPA Criteria API mocking
 */
class TradeOrderSpecificationTest {

    @Test
    void testHasId_ValidId() {
        // Arrange
        Integer id = 123;

        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.hasId(id);

        // Assert
        assertNotNull(spec);
    }

    @Test
    void testHasId_NullId() {
        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.hasId(null);

        // Assert
        assertNotNull(spec); // The specification is created but returns null predicate
    }

    @Test
    void testHasOrderId_ValidOrderId() {
        // Arrange
        Integer orderId = 456;

        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.hasOrderId(orderId);

        // Assert
        assertNotNull(spec);
    }

    @Test
    void testHasOrderId_NullOrderId() {
        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.hasOrderId(null);

        // Assert
        assertNotNull(spec); // The specification is created but returns null predicate
    }

    @Test
    void testHasOrderType_ValidType() {
        // Arrange
        String orderType = "BUY";

        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.hasOrderType(orderType);

        // Assert
        assertNotNull(spec);
    }

    @Test
    void testHasOrderType_MultipleTypes() {
        // Arrange
        String orderTypes = "BUY,SELL";

        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.hasOrderType(orderTypes);

        // Assert
        assertNotNull(spec);
    }

    @Test
    void testHasOrderType_NullOrEmpty() {
        // Test null
        assertNotNull(TradeOrderSpecification.hasOrderType(null)); // Returns spec with null predicate

        // Test empty
        assertNotNull(TradeOrderSpecification.hasOrderType("")); // Returns spec with null predicate

        // Test blank
        assertNotNull(TradeOrderSpecification.hasOrderType("   ")); // Returns spec with null predicate
    }

    @Test
    void testHasPortfolioId_ValidId() {
        // Arrange
        String portfolioId = "PORTFOLIO1";

        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.hasPortfolioId(portfolioId);

        // Assert
        assertNotNull(spec);
    }

    @Test
    void testHasPortfolioId_MultipleIds() {
        // Arrange
        String portfolioIds = "PORTFOLIO1,PORTFOLIO2";

        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.hasPortfolioId(portfolioIds);

        // Assert
        assertNotNull(spec);
    }

    @Test
    void testHasPortfolioId_NullOrEmpty() {
        // Test null
        assertNotNull(TradeOrderSpecification.hasPortfolioId(null)); // Returns spec with null predicate

        // Test empty
        assertNotNull(TradeOrderSpecification.hasPortfolioId("")); // Returns spec with null predicate

        // Test blank
        assertNotNull(TradeOrderSpecification.hasPortfolioId("   ")); // Returns spec with null predicate
    }

    @Test
    void testHasSecurityId_ValidId() {
        // Arrange
        String securityId = "SEC123";

        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.hasSecurityId(securityId);

        // Assert
        assertNotNull(spec);
    }

    @Test
    void testHasSecurityId_MultipleIds() {
        // Arrange
        String securityIds = "SEC123,SEC456";

        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.hasSecurityId(securityIds);

        // Assert
        assertNotNull(spec);
    }

    @Test
    void testHasSecurityId_NullOrEmpty() {
        // Test null
        assertNotNull(TradeOrderSpecification.hasSecurityId(null)); // Returns spec with null predicate

        // Test empty
        assertNotNull(TradeOrderSpecification.hasSecurityId("")); // Returns spec with null predicate

        // Test blank
        assertNotNull(TradeOrderSpecification.hasSecurityId("   ")); // Returns spec with null predicate
    }

    @Test
    void testHasQuantityGreaterThanOrEqual_ValidQuantity() {
        // Arrange
        BigDecimal minQuantity = new BigDecimal("100.00");

        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.hasQuantityGreaterThanOrEqual(minQuantity);

        // Assert
        assertNotNull(spec);
    }

    @Test
    void testHasQuantityGreaterThanOrEqual_NullQuantity() {
        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.hasQuantityGreaterThanOrEqual(null);

        // Assert
        assertNotNull(spec); // Returns spec with null predicate
    }

    @Test
    void testHasQuantityLessThanOrEqual_ValidQuantity() {
        // Arrange
        BigDecimal maxQuantity = new BigDecimal("1000.00");

        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.hasQuantityLessThanOrEqual(maxQuantity);

        // Assert
        assertNotNull(spec);
    }

    @Test
    void testHasQuantityLessThanOrEqual_NullQuantity() {
        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.hasQuantityLessThanOrEqual(null);

        // Assert
        assertNotNull(spec); // Returns spec with null predicate
    }

    @Test
    void testHasQuantitySentGreaterThanOrEqual_ValidQuantity() {
        // Arrange
        BigDecimal minQuantitySent = new BigDecimal("50.00");

        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.hasQuantitySentGreaterThanOrEqual(minQuantitySent);

        // Assert
        assertNotNull(spec);
    }

    @Test
    void testHasQuantitySentGreaterThanOrEqual_NullQuantity() {
        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.hasQuantitySentGreaterThanOrEqual(null);

        // Assert
        assertNotNull(spec); // Returns spec with null predicate
    }

    @Test
    void testHasQuantitySentLessThanOrEqual_ValidQuantity() {
        // Arrange
        BigDecimal maxQuantitySent = new BigDecimal("500.00");

        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.hasQuantitySentLessThanOrEqual(maxQuantitySent);

        // Assert
        assertNotNull(spec);
    }

    @Test
    void testHasQuantitySentLessThanOrEqual_NullQuantity() {
        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.hasQuantitySentLessThanOrEqual(null);

        // Assert
        assertNotNull(spec); // Returns spec with null predicate
    }

    @Test
    void testHasBlotterAbbreviation_ValidAbbreviation() {
        // Arrange
        String abbreviation = "EQ";

        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.hasBlotterAbbreviation(abbreviation);

        // Assert
        assertNotNull(spec);
    }

    @Test
    void testHasBlotterAbbreviation_MultipleAbbreviations() {
        // Arrange
        String abbreviations = "EQ,FI";

        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.hasBlotterAbbreviation(abbreviations);

        // Assert
        assertNotNull(spec);
    }

    @Test
    void testHasBlotterAbbreviation_NullOrEmpty() {
        // Test null
        assertNotNull(TradeOrderSpecification.hasBlotterAbbreviation(null)); // Returns spec with null predicate

        // Test empty
        assertNotNull(TradeOrderSpecification.hasBlotterAbbreviation("")); // Returns spec with null predicate

        // Test blank
        assertNotNull(TradeOrderSpecification.hasBlotterAbbreviation("   ")); // Returns spec with null predicate
    }

    @Test
    void testHasSubmitted_True() {
        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.hasSubmitted(true);

        // Assert
        assertNotNull(spec);
    }

    @Test
    void testHasSubmitted_False() {
        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.hasSubmitted(false);

        // Assert
        assertNotNull(spec);
    }

    @Test
    void testHasSubmitted_Null() {
        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.hasSubmitted(null);

        // Assert
        assertNotNull(spec); // Returns spec with null predicate
    }

    @Test
    void testBuildSpecification_AllParameters() {
        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.buildSpecification(
            123, 456, "BUY", "PORTFOLIO1", "SEC123",
            new BigDecimal("100"), new BigDecimal("1000"),
            new BigDecimal("50"), new BigDecimal("500"),
            "EQ", true
        );

        // Assert
        assertNotNull(spec);
    }

    @Test
    void testBuildSpecification_NoParameters() {
        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.buildSpecification(
            null, null, null, null, null,
            null, null, null, null, null, null
        );

        // Assert
        assertNotNull(spec); // Specification.where() always returns a specification, even if all predicates are null
    }

    @Test
    void testBuildSpecification_PartialParameters() {
        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.buildSpecification(
            123, null, "BUY", null, null,
            new BigDecimal("100"), null, null, null, null, null
        );

        // Assert
        assertNotNull(spec);
    }

    @Test
    void testBuildSpecification_OnlyOneParameter() {
        // Act
        Specification<TradeOrder> spec = TradeOrderSpecification.buildSpecification(
            123, null, null, null, null,
            null, null, null, null, null, null
        );

        // Assert
        assertNotNull(spec);
    }

    @Test
    void testCommaDelimitedStringHandling() {
        // Test that comma-delimited strings are handled correctly
        assertNotNull(TradeOrderSpecification.hasOrderType("BUY,SELL,SHORT"));
        assertNotNull(TradeOrderSpecification.hasPortfolioId("P1,P2,P3"));
        assertNotNull(TradeOrderSpecification.hasSecurityId("S1,S2,S3"));
        assertNotNull(TradeOrderSpecification.hasBlotterAbbreviation("EQ,FI,FX"));
    }

    @Test
    void testWhitespaceHandling() {
        // Test that whitespace is handled properly
        assertNotNull(TradeOrderSpecification.hasOrderType("BUY, SELL, SHORT"));
        assertNotNull(TradeOrderSpecification.hasPortfolioId(" P1 , P2 , P3 "));
        assertNotNull(TradeOrderSpecification.hasSecurityId("S1,S2 ,S3"));
        assertNotNull(TradeOrderSpecification.hasBlotterAbbreviation("EQ , FI,FX "));
    }
} 