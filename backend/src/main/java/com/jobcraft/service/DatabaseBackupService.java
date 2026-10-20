package com.jobcraft.service;

import com.jobcraft.config.JobCraftProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Sichert die lokale H2-Datenbank als ZIP (beim Start und danach regelmäßig) und behält nur die neuesten Sicherungen.
 * Für PostgreSQL ist der Dienst inaktiv – dort übernimmt die Datenbank-Infrastruktur die Sicherung.
 */
@Slf4j
@Service
public class DatabaseBackupService {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String PREFIX = "jobcraft-";

    private final JdbcTemplate jdbcTemplate;
    private final JobCraftProperties.Backup config;
    private final Clock clock;

    public DatabaseBackupService(JdbcTemplate jdbcTemplate, JobCraftProperties properties, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.config = properties.backup();
        this.clock = clock;
    }

    /** Sicherung direkt nach dem Start, bevor neue Änderungen hinzukommen. */
    @EventListener(ApplicationReadyEvent.class)
    public void backupOnStartup() {
        backupQuietly();
    }

    /** Regelmäßige Sicherung im konfigurierten Intervall. */
    @Scheduled(fixedDelayString = "${jobcraft.backup.interval:PT1H}",
            initialDelayString = "${jobcraft.backup.interval:PT1H}")
    public void backupPeriodically() {
        backupQuietly();
    }

    /**
     * Erstellt eine Sicherung, sofern aktiviert und eine dateibasierte H2-Datenbank verwendet wird.
     *
     * @return Pfad der Sicherung
     */
    public Optional<Path> backup() {
        if (config == null || !config.enabled() || !isFileH2()) {
            return Optional.empty();
        }
        try {
            Path dir = Path.of(config.directory()).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(PREFIX + LocalDateTime.now(clock).format(STAMP) + ".zip");
            jdbcTemplate.execute("BACKUP TO '" + target.toString().replace("'", "''") + "'");
            prune(dir);
            log.info("Datenbank gesichert: {}", target);
            return Optional.of(target);
        } catch (IOException e) {
            throw new IllegalStateException("Sicherungsverzeichnis nicht verfügbar", e);
        }
    }

    private void backupQuietly() {
        try {
            backup();
        } catch (RuntimeException e) {
            log.error("Datenbanksicherung fehlgeschlagen", e);
        }
    }

    private boolean isFileH2() {
        String url = jdbcTemplate.execute((Connection c) -> c.getMetaData().getURL());
        return url != null && url.startsWith("jdbc:h2:file:");
    }

    private void prune(Path dir) throws IOException {
        List<Path> backups;
        try (Stream<Path> files = Files.list(dir)) {
            backups = files.filter(p -> {
                String name = p.getFileName().toString();
                return name.startsWith(PREFIX) && name.endsWith(".zip");
            }).sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed()).toList();
        }
        for (Path old : backups.stream().skip(Math.max(1, config.keep())).toList()) {
            Files.deleteIfExists(old);
        }
    }
}
