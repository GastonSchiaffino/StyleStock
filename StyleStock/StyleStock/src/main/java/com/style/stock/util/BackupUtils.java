package com.style.stock.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utilidades para backup de base de datos
 */
public class BackupUtils {
    private static final Logger logger = LoggerFactory.getLogger(BackupUtils.class);
    private static final String BACKUP_DIR = "backups";

    /**
     * Realiza un backup de la base de datos
     */
    public static boolean realizarBackup() {
        try {
            Path appDir = Paths.get(System.getProperty("user.home"), "style-stock");
            Path dbFile = appDir.resolve("style_stock.db");
            
            if (!Files.exists(dbFile)) {
                logger.warn("Archivo de base de datos no encontrado");
                return false;
            }

            Path backupDir = appDir.resolve(BACKUP_DIR);
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFileName = "style_stock_" + timestamp + ".db";
            Path backupFile = backupDir.resolve(backupFileName);

            Files.copy(dbFile, backupFile);
            logger.info("Backup realizado exitosamente: {}", backupFile);

            // Limpiar backups antiguos (mantener solo los últimos 10)
            limpiarBackupsAntiguos(backupDir, 10);

            return true;

        } catch (IOException e) {
            logger.error("Error realizando backup", e);
            return false;
        }
    }

    /**
     * Restaura un backup
     */
    public static boolean restaurarBackup(Path backupFile) {
        try {
            Path appDir = Paths.get(System.getProperty("user.home"), "style-stock");
            Path dbFile = appDir.resolve("style_stock.db");

            // Hacer backup del archivo actual antes de restaurar
            if (Files.exists(dbFile)) {
                Path tempBackup = appDir.resolve("style_stock_pre_restore.db");
                Files.copy(dbFile, tempBackup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            Files.copy(backupFile, dbFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            logger.info("Backup restaurado exitosamente desde: {}", backupFile);
            return true;

        } catch (IOException e) {
            logger.error("Error restaurando backup", e);
            return false;
        }
    }

    /**
     * Limpia backups antiguos, manteniendo solo los N más recientes
     */
    private static void limpiarBackupsAntiguos(Path backupDir, int mantener) {
        try {
            var backups = Files.list(backupDir)
                .filter(p -> p.getFileName().toString().endsWith(".db"))
                .sorted((p1, p2) -> {
                    try {
                        return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1));
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .toList();

            if (backups.size() > mantener) {
                for (int i = mantener; i < backups.size(); i++) {
                    Files.deleteIfExists(backups.get(i));
                    logger.debug("Backup antiguo eliminado: {}", backups.get(i));
                }
            }

        } catch (IOException e) {
            logger.error("Error limpiando backups antiguos", e);
        }
    }
}

