package com.jobcraft.service.backup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

/**
 * Logische Sicherung der JobCraft-Tabellen einer PostgreSQL-Datenbank als ZIP (eine {@code COPY}-Datei je Tabelle
 * plus Manifest mit Spaltenlisten) und Wiederherstellung daraus – ohne externe Werkzeuge wie {@code pg_dump}.
 */
public final class PostgresDump {

    /** Alle Anwendungstabellen in Abhängigkeitsreihenfolge (Eltern vor Kindern). */
    public static final List<String> TABLES = List.of("users", "profiles", "documents", "profile_notes",
            "applications", "cover_letters", "chat_messages");

    private static final String MANIFEST = "manifest.properties";
    private static final String FORMAT = "jobcraft-postgres-copy-1";

    private PostgresDump() {
    }

    /**
     * Schreibt alle Tabellen konsistent (eine Transaktion, Snapshot) in ein ZIP.
     *
     * @param connection PostgreSQL-Verbindung
     * @param target     Ziel des ZIP
     * @return Anzahl Zeilen je Tabelle
     * @throws SQLException bei Datenbankfehlern
     * @throws IOException  beim Schreiben
     */
    public static Map<String, Long> export(Connection connection, OutputStream target) throws SQLException, IOException {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (ZipOutputStream zip = new ZipOutputStream(target, StandardCharsets.UTF_8)) {
            connection.createStatement().execute("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY");
            CopyManager copy = copyManager(connection);
            Properties manifest = new Properties();
            manifest.setProperty("format", FORMAT);
            manifest.setProperty("tables", String.join(",", TABLES));
            Map<String, Long> counts = new HashMap<>();
            for (String table : TABLES) {
                String columns = String.join(",", columns(connection, table));
                manifest.setProperty(table + ".columns", columns);
                ByteArrayOutputStream data = new ByteArrayOutputStream();
                long rows = copy.copyOut("COPY " + table + " (" + columns + ") TO STDOUT WITH (ENCODING 'UTF8')", data);
                counts.put(table, rows);
                manifest.setProperty(table + ".rows", Long.toString(rows));
                zip.putNextEntry(new ZipEntry(table + ".copy"));
                data.writeTo(zip);
                zip.closeEntry();
            }
            zip.putNextEntry(new ZipEntry(MANIFEST));
            manifest.store(zip, "JobCraft-Sicherung");
            zip.closeEntry();
            connection.commit();
            return counts;
        } catch (SQLException | IOException | RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    /**
     * Ersetzt den gesamten Inhalt der Anwendungstabellen durch die Sicherung – in einer Transaktion, d. h. bei einem
     * Fehler bleibt der bisherige Stand unverändert.
     *
     * @param connection PostgreSQL-Verbindung
     * @param source     ZIP aus {@link #export}
     * @return Anzahl Zeilen je Tabelle
     * @throws SQLException bei Datenbankfehlern
     * @throws IOException  beim Lesen oder bei ungültiger Sicherung
     */
    public static Map<String, Long> restore(Connection connection, InputStream source) throws SQLException, IOException {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(source, StandardCharsets.UTF_8)) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                entries.put(entry.getName(), zip.readAllBytes());
            }
        }
        if (!entries.containsKey(MANIFEST)) {
            throw new IOException("Keine JobCraft-Sicherung (manifest.properties fehlt).");
        }
        Properties manifest = new Properties();
        manifest.load(new ByteArrayInputStream(entries.get(MANIFEST)));
        if (!FORMAT.equals(manifest.getProperty("format"))) {
            throw new IOException("Unbekanntes Sicherungsformat: " + manifest.getProperty("format"));
        }
        List<String> tables = List.of(manifest.getProperty("tables").split(","));
        if (!TABLES.containsAll(tables)) {
            throw new IOException("Die Sicherung enthält unbekannte Tabellen.");
        }

        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            connection.createStatement().execute("TRUNCATE " + String.join(", ", TABLES) + " CASCADE");
            CopyManager copy = copyManager(connection);
            Map<String, Long> counts = new HashMap<>();
            for (String table : tables) {
                String columns = manifest.getProperty(table + ".columns");
                byte[] data = entries.get(table + ".copy");
                if (columns == null || data == null) {
                    throw new IOException("Die Sicherung ist unvollständig (" + table + ").");
                }
                counts.put(table, copy.copyIn("COPY " + table + " (" + columns + ") FROM STDIN WITH (ENCODING 'UTF8')",
                        new ByteArrayInputStream(data)));
            }
            connection.commit();
            return counts;
        } catch (SQLException | IOException | RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private static CopyManager copyManager(Connection connection) throws SQLException {
        return connection.unwrap(PGConnection.class).getCopyAPI();
    }

    private static List<String> columns(Connection connection, String table) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (ResultSet rs = connection.getMetaData().getColumns(null, "public", table, null)) {
            while (rs.next()) {
                // JDBC liefert die Spalten nach ORDINAL_POSITION sortiert.
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }
        if (columns.isEmpty()) {
            throw new SQLException("Tabelle " + table + " nicht gefunden.");
        }
        return columns;
    }
}
