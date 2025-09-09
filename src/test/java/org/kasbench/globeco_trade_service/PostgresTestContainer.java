package org.kasbench.globeco_trade_service;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

@SuppressWarnings("resource")
public class PostgresTestContainer {
    private static PostgreSQLContainer<?> container;

    public static PostgreSQLContainer<?> getInstance() {
        if (container == null) {
            container = new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("testuser")
                    .withPassword("testpass")
                    .withStartupTimeout(Duration.ofSeconds(60))
                    .withConnectTimeoutSeconds(30)
                    .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 1)
                            .withStartupTimeout(Duration.ofSeconds(60)));
            
            container.start();
        }
        return container;
    }
} 