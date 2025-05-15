package org.kasbench.globeco_trade_service;

import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresTestContainer {
    private static final PostgreSQLContainer<?> container;

    static {
        container = new PostgreSQLContainer<>("postgres:latest")
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass");
        container.start();
    }

    public static PostgreSQLContainer<?> getInstance() {
        return container;
    }
} 