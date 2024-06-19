package com.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.application.Platform;

public class Main extends Application {

    private final AuthService authService = new AuthService();

    @Override
    public void start(@SuppressWarnings("exports") Stage primaryStage) {
        primaryStage.setTitle("Supabase Authentication");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(8);
        grid.setHgap(10);

        // Username and Password Fields
        Label usernameLabel = new Label("Username:");
        GridPane.setConstraints(usernameLabel, 0, 0);
        TextField usernameInput = new TextField("test@hotmail.com"); 
        GridPane.setConstraints(usernameInput, 1, 0);

        Label passwordLabel = new Label("Password:");
        GridPane.setConstraints(passwordLabel, 0, 1);
        PasswordField passwordInput = new PasswordField();
        GridPane.setConstraints(passwordInput, 1, 1);

        // Register Button
        Button registerButton = new Button("Register");
        registerButton.setOnAction(e -> authService.register(usernameInput.getText(), passwordInput.getText()));
        GridPane.setConstraints(registerButton, 0, 2);

        // Login Button
        Button loginButton = new Button("Login");
        loginButton.setOnAction(e -> authService.login(usernameInput.getText(), passwordInput.getText(), new AuthService.LoginCallback() {
            @Override
            public void onSuccess() {
                showDashboard(primaryStage);
            }

            @Override
            public void onFailure() {
                System.out.println("Login failed.");
                // Handle login failure
            }
        }));
        GridPane.setConstraints(loginButton, 1, 2);

        grid.getChildren().addAll(usernameLabel, usernameInput, passwordLabel, passwordInput, registerButton, loginButton);

        Scene scene = new Scene(grid, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showDashboard(Stage primaryStage) {
        Platform.runLater(() -> {
            Dashboard dashboard = new Dashboard();
            dashboard.start(primaryStage);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
