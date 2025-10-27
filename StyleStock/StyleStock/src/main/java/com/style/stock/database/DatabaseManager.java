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
            config.setMaximumPoolSize(5); // SQLite no soporta muchas conexiones concurrentes
            config.setMinimumIdle(1);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            
            // Configuraciones específicas para SQLite
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            
            // Configuración PRAGMA para SQLite
            config.setConnectionInitSql(
                "PRAGMA foreign_keys = ON;" +
                "PRAGMA journal_mode = WAL;" +  // Write-Ahead Logging para mejor concurrencia
                "PRAGMA synchronous = NORMAL;" +
                "PRAGMA temp_store = MEMORY;" +
                "PRAGMA cache_size = -64000"    // 64MB de cache
            );

            dataSource = new HikariDataSource(config);
            logger.info("Connection pool inicializado correctamente: {}", jdbcUrl);

            // Crear tablas si no existen
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

    /**
     * Obtiene una conexión del pool
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("DataSource no disponible");
        }
        return dataSource.getConnection();
    }

    /**
     * Inicializa el schema de la base de datos
     */
    private void initializeSchema() {
        try (Connection conn = getConnection()) {
            logger.info("Inicializando schema de base de datos...");
            
            InputStream is = getClass().getResourceAsStream("/sql/create_tables.sql");
            if (is == null) {
                logger.warn("Archivo create_tables.sql no encontrado");
                return;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {
                    // Ignorar comentarios
                    if (line.trim().startsWith("--") || line.trim().isEmpty()) {
                        continue;
                    }
                    
                    sb.append(line).append("\n");

                    // Ejecutar cuando encontramos punto y coma
                    if (line.trim().endsWith(";")) {
                        String sql = sb.toString().trim();
                        if (!sql.isEmpty()) {
                            try (Statement stmt = conn.createStatement()) {
                                stmt.execute(sql);
                                logger.debug("SQL ejecutado exitosamente");
                            } catch (SQLException e) {
                                logger.warn("Error ejecutando SQL (puede ser esperado): {}", 
                                           e.getMessage());
                            }
                        }
                        sb.setLength(0);
                    }
                }

                // Ejecutar cualquier SQL pendiente
                if (sb.length() > 0) {
                    String sql = sb.toString().trim();
                    if (!sql.isEmpty()) {
                        try (Statement stmt = conn.createStatement()) {
                            stmt.execute(sql);
                        } catch (SQLException e) {
                            logger.warn("Error ejecutando SQL final: {}", e.getMessage());
                        }
                    }
                }

                logger.info("Schema inicializado correctamente");
            }

        } catch (Exception e) {
            logger.error("Error inicializando schema", e);
            throw new RuntimeException("Error al inicializar el schema de la base de datos", e);
        }
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
