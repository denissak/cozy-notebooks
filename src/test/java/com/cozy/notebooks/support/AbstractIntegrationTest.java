package com.cozy.notebooks.support;

import com.cozy.notebooks.domain.NotebookEntity;
import com.cozy.notebooks.domain.PageEntity;
import com.cozy.notebooks.domain.UserPlan;
import com.cozy.notebooks.repository.NotebookRepository;
import com.cozy.notebooks.repository.PageRepository;
import com.cozy.notebooks.repository.UserRepository;
import com.cozy.notebooks.security.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Base class for full-stack integration tests.
 *
 * <p>Two modes — selected at JVM startup via the {@code USE_LOCAL_MYSQL}
 * environment variable (or Gradle {@code -Pcozy.test.useLocalMysql=true},
 * see {@code build.gradle.kts}):
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

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private NotebookRepository notebookRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private UserRepository userRepository;

    private static final boolean USE_LOCAL_MYSQL = MySqlIntegrationSupport.useLocalMysql();

    private static MySQLContainer<?> mysql;

    static {
        if (!USE_LOCAL_MYSQL) {
            try {
                mysql = new MySQLContainer<>("mysql:8.4")
                        .withDatabaseName("cozy_notebooks")
                        .withUsername("cozy")
                        .withPassword("cozy");
                mysql.start();
            } catch (Throwable t) {
                throw new IllegalStateException(
                        """
                                Integration tests could not start MySQL via Testcontainers (Docker not running or not reachable?).
                                Fix one of:
                                1. Start Docker Desktop and re-run ./gradlew test
                                2. Or start MySQL on localhost:3306 (e.g. docker compose up -d mysql) then run:
                                   USE_LOCAL_MYSQL=true ./gradlew clean test
                                   or: ./gradlew clean test -Pcozy.test.useLocalMysql=true
                                See README: "Local-MySQL mode for Windows / IntelliJ users"."""
                                .stripIndent(),
                        t);
            }
        }
    }

    /**
     * Mock-mode tests share one dev user; soft-delete its notebooks/pages before each test
     * so free-plan quota limits do not leak across test methods.
     */
    @BeforeEach
    void resetMockUserQuotaState() {
        if (!securityProperties.mockUserEnabled()) {
            return;
        }
        UUID mockUserId = securityProperties.mockUserId();
        OffsetDateTime now = OffsetDateTime.now();
        for (NotebookEntity notebook : notebookRepository.findByUserIdAndDeletedAtIsNullOrderByPositionAscCreatedAtAsc(
                mockUserId)) {
            notebook.setDeletedAt(now);
            notebookRepository.save(notebook);
        }
        for (PageEntity page : pageRepository.findByUserIdAndDeletedAtIsNull(mockUserId)) {
            page.setDeletedAt(now);
            pageRepository.save(page);
        }
        userRepository.findByIdAndDeletedAtIsNull(mockUserId).ifPresent(user -> {
            if (!UserPlan.FREE.code().equals(user.getPlanCode())) {
                user.setPlanCode(UserPlan.FREE.code());
                userRepository.save(user);
            }
        });
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
