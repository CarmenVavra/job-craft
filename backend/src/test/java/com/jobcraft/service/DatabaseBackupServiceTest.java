package com.jobcraft.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jobcraft.config.JobCraftProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class DatabaseBackupServiceTest {

    @TempDir
    Path tempDir;

    private DatabaseBackupService service(String url, Path backupDir, int keep, Instant now) {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(url, "sa", ""));
        jdbc.execute("CREATE TABLE IF NOT EXISTS t (id INT)");
        JobCraftProperties props = new JobCraftProperties(null, null, null, null, null, null,
                new JobCraftProperties.Backup(true, backupDir.toString(), Duration.ofHours(1), keep));
        return new DatabaseBackupService(jdbc, props, Clock.fixed(now, ZoneId.of("Europe/Berlin")));
    }

    @Test
    void writesZipBackupOfFileDatabaseAndKeepsOnlyNewest() throws IOException {
        String url = "jdbc:h2:file:" + tempDir.resolve("db").toAbsolutePath().toString().replace('\\', '/');
        Path backups = tempDir.resolve("backups");
        Instant start = Instant.parse("2026-10-20T05:00:00Z");

        for (int i = 0; i < 4; i++) {
            assertThat(service(url, backups, 2, start.plusSeconds(3600L * i)).backup()).isPresent();
        }

        List<String> names;
        try (Stream<Path> files = Files.list(backups)) {
            names = files.map(p -> p.getFileName().toString()).sorted().toList();
        }
        assertThat(names).containsExactly("jobcraft-20261020-090000.zip", "jobcraft-20261020-100000.zip");
        assertThat(Files.size(backups.resolve(names.get(1)))).isPositive();
    }

    @Test
    void skipsInMemoryDatabases() {
        Path backups = tempDir.resolve("mem-backups");
        assertThat(service("jdbc:h2:mem:backup-test", backups, 5, Instant.now()).backup()).isEmpty();
        assertThat(backups).doesNotExist();
    }
}
