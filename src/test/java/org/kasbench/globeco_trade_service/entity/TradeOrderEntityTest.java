package org.kasbench.globeco_trade_service.entity;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class TradeOrderEntityTest {
    @Test
    void testGettersAndSetters() {
        TradeOrder tradeOrder = new TradeOrder();
        tradeOrder.setId(1);
        tradeOrder.setOrderId(100);
        tradeOrder.setPortfolioId("PORT123");
        tradeOrder.setOrderType("BUY");
        tradeOrder.setSecurityId("SEC456");
        tradeOrder.setQuantity(new BigDecimal("100.25"));
        tradeOrder.setLimitPrice(new BigDecimal("10.50"));
        OffsetDateTime now = OffsetDateTime.now();
        tradeOrder.setTradeTimestamp(now);
        tradeOrder.setVersion(2);
        Blotter blotter = new Blotter();
        blotter.setId(5);
        tradeOrder.setBlotter(blotter);
        tradeOrder.setSubmitted(true);

        assertEquals(1, tradeOrder.getId());
        assertEquals(100, tradeOrder.getOrderId());
        assertEquals("PORT123", tradeOrder.getPortfolioId());
        assertEquals("BUY", tradeOrder.getOrderType());
        assertEquals("SEC456", tradeOrder.getSecurityId());
        assertEquals(new BigDecimal("100.25"), tradeOrder.getQuantity());
        assertEquals(new BigDecimal("10.50"), tradeOrder.getLimitPrice());
        assertEquals(now, tradeOrder.getTradeTimestamp());
        assertEquals(2, tradeOrder.getVersion());
        assertEquals(blotter, tradeOrder.getBlotter());
        assertEquals(true, tradeOrder.getSubmitted());
        tradeOrder.setSubmitted(null);
        assertNull(tradeOrder.getSubmitted());
    }

    @Test
    void testVersionAnnotationPresent() throws NoSuchFieldException {
        assertNotNull(TradeOrder.class.getDeclaredField("version").getAnnotation(jakarta.persistence.Version.class));
    }
} 