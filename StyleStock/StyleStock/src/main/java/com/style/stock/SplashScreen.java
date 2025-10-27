package com.style.stock;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class SplashScreen extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Imagen del logo
        ImageView logo = new ImageView(new Image("file:src/main/resources/icons/StyleSR_Stock_Logo.png"));
        logo.setFitWidth(300);
        logo.setPreserveRatio(true);

        StackPane root = new StackPane(logo);
        Scene scene = new Scene(root, 600, 400);

        Stage splashStage = new Stage(StageStyle.UNDECORATED);
        Image icon = new Image("file:src/main/resources/icons/Style_Icon32X32.png");
        splashStage.getIcons().add(icon);
        splashStage.setScene(scene);
        splashStage.show();

        // Fade in
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), logo);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // Pausa y fade out
        fadeIn.setOnFinished(e -> {
            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(ev -> {
                FadeTransition fadeOut = new FadeTransition(Duration.seconds(1.5), logo);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(ev2 -> {
                    splashStage.close();
                    openMainApp();
                });
                fadeOut.play();
            });
            pause.play();
        });
    }

    private void openMainApp() {
        try {
            Main mainApp = new Main();
            Stage mainStage = new Stage();
            mainApp.start(mainStage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
