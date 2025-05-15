package org.kasbench.globeco_trade_service.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TradeTypeEntityTest {
    @Test
    void testGettersAndSetters() {
        TradeType tradeType = new TradeType();
        tradeType.setId(1);
        tradeType.setAbbreviation("BUY");
        tradeType.setDescription("Buy");
        tradeType.setVersion(2);
        assertEquals(1, tradeType.getId());
        assertEquals("BUY", tradeType.getAbbreviation());
        assertEquals("Buy", tradeType.getDescription());
        assertEquals(2, tradeType.getVersion());
    }

    @Test
    void testVersionDefault() {
        TradeType tradeType = new TradeType();
        assertEquals(1, tradeType.getVersion());
    }
} 