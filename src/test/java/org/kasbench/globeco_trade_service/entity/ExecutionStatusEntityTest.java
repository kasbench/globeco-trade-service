package org.kasbench.globeco_trade_service.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ExecutionStatusEntityTest {
    @Test
    void testGettersAndSetters() {
        ExecutionStatus status = new ExecutionStatus();
        status.setId(1);
        status.setAbbreviation("NEW");
        status.setDescription("New");
        status.setVersion(2);
        assertEquals(1, status.getId());
        assertEquals("NEW", status.getAbbreviation());
        assertEquals("New", status.getDescription());
        assertEquals(2, status.getVersion());
    }

    @Test
    void testVersionDefault() {
        ExecutionStatus status = new ExecutionStatus();
        assertEquals(1, status.getVersion());
    }
} 