package com.cozy.notebooks.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base class for full-stack integration tests.
 *
 * <p>Two modes — selected at JVM startup via the {@code USE_LOCAL_MYSQL}
 * environment variable:
 *
 * <ul>
 *   <li><b>Default ({@code USE_LOCAL_MYSQL} unset / not "true")</b> —
 *       Testcontainers starts a fresh {@code mysql:8.4} container per JVM
 *       (matches MySQL HeatWave on OCI). This is what runs in CI.</li>
 *   <li><b>Local mode ({@code USE_LOCAL_MYSQL=true})</b> — no container is
 *       constructed and no container is started; Spring is wired to a MySQL
 *       already running on {@code localhost:3306} (typically the
 *       {@code mysql} service from {@code docker-compose.yml}:
 *       {@code docker compose up -d mysql}). Recommended for IntelliJ on
 *       Windows where Testcontainers' Docker auto-detection is flaky.</li>
 * </ul>
 *
 * <p>Flyway runs migrations on Spring context startup in both modes.
 *
 * <p>{@code @AutoConfigureMockMvc} and {@code @ActiveProfiles("test")} are
 * declared here so existing MockMvc tests continue to work unchanged.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final boolean USE_LOCAL_MYSQL =
            "true".equalsIgnoreCase(System.getenv("USE_LOCAL_MYSQL"));

    private static MySQLContainer<?> mysql;

    static {
        if (!USE_LOCAL_MYSQL) {
            mysql = new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("cozy_notebooks")
                    .withUsername("cozy")
                    .withPassword("cozy");

            mysql.start();
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        if (USE_LOCAL_MYSQL) {
            registry.add("spring.datasource.url",
                    () -> "jdbc:mysql://localhost:3306/cozy_notebooks");
            registry.add("spring.datasource.username", () -> "cozy");
            registry.add("spring.datasource.password", () -> "cozy");
        } else {
            registry.add("spring.datasource.url", mysql::getJdbcUrl);
            registry.add("spring.datasource.username", mysql::getUsername);
            registry.add("spring.datasource.password", mysql::getPassword);
        }
    }
}
