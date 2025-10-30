package com.style.stock.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de base de datos con Connection Pool (HikariCP)
 * Implementa patrón Singleton
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static DatabaseManager instance;
    private HikariDataSource dataSource;
    private static final String DB_FILE_NAME = "style_stock.db";

    private DatabaseManager() {
        initializeDataSource();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializeDataSource() {
        try {
            Path dbPath = getDefaultDbPath();
            String jdbcUrl = "jdbc:sqlite:" + dbPath.toString();

            // Configuración de HikariCP
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setMaximumPoolSize(5);
            config.setMinimumIdle(1);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);

            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            config.setConnectionInitSql(
                    "PRAGMA foreign_keys = ON;" +
                            "PRAGMA journal_mode = WAL;" +
                            "PRAGMA synchronous = NORMAL;" +
                            "PRAGMA temp_store = MEMORY;" +
                            "PRAGMA cache_size = -64000"
            );

            dataSource = new HikariDataSource(config);
            logger.info("Connection pool inicializado correctamente: {}", jdbcUrl);

            initializeSchema();

        } catch (Exception e) {
            logger.error("Error inicializando base de datos", e);
            throw new RuntimeException("No se pudo inicializar la base de datos", e);
        }
    }

    private Path getDefaultDbPath() {
        String userHome = System.getProperty("user.home");
        Path appDir = Paths.get(userHome, "style-stock");
        try {
            if (!Files.exists(appDir)) {
                Files.createDirectories(appDir);
                logger.info("Directorio de aplicación creado: {}", appDir);
            }
        } catch (Exception e) {
            logger.error("Error creando directorio de aplicación", e);
        }
        return appDir.resolve(DB_FILE_NAME);
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("DataSource no disponible");
        }
        return dataSource.getConnection();
    }

    /**
     * Inicializa el schema - VERSIÓN CORREGIDA para manejar triggers y bloques BEGIN/END
     */
    private void initializeSchema() {
        try (Connection conn = getConnection()) {
            logger.info("Inicializando schema de base de datos...");

            InputStream is = getClass().getResourceAsStream("/sql/create_tables.sql");
            if (is == null) {
                logger.warn("Archivo create_tables.sql no encontrado");
                return;
            }

            List<String> statements = parseSqlStatements(is);

            for (String sql : statements) {
                if (sql.trim().isEmpty()) continue;

                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                    logger.debug("SQL ejecutado exitosamente: {}",
                            sql.length() > 50 ? sql.substring(0, 50) + "..." : sql);
                } catch (SQLException e) {
                    // Solo log de warning para errores esperados (tablas ya existentes, etc)
                    if (e.getMessage().contains("already exists") ||
                            e.getMessage().contains("duplicate column")) {
                        logger.debug("SQL ya ejecutado previamente: {}", e.getMessage());
                    } else {
                        logger.warn("Error ejecutando SQL: {}", e.getMessage());
                    }
                }
            }

            logger.info("Schema inicializado correctamente");

        } catch (Exception e) {
            logger.error("Error inicializando schema", e);
            throw new RuntimeException("Error al inicializar el schema de la base de datos", e);
        }
    }

    /**
     * Parser mejorado que maneja correctamente triggers con BEGIN/END
     */
    private List<String> parseSqlStatements(InputStream is) throws Exception {
        List<String> statements = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder currentStatement = new StringBuilder();
            String line;
            int beginCount = 0;
            boolean inTrigger = false;

            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();

                // Ignorar comentarios y líneas vacías
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }

                // Detectar inicio de trigger o bloque BEGIN
                if (trimmed.toUpperCase().startsWith("CREATE TRIGGER") ||
                        trimmed.toUpperCase().startsWith("CREATE VIEW")) {
                    inTrigger = true;
                }

                // Contar BEGIN/END para triggers
                if (trimmed.toUpperCase().equals("BEGIN")) {
                    beginCount++;
                }
                if (trimmed.toUpperCase().equals("END;")) {
                    beginCount--;
                }

                currentStatement.append(line).append("\n");

                // Determinar si la sentencia está completa
                boolean isComplete = false;

                if (inTrigger) {
                    // Para triggers, esperar hasta END;
                    if (trimmed.toUpperCase().equals("END;") && beginCount == 0) {
                        isComplete = true;
                        inTrigger = false;
                    }
                } else {
                    // Para sentencias normales, buscar punto y coma al final
                    if (trimmed.endsWith(";")) {
                        isComplete = true;
                    }
                }

                if (isComplete) {
                    String stmt = currentStatement.toString().trim();
                    if (!stmt.isEmpty()) {
                        statements.add(stmt);
                    }
                    currentStatement.setLength(0);
                }
            }

            // Agregar cualquier sentencia pendiente
            if (currentStatement.length() > 0) {
                String stmt = currentStatement.toString().trim();
                if (!stmt.isEmpty()) {
                    statements.add(stmt);
                }
            }
        }

        logger.debug("Parseadas {} sentencias SQL", statements.size());
        return statements;
    }

    /**
     * Cierra el pool de conexiones
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Connection pool cerrado correctamente");
        }
    }

    /**
     * Verifica el estado de la base de datos
     */
    public boolean isHealthy() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT 1");
            return true;
        } catch (SQLException e) {
            logger.error("Health check falló", e);
            return false;
        }
    }

    /**
     * Obtiene estadísticas del pool de conexiones
     */
    public String getPoolStats() {
        if (dataSource != null) {
            return String.format("Pool Stats - Activas: %d, Inactivas: %d, Total: %d",
                    dataSource.getHikariPoolMXBean().getActiveConnections(),
                    dataSource.getHikariPoolMXBean().getIdleConnections(),
                    dataSource.getHikariPoolMXBean().getTotalConnections());
        }
        return "DataSource no disponible";
    }
}