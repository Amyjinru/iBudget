package com.accounting.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) {
        Label label = new Label("Accounting App");
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 640, 400);
        stage.setTitle("Accounting App");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
