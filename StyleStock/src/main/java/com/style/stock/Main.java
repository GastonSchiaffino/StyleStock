// ============================================
// Main.java
// ============================================
package com.style.stock;

import com.style.stock.database.DatabaseManager;
import com.style.stock.util.AppConfig;
import com.style.stock.util.BackupUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.time.LocalDate;

public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage stage) {
        try {
            logger.info("=== Iniciando StyleStock v2.0 ===");
            
            DatabaseManager dbManager = DatabaseManager.getInstance();
            if (!dbManager.isHealthy()) {
                logger.error("La base de datos no está disponible");
                mostrarErrorYCerrar("Error crítico", "No se pudo conectar a la base de datos");
                return;
            }
            logger.info("Base de datos inicializada correctamente");

            realizarBackupAutomatico();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
            Scene scene = new Scene(loader.load());
            
            aplicarTema(scene);
            configurarIcono(stage);
            
            stage.setTitle("StyleStock v2.0 - Sistema de Gestión de Ventas");
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setMinWidth(1280);
            stage.setMinHeight(768);
            
            stage.setOnCloseRequest(event -> {
                logger.info("Cerrando aplicación...");
                DatabaseManager.getInstance().shutdown();
                logger.info("=== StyleStock cerrado ===");
            });
            
            stage.show();
            logger.info("Interfaz cargada exitosamente");

        } catch (Exception e) {
            logger.error("Error iniciando aplicación", e);
            mostrarErrorYCerrar("Error al iniciar", "No se pudo iniciar la aplicación: " + e.getMessage());
        }
    }

    private void aplicarTema(Scene scene) {
        try {
            String theme = AppConfig.get("theme", "light");
            String cssFile = theme.equals("dark") ? "/css/dark.css" : "/css/light.css";
            String cssUrl = getClass().getResource(cssFile).toExternalForm();
            scene.getStylesheets().add(cssUrl);
            logger.debug("Tema aplicado: {}", theme);
        } catch (Exception e) {
            logger.warn("No se pudo cargar el tema CSS", e);
        }
    }

    private void configurarIcono(Stage stage) {
        try {
            InputStream iconStream = getClass().getResourceAsStream("/icons/Style_Icon32X32.png");
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
            } else {
                logger.warn("Ícono de aplicación no encontrado");
            }
        } catch (Exception e) {
            logger.warn("Error configurando ícono", e);
        }
    }

    private void realizarBackupAutomatico() {
        try {
            boolean backupHabilitado = AppConfig.getBoolean("backup_auto", true);
            if (!backupHabilitado) return;

            String ultimoBackupStr = AppConfig.get("ultimo_backup");
            int diasBackup = AppConfig.getInt("backup_dias", 7);

            if (ultimoBackupStr == null || ultimoBackupStr.isEmpty()) {
                realizarBackup();
            } else {
                LocalDate ultimoBackup = LocalDate.parse(ultimoBackupStr);
                LocalDate hoy = LocalDate.now();
                
                if (ultimoBackup.plusDays(diasBackup).isBefore(hoy) || ultimoBackup.isEqual(hoy)) {
                    realizarBackup();
                }
            }
        } catch (Exception e) {
            logger.warn("Error en backup automático", e);
        }
    }

    private void realizarBackup() {
        new Thread(() -> {
            logger.info("Realizando backup automático...");
            if (BackupUtils.realizarBackup()) {
                AppConfig.set("ultimo_backup", LocalDate.now().toString());
                logger.info("Backup automático completado");
            } else {
                logger.error("Error en backup automático");
            }
        }).start();
    }

    private void mostrarErrorYCerrar(String titulo, String mensaje) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR
            );
            alert.setTitle(titulo);
            alert.setHeaderText(mensaje);
            alert.showAndWait();
            Platform.exit();
        });
    }

    @Override
    public void stop() {
        logger.info("Deteniendo aplicación...");
        DatabaseManager.getInstance().shutdown();
    }

    public static void main(String[] args) {
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("prism.text", "t2k");
        
        logger.info("Lanzando aplicación...");
        launch(args);
    }
}