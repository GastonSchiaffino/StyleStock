package com.style.stock.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private static final String DB_FILE_NAME = "style_stock.db";

    private static Path getDefaultDbPath() {
        String userHome = System.getProperty("user.home");
        Path appDir = Paths.get(userHome, "style-stock");
        try {
            if (!Files.exists(appDir)) Files.createDirectories(appDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return appDir.resolve(DB_FILE_NAME);
    }

    private static String getJdbcUrl() {
        return "jdbc:sqlite:" + getDefaultDbPath().toString();
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(getJdbcUrl());
    }
}
