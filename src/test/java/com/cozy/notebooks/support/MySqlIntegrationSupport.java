package com.cozy.notebooks.support;

/**
 * Mirrors how {@link AbstractIntegrationTest} chooses between Testcontainers
 * ({@code Docker}) and a JDBC URL on localhost. Kept minimal so JDBC-only tests
 * (Flyway migrations) can share the same rules.
 *
 * @see AbstractIntegrationTest
 */
public final class MySqlIntegrationSupport {

    /**
     * Set by the Gradle test task when you pass {@code -Pcozy.test.useLocalMysql=true},
     * or export {@code USE_LOCAL_MYSQL=true} in your shell.
     */
    public static boolean useLocalMysql() {
        return "true".equalsIgnoreCase(System.getenv("USE_LOCAL_MYSQL"));
    }

    private MySqlIntegrationSupport() {
    }
}
