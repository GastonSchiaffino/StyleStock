package com.style.stock;

import com.style.stock.util.AppConfig;
import com.style.stock.util.DBInit;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // initialize DB and config directory
        DBInit.ensureDatabase();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
        Scene scene = new Scene(loader.load());
        // apply theme from config
        String theme = AppConfig.get("theme", "light");
        scene.getStylesheets().add(getClass().getResource(theme.equals("dark") ? "/css/dark.css" : "/css/light.css").toExternalForm());
        Image icon = new Image("file:src/main/resources/icons/Style_Icon32X32.png");
        stage.getIcons().add(icon);
        stage.setTitle("Style Stock");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
