package com.jobcraft.config;

import com.jobcraft.service.backup.H2Import;
import com.jobcraft.service.backup.PostgresDump;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Bereitet die lokale PostgreSQL-Datenbank nach den Flyway-Migrationen vor:
 * <ul>
 *   <li>Mit {@code jobcraft.restore-from} (Umgebungsvariable {@code JOBCRAFT_RESTORE_FROM}, gesetzt von
 *       {@code restore.bat}) wird eine Sicherung zurückgespielt – vorher wird der aktuelle Stand gesichert.</li>
 *   <li>Ist die Datenbank leer und gibt es noch die frühere H2-Datei, werden deren Daten einmalig übernommen und die
 *       Datei danach umbenannt.</li>
 * </ul>
 */
@Slf4j
@Configuration
@Profile("dev")
public class DevDatabaseInitializer {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final String restoreFrom;
    private final Path legacyH2;
    private final Path backupDirectory;
    private final Clock clock;

    public DevDatabaseInitializer(@Value("${jobcraft.restore-from:}") String restoreFrom,
                                  @Value("${jobcraft.embedded-db.legacy-h2-file}") String legacyH2,
                                  @Value("${jobcraft.backup.directory}") String backupDirectory, Clock clock) {
        this.restoreFrom = restoreFrom;
        this.legacyH2 = Path.of(legacyH2).toAbsolutePath().normalize();
        this.backupDirectory = Path.of(backupDirectory).toAbsolutePath().normalize();
        this.clock = clock;
    }

    /**
     * @return Migration inkl. anschließender Wiederherstellung bzw. H2-Übernahme
     */
    @Bean
    public FlywayMigrationStrategy migrateAndPrepare() {
        return flyway -> {
            flyway.migrate();
            try {
                prepare(flyway.getConfiguration().getDataSource());
            } catch (SQLException | IOException e) {
                throw new IllegalStateException("Die Datenbank konnte nicht vorbereitet werden: " + e.getMessage(), e);
            }
        };
    }

    private void prepare(DataSource dataSource) throws SQLException, IOException {
        try (Connection postgres = dataSource.getConnection()) {
            if (restoreFrom != null && !restoreFrom.isBlank()) {
                restore(postgres, Path.of(restoreFrom).toAbsolutePath().normalize());
            } else if (isEmpty(postgres) && Files.exists(legacyH2.resolveSibling(legacyH2.getFileName() + ".mv.db"))) {
                importLegacyH2(postgres);
            }
        }
    }

    private void restore(Connection postgres, Path zip) throws SQLException, IOException {
        if (!Files.isRegularFile(zip)) {
            throw new IOException("Sicherung nicht gefunden: " + zip);
        }
        Files.createDirectories(backupDirectory);
        Path safety = backupDirectory.resolve("vor-wiederherstellung-" + LocalDateTime.now(clock).format(STAMP) + ".zip");
        try (OutputStream out = Files.newOutputStream(safety)) {
            PostgresDump.export(postgres, out);
        }
        log.info("Aktueller Stand vor der Wiederherstellung gesichert: {}", safety);
        Map<String, Long> counts;
        try (InputStream in = Files.newInputStream(zip)) {
            counts = PostgresDump.restore(postgres, in);
        }
        log.warn("Sicherung {} wiederhergestellt: {}", zip.getFileName(), counts);
    }

    private void importLegacyH2(Connection postgres) throws SQLException, IOException {
        Path file = legacyH2.resolveSibling(legacyH2.getFileName() + ".mv.db");
        String url = "jdbc:h2:file:" + legacyH2.toString().replace('\\', '/')
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;ACCESS_MODE_DATA=r;IFEXISTS=TRUE";
        Map<String, Integer> counts;
        try (Connection h2 = DriverManager.getConnection(url, "sa", "")) {
            counts = H2Import.copy(h2, postgres);
        }
        Path moved = file.resolveSibling(file.getFileName() + ".uebernommen-" + LocalDateTime.now(clock).format(STAMP));
        Files.move(file, moved);
        log.warn("Daten aus der bisherigen H2-Datenbank übernommen: {} – Datei umbenannt in {}", counts,
                moved.getFileName());
    }

    private static boolean isEmpty(Connection postgres) throws SQLException {
        try (ResultSet rs = postgres.createStatement().executeQuery("SELECT EXISTS (SELECT 1 FROM users)")) {
            rs.next();
            return !rs.getBoolean(1);
        }
    }
}
