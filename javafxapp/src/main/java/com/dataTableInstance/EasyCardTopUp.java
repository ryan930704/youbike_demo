package com.dataTableInstance;

import com.example.AuthService;
import com.example.Dashboard;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.IOException;

public class EasyCardTopUp {

    private final AuthService authService = new AuthService();
    private String currentUserPhoneNumber;
    private String easyCardNumber;

    public EasyCardTopUp(String currentUserPhoneNumber, String easyCardNumber) {
        this.currentUserPhoneNumber = currentUserPhoneNumber;
        this.easyCardNumber = easyCardNumber;
    }

    public void start(Stage primaryStage) {
        primaryStage.setTitle("悠遊卡儲值");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20, 20, 20, 20));
        grid.setVgap(10);
        grid.setHgap(10);

        // EasyCard Number Field
        Label easyCardLabel = new Label("悠遊卡卡號:");
        GridPane.setConstraints(easyCardLabel, 0, 0);
        TextField easyCardInput = new TextField(easyCardNumber);
        easyCardInput.setDisable(true);
        GridPane.setConstraints(easyCardInput, 1, 0);

        // Balance Field
        Label balanceLabel = new Label("餘額:");
        GridPane.setConstraints(balanceLabel, 0, 1);
        TextField balanceInput = new TextField();
        balanceInput.setDisable(true);
        GridPane.setConstraints(balanceInput, 1, 1);

        // TopUp Amount Field
        Label topUpLabel = new Label("儲值金額:");
        GridPane.setConstraints(topUpLabel, 0, 2);
        TextField topUpInput = new TextField();
        GridPane.setConstraints(topUpInput, 1, 2);

        // Update Button
        Button updateButton = new Button("儲值!");
        GridPane.setConstraints(updateButton, 1, 3);

        // 新增返回按鈕
        Button backButton = new Button("返回主頁面");
        backButton.setOnAction(e -> {
            primaryStage.close();
            new Dashboard(currentUserPhoneNumber, easyCardNumber).start(new Stage());
        });
        GridPane.setConstraints(backButton, 0, 4);

        grid.getChildren().addAll(easyCardLabel, easyCardInput, balanceLabel, balanceInput, topUpLabel, topUpInput, updateButton, backButton);

        Scene scene = new Scene(grid, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Load EasyCard Information
        new Thread(() -> {
            try {
                JSONObject easyCardInfo = authService.getEasyCardInfo(easyCardNumber);
                Platform.runLater(() -> balanceInput.setText(String.valueOf(easyCardInfo.getDouble("balance"))));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        updateButton.setOnAction(e -> {
            double topUpAmount = Double.parseDouble(topUpInput.getText());
            new Thread(() -> {
                try {
                    authService.topUpEasyCard(easyCardNumber, topUpAmount);
                    JSONObject easyCardInfo = authService.getEasyCardInfo(easyCardNumber);
                    Platform.runLater(() -> balanceInput.setText(String.valueOf(easyCardInfo.getDouble("balance"))));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }).start();
        });
    }
}
