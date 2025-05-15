package org.kasbench.globeco_trade_service.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DestinationEntityTest {
    @Test
    void testGettersAndSetters() {
        Destination destination = new Destination();
        destination.setId(1);
        destination.setAbbreviation("ML");
        destination.setDescription("Merrill Lynch");
        destination.setVersion(2);
        assertEquals(1, destination.getId());
        assertEquals("ML", destination.getAbbreviation());
        assertEquals("Merrill Lynch", destination.getDescription());
        assertEquals(2, destination.getVersion());
    }

    @Test
    void testVersionDefault() {
        Destination destination = new Destination();
        assertEquals(1, destination.getVersion());
    }
} 