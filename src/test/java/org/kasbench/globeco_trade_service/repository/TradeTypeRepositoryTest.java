package org.kasbench.globeco_trade_service.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kasbench.globeco_trade_service.entity.TradeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TradeTypeRepositoryTest extends org.kasbench.globeco_trade_service.AbstractPostgresContainerTest {
    @Autowired
    private TradeTypeRepository tradeTypeRepository;

    @Test
    void testCrud() {
        TradeType tradeType = new TradeType();
        tradeType.setAbbreviation("BUY");
        tradeType.setDescription("Buy");
        TradeType saved = tradeTypeRepository.save(tradeType);
        assertNotNull(saved.getId());
        TradeType found = tradeTypeRepository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals("BUY", found.getAbbreviation());
        found.setDescription("Updated");
        tradeTypeRepository.save(found);
        TradeType updated = tradeTypeRepository.findById(saved.getId()).orElse(null);
        assertEquals("Updated", updated.getDescription());
        tradeTypeRepository.deleteById(saved.getId());
        assertFalse(tradeTypeRepository.findById(saved.getId()).isPresent());
    }

    @Test
    // @Disabled("Disabled: persistent failures with optimistic locking exception detection in test environment")
    void testOptimisticConcurrency() {
        TradeType tradeType = new TradeType();
        tradeType.setAbbreviation("BUY");
        tradeType.setDescription("Buy");
        TradeType saved = tradeTypeRepository.save(tradeType);
        TradeType found1 = tradeTypeRepository.findById(saved.getId()).orElse(null);
        TradeType found2 = tradeTypeRepository.findById(saved.getId()).orElse(null);
        found1.setDescription("A");
        tradeTypeRepository.save(found1);
        found2.setDescription("B");
        assertThrows(OptimisticLockingFailureException.class, () -> tradeTypeRepository.saveAndFlush(found2));
    }
} 