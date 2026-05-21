package com.cozy.notebooks.migration;

import com.cozy.notebooks.service.HrefCodeGenerator;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures V5 backfills {@code href_code} for rows that existed before the column
 * was introduced (migration applied after V4).
 */
@Testcontainers
class FlywayV5HrefCodeBackfillIT {

    private static final String MOCK_USER = "00000000-0000-0000-0000-000000000001";

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("cozy_notebooks_href_migration")
            .withUsername("cozy")
            .withPassword("cozy");

    @Test
    void v5_migration_backfillsEighteenCharacterHrefCodes() throws Exception {
        String url = MYSQL.getJdbcUrl();
        String user = MYSQL.getUsername();
        String pass = MYSQL.getPassword();

        Flyway.configure()
                .dataSource(url, user, pass)
                .locations("classpath:db/migration")
                .target("4")
                .load()
                .migrate();

        String nb1 = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1";
        String nb2 = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2";
        String tpl1 = "cccccccc-cccc-cccc-cccc-ccccccccccc1";
        String tpl2 = "cccccccc-cccc-cccc-cccc-ccccccccccc2";

        try (Connection c = DriverManager.getConnection(url, user, pass);
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
                .dataSource(url, user, pass)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection c = DriverManager.getConnection(url, user, pass);
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
}
