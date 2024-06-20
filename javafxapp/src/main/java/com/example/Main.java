package com.example;

import java.io.IOException;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.application.Platform;

public class Main extends Application {

    private final AuthService authService = new AuthService();
    private String currentUserPhoneNumber;
    private String easyCardNumber;

    @Override
    public void start(@SuppressWarnings("exports") Stage primaryStage) {
        primaryStage.setTitle("Supabase Authentication");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(8);
        grid.setHgap(10);

        // Phone Number Field
        Label phoneLabel = new Label("Phone Number:");
        GridPane.setConstraints(phoneLabel, 0, 0);
        TextField phoneInput = new TextField("0977334567");
        GridPane.setConstraints(phoneInput, 1, 0);

        // Password Field
        Label passwordLabel = new Label("Password:");
        GridPane.setConstraints(passwordLabel, 0, 1);
        PasswordField passwordInput = new PasswordField();
        GridPane.setConstraints(passwordInput, 1, 1);

        // ID Number Field
        Label idLabel = new Label("ID Number:");
        GridPane.setConstraints(idLabel, 0, 2);
        TextField idInput = new TextField();
        GridPane.setConstraints(idInput, 1, 2);

        // Email Field
        Label emailLabel = new Label("Email:");
        GridPane.setConstraints(emailLabel, 0, 3);
        TextField emailInput = new TextField();
        GridPane.setConstraints(emailInput, 1, 3);

        // EasyCard Number Field
        Label easyCardLabel = new Label("EasyCard Number:");
        GridPane.setConstraints(easyCardLabel, 0, 4);
        TextField easyCardInput = new TextField();
        GridPane.setConstraints(easyCardInput, 1, 4);

        // Toggle Buttons for Register and Login
        ToggleGroup toggleGroup = new ToggleGroup();
        RadioButton registerToggle = new RadioButton("Register");
        registerToggle.setToggleGroup(toggleGroup);
        registerToggle.setSelected(true);
        GridPane.setConstraints(registerToggle, 0, 5);

        RadioButton loginToggle = new RadioButton("Login");
        loginToggle.setToggleGroup(toggleGroup);
        GridPane.setConstraints(loginToggle, 1, 5);

        // Register Button
        Button registerButton = new Button("Register");
        registerButton.setOnAction(e -> authService.register(
                phoneInput.getText(),
                passwordInput.getText(),
                idInput.getText(),
                emailInput.getText(),
                easyCardInput.getText()
        ));
        GridPane.setConstraints(registerButton, 0, 6);

        // Login Button
        Button loginButton = new Button("Login");
        loginButton.setOnAction(e -> {
            authService.login(phoneInput.getText(), passwordInput.getText(), new AuthService.LoginCallback() {
                @Override
                public void onSuccess() {
                    currentUserPhoneNumber = phoneInput.getText();
                    try {
                        easyCardNumber = authService.getEasyCardNumber(currentUserPhoneNumber);
                        showDashboard(primaryStage);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }

                @Override
                public void onFailure() {
                    System.out.println("Login failed.");
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Login Failed");
                        alert.setHeaderText(null);
                        alert.setContentText("Invalid phone number or password. Please try again.");
                        alert.showAndWait();
                    });
                }
            });
        });
        GridPane.setConstraints(loginButton, 1, 6);

        grid.getChildren().addAll(
                phoneLabel, phoneInput, passwordLabel, passwordInput,
                idLabel, idInput, emailLabel, emailInput,
                easyCardLabel, easyCardInput, registerToggle, loginToggle, registerButton, loginButton
        );

        // Add listeners to toggle buttons
        registerToggle.setOnAction(e -> {
            idLabel.setVisible(true);
            idInput.setVisible(true);
            emailLabel.setVisible(true);
            emailInput.setVisible(true);
            easyCardLabel.setVisible(true);
            easyCardInput.setVisible(true);
            registerButton.setVisible(true);
            loginButton.setVisible(false);
        });

        loginToggle.setOnAction(e -> {
            idLabel.setVisible(false);
            idInput.setVisible(false);
            emailLabel.setVisible(false);
            emailInput.setVisible(false);
            easyCardLabel.setVisible(false);
            easyCardInput.setVisible(false);
            registerButton.setVisible(false);
            loginButton.setVisible(true);
        });

        // Initial state
        idLabel.setVisible(true);
        idInput.setVisible(true);
        emailLabel.setVisible(true);
        emailInput.setVisible(true);
        easyCardLabel.setVisible(true);
        easyCardInput.setVisible(true);
        registerButton.setVisible(true);
        loginButton.setVisible(false);

        Scene scene = new Scene(grid, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showDashboard(Stage primaryStage) {
        Platform.runLater(() -> {
            Dashboard dashboard = new Dashboard(currentUserPhoneNumber, easyCardNumber);
            dashboard.start(primaryStage);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
