package org.kasbench.globeco_trade_service.entity;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class ExecutionEntityTest {
    @Test
    void testGettersAndSetters() {
        Execution execution = new Execution();
        execution.setId(1);
        OffsetDateTime now = OffsetDateTime.now();
        execution.setExecutionTimestamp(now);
        ExecutionStatus status = new ExecutionStatus(); status.setId(2);
        execution.setExecutionStatus(status);
        Blotter blotter = new Blotter(); blotter.setId(3);
        execution.setBlotter(blotter);
        TradeType tradeType = new TradeType(); tradeType.setId(4);
        execution.setTradeType(tradeType);
        TradeOrder tradeOrder = new TradeOrder(); tradeOrder.setId(5);
        execution.setTradeOrder(tradeOrder);
        Destination destination = new Destination(); destination.setId(6);
        execution.setDestination(destination);
        execution.setQuantityOrdered(new BigDecimal("10"));
        execution.setQuantityPlaced(new BigDecimal("100.12345678"));
        execution.setQuantityFilled(new BigDecimal("50.12345678"));
        execution.setLimitPrice(new BigDecimal("99.99"));
        execution.setVersion(7);

        assertEquals(1, execution.getId());
        assertEquals(now, execution.getExecutionTimestamp());
        assertEquals(status, execution.getExecutionStatus());
        assertEquals(blotter, execution.getBlotter());
        assertEquals(tradeType, execution.getTradeType());
        assertEquals(tradeOrder, execution.getTradeOrder());
        assertEquals(destination, execution.getDestination());
        assertEquals(new BigDecimal("10"), execution.getQuantityOrdered());
        assertEquals(new BigDecimal("100.12345678"), execution.getQuantityPlaced());
        assertEquals(new BigDecimal("50.12345678"), execution.getQuantityFilled());
        assertEquals(new BigDecimal("99.99"), execution.getLimitPrice());
        assertEquals(7, execution.getVersion());
    }

    @Test
    void testVersionAnnotationPresent() throws NoSuchFieldException {
        assertNotNull(Execution.class.getDeclaredField("version").getAnnotation(jakarta.persistence.Version.class));
    }
} 