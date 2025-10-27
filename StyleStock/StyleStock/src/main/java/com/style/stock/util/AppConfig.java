package com.style.stock.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Gestor de configuración de la aplicación (mejorado)
 */
public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static final Path CONFIG_DIR = Paths.get(System.getProperty("user.home"), "style-stock");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.properties");
    private static Properties props = new Properties();

    static {
        cargarConfiguracion();
    }

    private static void cargarConfiguracion() {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
                logger.info("Directorio de configuración creado: {}", CONFIG_DIR);
            }

            if (Files.exists(CONFIG_FILE)) {
                try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
                    props.load(in);
                    logger.info("Configuración cargada desde: {}", CONFIG_FILE);
                }
            } else {
                // Crear configuración por defecto
                setDefaultConfig();
                guardar();
                logger.info("Configuración por defecto creada");
            }
        } catch (IOException e) {
            logger.error("Error cargando configuración", e);
        }
    }

    private static void setDefaultConfig() {
        props.setProperty("theme", "light");
        props.setProperty("backup_auto", "true");
        props.setProperty("backup_dias", "7");
        props.setProperty("stock_alerta_activa", "true");
    }

    public static String get(String key) {
        return props.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }

    public static void set(String key, String value) {
        props.setProperty(key, value);
        guardar();
    }

    public static void setInt(String key, int value) {
        set(key, String.valueOf(value));
    }

    public static void setBoolean(String key, boolean value) {
        set(key, String.valueOf(value));
    }

    private static void guardar() {
        try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
            props.store(out, "StyleStock Configuration");
            logger.debug("Configuración guardada");
        } catch (IOException e) {
            logger.error("Error guardando configuración", e);
        }
    }

    public static Path getAppDirectory() {
        return CONFIG_DIR;
    }
}