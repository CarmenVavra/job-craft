package com.jobcraft.service.backup;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Überträgt alle JobCraft-Tabellen aus einer H2-Datenbank in eine (leere) PostgreSQL-Datenbank – in einer
 * Transaktion. Spalten werden über ihren Namen zugeordnet; JSON-Spalten werden als {@code jsonb} übernommen.
 */
public final class H2Import {

    private H2Import() {
    }

    /**
     * @param h2       Quelle (H2, nur lesend genutzt)
     * @param postgres Ziel (PostgreSQL, Schema bereits per Flyway angelegt, Tabellen leer)
     * @return übernommene Zeilen je Tabelle
     * @throws SQLException bei Fehlern; das Ziel bleibt dann unverändert
     */
    public static Map<String, Integer> copy(Connection h2, Connection postgres) throws SQLException {
        boolean autoCommit = postgres.getAutoCommit();
        postgres.setAutoCommit(false);
        try {
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (String table : PostgresDump.TABLES) {
                counts.put(table, copyTable(h2, postgres, table));
            }
            postgres.commit();
            return counts;
        } catch (SQLException | RuntimeException e) {
            postgres.rollback();
            throw e;
        } finally {
            postgres.setAutoCommit(autoCommit);
        }
    }

    private static int copyTable(Connection h2, Connection postgres, String table) throws SQLException {
        Map<String, String> targetTypes = new LinkedHashMap<>();
        try (ResultSet rs = postgres.getMetaData().getColumns(null, "public", table, null)) {
            while (rs.next()) {
                targetTypes.put(rs.getString("COLUMN_NAME"), rs.getString("TYPE_NAME"));
            }
        }
        try (ResultSet source = h2.createStatement().executeQuery("SELECT * FROM " + table)) {
            ResultSetMetaData meta = source.getMetaData();
            List<String> columns = new ArrayList<>();
            Set<String> json = new TreeSet<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                String name = meta.getColumnLabel(i).toLowerCase();
                if (targetTypes.containsKey(name)) {
                    columns.add(name);
                    if (targetTypes.get(name).startsWith("json")) {
                        json.add(name);
                    }
                }
            }
            StringBuilder sql = new StringBuilder("INSERT INTO ").append(table).append(" (")
                    .append(String.join(", ", columns)).append(") VALUES (");
            for (int i = 0; i < columns.size(); i++) {
                sql.append(i == 0 ? "" : ", ").append(json.contains(columns.get(i)) ? "CAST(? AS jsonb)" : "?");
            }
            sql.append(')');
            int rows = 0;
            try (PreparedStatement insert = postgres.prepareStatement(sql.toString())) {
                while (source.next()) {
                    for (int i = 0; i < columns.size(); i++) {
                        String column = columns.get(i);
                        if (json.contains(column)) {
                            byte[] value = source.getBytes(column);
                            insert.setString(i + 1, value == null ? null : new String(value, StandardCharsets.UTF_8));
                        } else {
                            insert.setObject(i + 1, source.getObject(column));
                        }
                    }
                    insert.addBatch();
                    rows++;
                }
                insert.executeBatch();
            }
            return rows;
        }
    }
}
