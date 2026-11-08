package com.jobcraft.service.backup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jobcraft.config.JobCraftProperties;
import com.jobcraft.service.DatabaseBackupService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Übernahme aus H2, logische Sicherung und Wiederherstellung gegen eine echte (eingebettete) PostgreSQL-Instanz.
 */
class PostgresBackupTest {

    private static EmbeddedPostgres postgres;
    private static DataSource dataSource;
    private static JdbcTemplate jdbc;

    @TempDir
    Path tempDir;

    @BeforeAll
    static void startPostgres() throws IOException {
        postgres = EmbeddedPostgres.builder().start();
        dataSource = postgres.getPostgresDatabase();
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration/postgresql").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
    }

    @AfterAll
    static void stopPostgres() throws IOException {
        postgres.close();
    }

    @BeforeEach
    void emptyTables() {
        jdbc.execute("TRUNCATE " + String.join(", ", PostgresDump.TABLES) + " CASCADE");
    }

    @Test
    void importsAllTablesFromH2IncludingJsonAndUmlauts() throws SQLException {
        String url = "jdbc:h2:mem:legacy-" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration/h2").load().migrate();
        try (Connection h2 = DriverManager.getConnection(url, "sa", "")) {
            seed(new JdbcTemplate(new org.springframework.jdbc.datasource.SingleConnectionDataSource(h2, true)), true);
            Map<String, Integer> counts;
            try (Connection pg = dataSource.getConnection()) {
                counts = H2Import.copy(h2, pg);
            }
            assertThat(counts).containsEntry("users", 1).containsEntry("applications", 1)
                    .containsEntry("cover_letters", 2).containsEntry("chat_messages", 1)
                    .containsEntry("profile_notes", 1);
        }

        assertThat(jdbc.queryForObject("SELECT custom_fields->0->>'label' FROM profiles", String.class))
                .isEqualTo("Führerschein");
        assertThat(jdbc.queryForObject("SELECT requirements->0->>'text' FROM applications", String.class))
                .isEqualTo("Java & Spring");
        assertThat(jdbc.queryForObject("SELECT salutation FROM applications", String.class))
                .isEqualTo("Liebe Frau Kessler,");
        assertThat(jdbc.queryForObject("SELECT paragraphs->>1 FROM cover_letters WHERE version = 2", String.class))
                .isEqualTo("Zweiter Absatz – überarbeitet");
    }

    @Test
    void restoresExactlyTheExportedState() throws Exception {
        seed(jdbc, false);
        ByteArrayOutputStream zip = new ByteArrayOutputStream();
        try (Connection c = dataSource.getConnection()) {
            assertThat(PostgresDump.export(c, zip)).containsEntry("applications", 1L);
        }
        String before = snapshot();

        jdbc.update("DELETE FROM applications");
        jdbc.update("UPDATE profiles SET full_name = 'Geändert'");
        assertThat(snapshot()).isNotEqualTo(before);

        try (Connection c = dataSource.getConnection()) {
            PostgresDump.restore(c, new ByteArrayInputStream(zip.toByteArray()));
        }
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void keepsDataWhenTheBackupIsInvalid() throws Exception {
        seed(jdbc, false);
        String before = snapshot();
        try (Connection c = dataSource.getConnection()) {
            assertThatThrownBy(() -> PostgresDump.restore(c, new ByteArrayInputStream(new byte[] {1, 2, 3})))
                    .isInstanceOf(IOException.class);
        }
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void backupServiceWritesRestorableZipsAndKeepsOnlyTheNewest() throws Exception {
        seed(jdbc, false);
        Path dir = tempDir.resolve("backups");
        Instant start = Instant.parse("2026-11-08T10:00:00Z");
        Path last = null;
        for (int i = 0; i < 3; i++) {
            JobCraftProperties props = new JobCraftProperties(null, null, null, null, null, null,
                    new JobCraftProperties.Backup(true, dir.toString(), Duration.ofHours(1), 2));
            last = new DatabaseBackupService(jdbc, props, Clock.fixed(start.plusSeconds(3600L * i), ZoneId.of("UTC")))
                    .backup().orElseThrow();
        }
        try (var files = Files.list(dir)) {
            assertThat(files.map(p -> p.getFileName().toString()).sorted().toList())
                    .containsExactly("jobcraft-20261108-110000.zip", "jobcraft-20261108-120000.zip");
        }

        String before = snapshot();
        jdbc.update("DELETE FROM users");
        try (Connection c = dataSource.getConnection(); InputStream in = Files.newInputStream(last)) {
            PostgresDump.restore(c, in);
        }
        assertThat(snapshot()).isEqualTo(before);
    }

    /** Legt einen Benutzer mit Profil, Angabe, Bewerbung, zwei Versionen und einer Chat-Nachricht an. */
    private static void seed(JdbcTemplate db, boolean h2) {
        String json = h2 ? " FORMAT JSON" : "::jsonb";
        UUID user = UUID.randomUUID();
        UUID app = UUID.randomUUID();
        db.update("INSERT INTO users (id, email, password_hash) VALUES (?, 'lena@example.de', 'x')", user);
        db.update("INSERT INTO profiles (user_id, full_name, letter_style, custom_fields) VALUES (?, 'Lena Hoffmann',"
                + " 'MODERN', '[{\"label\":\"Führerschein\",\"value\":\"Klasse B\"}]'" + json + ")", user);
        db.update("INSERT INTO profile_notes (user_id, title, content) VALUES (?, 'Privatprojekt', 'Angular')", user);
        db.update("INSERT INTO applications (id, user_id, job_url, company_name, status, salutation, requirements)"
                + " VALUES (?, ?, 'https://example.de/job', 'Quantis', 'SENT', 'Liebe Frau Kessler,',"
                + " '[{\"text\":\"Java & Spring\",\"matched\":true}]'" + json + ")", app, user);
        db.update("INSERT INTO cover_letters (application_id, version, paragraphs) VALUES (?, 1,"
                + " '[\"Erster Absatz\",\"Zweiter Absatz\"]'" + json + ")", app);
        db.update("INSERT INTO cover_letters (application_id, version, paragraphs) VALUES (?, 2,"
                + " '[\"Erster Absatz\",\"Zweiter Absatz – überarbeitet\"]'" + json + ")", app);
        db.update("INSERT INTO chat_messages (application_id, role, message, meta) VALUES (?, 'SYSTEM', 'Tab\\tund"
                + " Zeilen\\numbruch \\\\ Backslash', 'v2')", app);
    }

    /** Gesamter Inhalt aller Tabellen als Text, um Stände exakt vergleichen zu können. */
    private static String snapshot() {
        StringBuilder sb = new StringBuilder();
        for (String table : PostgresDump.TABLES) {
            sb.append(table).append(':').append(jdbc.queryForList(
                    "SELECT to_jsonb(t)::text AS row FROM " + table + " t ORDER BY 1", String.class)).append('\n');
        }
        return sb.toString();
    }
}
