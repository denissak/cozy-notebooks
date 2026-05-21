package com.cozy.notebooks.migration;

import com.cozy.notebooks.service.HrefCodeGenerator;
import com.cozy.notebooks.support.MySqlIntegrationSupport;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures V5 backfills {@code href_code} for rows that existed before the column
 * was introduced (migration applied after V4).
 *
 * <p>Uses {@link MySqlIntegrationSupport}: Testcontainers outside local mode,
 * localhost + separate DB ({@link #LOCAL_HREF_DATABASE}) inside local mode
 * ({@code USE_LOCAL_MYSQL=true}).
 */
class FlywayV5HrefCodeBackfillIT {

    /**
     * Isolated logical database — avoids clobbering the {@code cozy_notebooks} schema
     * Spring tests use locally. Created automatically when using localhost + root creds from
     * docker-compose.yml (see README).
     */
    private static final String LOCAL_HREF_DATABASE = "cozy_notebooks_href_migration";

    private static final String MOCK_USER = "00000000-0000-0000-0000-000000000001";

    /** Guarded lazy start (only when not using local MySQL). */
    private static volatile MySQLContainer<?> container;

    @Test
    void v5_migration_backfillsEighteenCharacterHrefCodes() throws Exception {
        JdbcTarget target = jdbcTarget();

        Flyway.configure()
                .cleanDisabled(false)
                .dataSource(target.url(), target.user(), target.password())
                .locations("classpath:db/migration")
                .load()
                .clean();

        Flyway.configure()
                .dataSource(target.url(), target.user(), target.password())
                .locations("classpath:db/migration")
                .target("4")
                .load()
                .migrate();

        String nb1 = "b0000000-0000-4000-8000-000000000011";
        String nb2 = "b0000000-0000-4000-8000-000000000012";
        String tpl1 = "c0000000-0000-4000-8000-000000000021";
        String tpl2 = "c0000000-0000-4000-8000-000000000022";

        try (Connection c = DriverManager.getConnection(target.url(), target.user(), target.password());
             Statement st = c.createStatement()) {
            st.executeUpdate(
                    """
                            INSERT INTO notebooks (id, user_id, title, position, created_at, updated_at)
                            VALUES ('%s', '%s', 'Legacy Notebook A', 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
                                   ('%s', '%s', 'Legacy Notebook B', 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                            """
                            .formatted(nb1, MOCK_USER, nb2, MOCK_USER));

            String hash = "a".repeat(64);
            st.executeUpdate(
                    """
                            INSERT INTO page_templates
                                (id, user_id, name, content_json, content_hash, is_built_in, created_at, updated_at)
                            VALUES
                                ('%s', '%s', 'Tpl A', CAST('{}' AS JSON), '%s', 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
                                ('%s', '%s', 'Tpl B', CAST('{\"k\":true}' AS JSON), '%s', 0,
                                 CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                            """
                            .formatted(tpl1, MOCK_USER, hash, tpl2, MOCK_USER, hash));
        }

        Flyway.configure()
                .dataSource(target.url(), target.user(), target.password())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection c = DriverManager.getConnection(target.url(), target.user(), target.password());
             Statement st = c.createStatement()) {
            List<String> notebookHrefs = new ArrayList<>();
            try (ResultSet rs = st.executeQuery(
                    "SELECT href_code FROM notebooks WHERE user_id = '" + MOCK_USER + "' ORDER BY id")) {
                while (rs.next()) {
                    notebookHrefs.add(rs.getString(1));
                }
            }
            assertThat(notebookHrefs).hasSize(2);
            assertHrefCodes(notebookHrefs);
            assertThat(notebookHrefs.get(0)).isNotEqualTo(notebookHrefs.get(1));

            List<String> templateHrefs = new ArrayList<>();
            try (ResultSet rs = st.executeQuery(
                    "SELECT href_code FROM page_templates WHERE user_id = '" + MOCK_USER + "' ORDER BY id")) {
                while (rs.next()) {
                    templateHrefs.add(rs.getString(1));
                }
            }
            assertThat(templateHrefs).hasSize(2);
            assertHrefCodes(templateHrefs);
            assertThat(templateHrefs.get(0)).isNotEqualTo(templateHrefs.get(1));
        }
    }

    private static JdbcTarget jdbcTarget() {
        try {
            if (MySqlIntegrationSupport.useLocalMysql()) {
                return localMysqlTarget();
            }
            return containerTarget();
        } catch (Exception e) {
            throw new IllegalStateException(
                    """
                            Flyway href backfill integration test failed to acquire MySQL.
                            With Docker/Testcontainers (default): start Docker Desktop or see Testcontainers diagnostics.
                            With local MySQL (USE_LOCAL_MYSQL=true): run docker compose up -d mysql and try again.""",
                    e);
        }
    }

    /** Matches docker-compose default root password — only used for JDBC on localhost in tests. */
    private static final String ROOT_USER = "root";

    /** Matches MYSQL_ROOT_PASSWORD in docker-compose.yml. */
    private static final String ROOT_PASSWORD = "root";

    private static JdbcTarget localMysqlTarget() throws SQLException {
        ensureLocalHrefDatabaseExists();
        String jdbcUrl =
                "jdbc:mysql://localhost:3306/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
                        .formatted(LOCAL_HREF_DATABASE);
        return new JdbcTarget(jdbcUrl, ROOT_USER, ROOT_PASSWORD);
    }

    /**
     * The {@code cozy} user from docker-compose is only granted the default MYSQL_DATABASE —
     * it cannot CREATE DATABASE {@value #LOCAL_HREF_DATABASE}.
     */
    private static void ensureLocalHrefDatabaseExists() throws SQLException {
        String adminUrl =
                "jdbc:mysql://localhost:3306/?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        try (Connection admin = DriverManager.getConnection(adminUrl, ROOT_USER, ROOT_PASSWORD);
             Statement st = admin.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + LOCAL_HREF_DATABASE);
        }
    }

    private static synchronized JdbcTarget containerTarget() {
        if (container == null) {
            container = new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName(LOCAL_HREF_DATABASE)
                    .withUsername("cozy")
                    .withPassword("cozy");
            container.start();
        }
        return new JdbcTarget(container.getJdbcUrl(), container.getUsername(), container.getPassword());
    }

    private static void assertHrefCodes(List<String> hrefs) {
        for (String href : hrefs) {
            assertThat(href).hasSize(18);
            assertThat(validHrefAlphabet(href)).isTrue();
        }
        if (hrefs.size() > 1) {
            assertThat(hrefs.stream().distinct().count()).as("href codes unique").isEqualTo(hrefs.size());
        }
    }

    private static boolean validHrefAlphabet(String href) {
        for (char ch : href.toCharArray()) {
            if (HrefCodeGenerator.ALPHABET.indexOf(ch) < 0) {
                return false;
            }
        }
        return true;
    }

    private record JdbcTarget(String url, String user, String password) {
    }
}
