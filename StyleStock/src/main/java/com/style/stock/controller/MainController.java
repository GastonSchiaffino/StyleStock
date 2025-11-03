package com.style.stock.controller;

import com.style.stock.util.AppConfig;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ChoiceBox;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML private ChoiceBox<String> themeChoice;
    @FXML private StackPane mainContent;

    @FXML
    public void initialize() throws IOException {
        configurarTemas();
        loadView("/fxml/venta-view.fxml"); // Vista inicial
    }

    private void configurarTemas() {
        themeChoice.getItems().addAll("Light", "Dark");
        String t = AppConfig.get("theme", "light");
        themeChoice.setValue(t.equals("dark") ? "Dark" : "Light");

        themeChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            String theme = newV.equals("Dark") ? "dark" : "light";
            AppConfig.set("theme", theme);

            Stage stage = (Stage) themeChoice.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.getStylesheets().clear();

            String cssFile = theme.equals("dark") ? "/css/dark.css" : "/css/light.css";
            scene.getStylesheets().add(getClass().getResource(cssFile).toExternalForm());

            logger.info("Tema cambiado a: {}", theme);
        });
    }

    @FXML
    private void showVentas() throws IOException {
        loadView("/fxml/venta-view.fxml");
    }

    @FXML
    private void showHistorialVentas() throws IOException {
        loadView("/fxml/historial-ventas-view.fxml");
    }

    @FXML
    private void showClientes() throws IOException {
        loadView("/fxml/cliente-view.fxml");
    }

    // ============================================
    // CAMBIADO: Ahora carga la vista UNIFICADA
    // ============================================
    @FXML
    private void showProductos() throws IOException {
        loadView("/fxml/producto-variante-unificado.fxml");
    }

    @FXML
    private void showStock() throws IOException {
        loadView("/fxml/stock-view.fxml");
    }

    @FXML
    private void showCategorias() throws IOException {
        loadView("/fxml/categoria-view.fxml"); // CAMBIADO: Vista mejorada
    }

    @FXML
    private void showConfiguracion() throws IOException {
        loadView("/fxml/configuracion-view.fxml");
    }

    @FXML
    private void showReportes() throws IOException {
        loadView("/fxml/reportes-view.fxml");
    }

    private void loadView(String fxmlPath) throws IOException {
        logger.debug("Cargando vista: {}", fxmlPath);
        Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
        mainContent.getChildren().setAll(view);
    }
}