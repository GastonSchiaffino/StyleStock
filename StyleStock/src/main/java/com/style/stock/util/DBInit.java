package com.style.stock.util;

import com.style.stock.model.Database;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;

public class DBInit {
    public static void ensureDatabase() {
        try (Connection conn = Database.connect()) {
            // execute create_tables.sql from resources
            InputStream is = DBInit.class.getResourceAsStream("/sql/create_tables.sql");
            if (is == null) return;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                StringBuilder sb = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                    if (line.trim().endsWith(";")) {
                        String stmt = sb.toString();
                        try (Statement s = conn.createStatement()) {
                            s.execute(stmt);
                        } catch (Exception ex) {
                            // ignore individual statement errors
                        }
                        sb.setLength(0);
                    }
                }
                if (sb.length() > 0) {
                    try (Statement s = conn.createStatement()) {
                        s.execute(sb.toString());
                    } catch (Exception ex) {}
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
