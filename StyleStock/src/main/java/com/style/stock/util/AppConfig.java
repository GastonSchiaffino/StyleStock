package com.style.stock.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class AppConfig {
    private static final Path CONFIG_DIR = Paths.get(System.getProperty("user.home"), "style-stock");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.properties");
    private static Properties props = new Properties();

    static {
        try {
            if (!Files.exists(CONFIG_DIR)) Files.createDirectories(CONFIG_DIR);
            if (Files.exists(CONFIG_FILE)) {
                try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
                    props.load(in);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String get(String key, String def) {
        return props.getProperty(key, def);
    }

    public static void set(String key, String value) {
        props.setProperty(key, value);
        try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
            props.store(out, "Style Stock config");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
