package org.kasbench.globeco_trade_service;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@Timeout(value = 60, unit = TimeUnit.SECONDS)
public abstract class AbstractH2Test {
    // H2 configuration is handled by application-test.properties
}