package com.style.stock.controller;

import com.style.stock.util.AppConfig;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ChoiceBox;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class MainController {
    @FXML private ChoiceBox<String> themeChoice;
    @FXML private StackPane mainContent;

    @FXML
    private void showClientes() throws IOException {
        loadView("/fxml/cliente-view.fxml");
    }

    @FXML
    private void showProductos() throws IOException {
        loadView("/fxml/producto-view.fxml");
    }

    @FXML
    private void showFacturas() throws IOException {
        loadView("/fxml/factura-view.fxml");
    }

    private void loadView(String fxmlPath) throws IOException {
        Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
        mainContent.getChildren().setAll(view);
    }

    @FXML
    public void initialize() throws IOException {
        themeChoice.getItems().addAll("Light", "Dark");
        String t = AppConfig.get("theme", "light");
        themeChoice.setValue(t.equals("dark") ? "Dark" : "Light");
        themeChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            String theme = newV.equals("Dark") ? "dark" : "light";
            AppConfig.set("theme", theme);
            // Reload stage stylesheet - simple approach
            Stage stage = (Stage) themeChoice.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource(theme.equals("dark") ? "/css/dark.css" : "/css/light.css").toExternalForm());
        });

        loadView("/fxml/producto-view.fxml");
    }
}
