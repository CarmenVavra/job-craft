package com.jobcraft.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Lokale PostgreSQL-Instanz für die Entwicklung (Profil {@code dev}) – ohne Installation oder Docker.
 * Die Daten liegen dauerhaft in {@code data/postgres}; PostgreSQL schreibt jede Transaktion sofort sicher auf die
 * Platte (fsync, synchronous_commit) und übersteht Standby, Stromausfall und hartes Beenden per WAL-Recovery.
 */
@Slf4j
@Configuration
@Profile("dev")
public class EmbeddedPostgresConfig {

    /**
     * Startet PostgreSQL bzw. übernimmt den vorhandenen Datenbestand.
     *
     * @param dataDirectory Datenverzeichnis
     * @param binDirectory  Verzeichnis für die entpackten PostgreSQL-Programme
     * @param port          lokaler Port (nur 127.0.0.1)
     * @return laufende Instanz; wird beim Herunterfahren geordnet beendet
     * @throws IOException wenn PostgreSQL nicht starten kann
     */
    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres(@Value("${jobcraft.embedded-db.data-directory}") String dataDirectory,
                                             @Value("${jobcraft.embedded-db.bin-directory}") String binDirectory,
                                             @Value("${jobcraft.embedded-db.port}") int port) throws IOException {
        Path data = Path.of(dataDirectory).toAbsolutePath().normalize();
        Path bin = Path.of(binDirectory).toAbsolutePath().normalize();
        Files.createDirectories(data);
        Files.createDirectories(bin);
        EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setDataDirectory(data)
                .setCleanDataDirectory(false)
                .setOverrideWorkingDirectory(bin.toFile())
                .setPort(port)
                .setPGStartupWait(Duration.ofSeconds(60))
                // Die Bibliothek ist für Tests auf Tempo eingestellt – hier zählt Datensicherheit.
                .setServerConfig("fsync", "on")
                .setServerConfig("synchronous_commit", "on")
                .setServerConfig("full_page_writes", "on")
                .setServerConfig("listen_addresses", "127.0.0.1")
                .setServerConfig("max_connections", "50")
                .start();
        log.info("Lokale PostgreSQL-Datenbank läuft auf Port {} (Daten: {})", port, data);
        return postgres;
    }

    /**
     * @param postgres laufende Instanz
     * @return Verbindungspool zur Datenbank {@code postgres}
     */
    @Bean
    public DataSource dataSource(EmbeddedPostgres postgres) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl("postgres", "postgres"));
        config.setUsername("postgres");
        config.setMaximumPoolSize(10);
        config.setPoolName("JobCraftPostgres");
        return new HikariDataSource(config);
    }
}
