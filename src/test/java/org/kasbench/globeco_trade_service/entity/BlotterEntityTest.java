package org.kasbench.globeco_trade_service.entity;

import org.junit.jupiter.api.Test;
import jakarta.persistence.Version;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class BlotterEntityTest {
    @Test
    void testGettersAndSetters() {
        Blotter blotter = new Blotter();
        blotter.setId(1);
        blotter.setAbbreviation("EQ");
        blotter.setName("Equity");
        blotter.setVersion(2);

        assertEquals(1, blotter.getId());
        assertEquals("EQ", blotter.getAbbreviation());
        assertEquals("Equity", blotter.getName());
        assertEquals(2, blotter.getVersion());
    }

    @Test
    void testVersionAnnotationPresent() throws NoSuchFieldException {
        Field versionField = Blotter.class.getDeclaredField("version");
        assertNotNull(versionField.getAnnotation(Version.class), "@Version annotation should be present on version field");
    }
} 