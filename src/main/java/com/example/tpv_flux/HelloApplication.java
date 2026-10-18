package com.example.tpv_flux;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        FXMLLoader fxmlLoader = new FXMLLoader(
                HelloApplication.class.getResource("/com/example/tpv_flux/views/layout/MainLayout.fxml")
        );
        Scene scene = new Scene(fxmlLoader.load(), 1280, 800);
        stage.setTitle("TPV Core — Accesorios de cómputo y celulares");
        stage.setMinWidth(1080);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }
}
