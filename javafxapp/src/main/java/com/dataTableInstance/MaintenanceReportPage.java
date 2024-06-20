package com.dataTableInstance;

import com.example.AuthService;
import com.example.Dashboard;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class MaintenanceReportPage {

    private String currentUserPhoneNumber;
    private String easyCardNumber;

    public MaintenanceReportPage(String currentUserPhoneNumber, String easyCardNumber) {
        this.currentUserPhoneNumber = currentUserPhoneNumber;
        this.easyCardNumber = easyCardNumber;
    }

    public void showMaintenanceReportPage(Stage primaryStage) {
        primaryStage.setTitle("維修通報");
        
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(8);
        grid.setHgap(10);

        // 設置列的最小寬度
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(120);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        Label stationIdLabel = new Label("站點ID:");
        GridPane.setConstraints(stationIdLabel, 0, 0);
        ComboBox<Integer> stationIdDropdown = new ComboBox<>();
        GridPane.setConstraints(stationIdDropdown, 1, 0);

        Label dockLabel = new Label("車柱編號:");
        GridPane.setConstraints(dockLabel, 0, 1);
        TextField dockInput = new TextField();
        GridPane.setConstraints(dockInput, 1, 1);

        Label bikeNumberLabel = new Label("車號:");
        GridPane.setConstraints(bikeNumberLabel, 0, 2);
        TextField bikeNumberInput = new TextField();
        GridPane.setConstraints(bikeNumberInput, 1, 2);

        Label maintenanceLabel = new Label("維修和回報紀錄:");
        GridPane.setConstraints(maintenanceLabel, 0, 3);
        TextArea maintenanceInput = new TextArea();
        GridPane.setConstraints(maintenanceInput, 1, 3);

        Label maintenanceItemsLabel = new Label("維修項目:");
        GridPane.setConstraints(maintenanceItemsLabel, 0, 4);
        TextArea maintenanceItemsInput = new TextArea();
        GridPane.setConstraints(maintenanceItemsInput, 1, 4);

        Button submitButton = new Button("提交");
        submitButton.setOnAction(e -> {
            int stationId = stationIdDropdown.getValue();
            int dockNumber = Integer.parseInt(dockInput.getText());
            String bikeNumber = bikeNumberInput.getText();
            String report = maintenanceInput.getText();
            String maintenanceItems = maintenanceItemsInput.getText();
            AuthService authService = new AuthService();
            authService.reportMaintenance(stationId, dockNumber, bikeNumber, report, maintenanceItems);
        });
        GridPane.setConstraints(submitButton, 1, 5);

        // 新增返回按鈕
        Button backButton = new Button("返回主頁面");
        backButton.setOnAction(e -> {
            primaryStage.close();
            new Dashboard(currentUserPhoneNumber, easyCardNumber).start(new Stage());
        });
        GridPane.setConstraints(backButton, 0, 5);

        grid.getChildren().addAll(stationIdLabel, stationIdDropdown, dockLabel, dockInput, bikeNumberLabel, bikeNumberInput, maintenanceLabel, maintenanceInput, maintenanceItemsLabel, maintenanceItemsInput, submitButton, backButton);

        Scene scene = new Scene(grid, 400, 400);
        primaryStage.setScene(scene);
        primaryStage.show();

        // 加載站點ID到下拉選單
        new Thread(() -> {
            AuthService authService = new AuthService();
            try {
                List<Integer> stationIds = authService.fetchStationIds();
                Platform.runLater(() -> stationIdDropdown.getItems().addAll(stationIds));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
