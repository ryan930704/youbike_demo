package com.example;

import com.dataTableInstance.StationQuery; 
import com.dataTableInstance.RentalRecords;
import com.dataTableInstance.EasyCardTopUp;
import com.dataTableInstance.MaintenanceReportPage;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage; 

public class Dashboard {

    private String currentUserPhoneNumber;

    public Dashboard(String currentUserPhoneNumber) {
        this.currentUserPhoneNumber = currentUserPhoneNumber;
    }

    public void start(@SuppressWarnings("exports") Stage primaryStage) {
        primaryStage.setTitle("Dashboard");

        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.setHgap(10);
        grid.setPadding(new Insets(20, 20, 20, 20));
        
        // 設置列的寬度比例
        grid.getColumnConstraints().add(createColumnConstraint());
        grid.getColumnConstraints().add(createColumnConstraint());
        grid.getColumnConstraints().add(createColumnConstraint());

        Button stationQueryButton = new Button("站位查詢");
        stationQueryButton.setOnAction(e -> showStationQueryPage(primaryStage));

        Button rentalRecordButton = new Button("租還車紀錄查詢");
        rentalRecordButton.setOnAction(e -> showRentalRecordsPage(primaryStage));

        Button easyCardTopUpButton = new Button("悠遊卡儲值");
        easyCardTopUpButton.setOnAction(e -> showEasyCardTopUpPage(primaryStage));

        Button rentBikeButton = new Button("租車");
        rentBikeButton.setOnAction(e -> showRentBikePage(primaryStage));

        Button returnBikeButton = new Button("還車");
        returnBikeButton.setOnAction(e -> showReturnBikePage(primaryStage));

        Button maintenanceReportButton = new Button("維修通報");
        maintenanceReportButton.setOnAction(e -> showMaintenanceReportPage(primaryStage));
        
        Button logoutButton = new Button("登出");
        logoutButton.setOnAction(e -> logout(primaryStage));
        
        stationQueryButton.setMaxWidth(Double.MAX_VALUE);
        rentalRecordButton.setMaxWidth(Double.MAX_VALUE);
        easyCardTopUpButton.setMaxWidth(Double.MAX_VALUE);
        rentBikeButton.setMaxWidth(Double.MAX_VALUE);
        returnBikeButton.setMaxWidth(Double.MAX_VALUE);
        maintenanceReportButton.setMaxWidth(Double.MAX_VALUE);
        logoutButton.setMaxWidth(Double.MAX_VALUE);
        
        stationQueryButton.setMinWidth(100);
        rentalRecordButton.setMinWidth(100);
        easyCardTopUpButton.setMinWidth(100);
        rentBikeButton.setMinWidth(100);
        returnBikeButton.setMinWidth(100);
        maintenanceReportButton.setMinWidth(100);
        logoutButton.setMinWidth(100);

        GridPane.setConstraints(logoutButton, 2, 0); // 將登出按鈕放在最右上角

        grid.add(stationQueryButton, 0, 1);
        grid.add(rentalRecordButton, 0, 2);
        grid.add(easyCardTopUpButton, 0, 3);
        grid.add(rentBikeButton, 1, 1);
        grid.add(returnBikeButton, 1, 2);
        grid.add(maintenanceReportButton, 1, 3);
        grid.add(logoutButton, 2, 0);
        
        GridPane.setMargin(stationQueryButton, new Insets(10));
        GridPane.setMargin(rentalRecordButton, new Insets(10));
        GridPane.setMargin(easyCardTopUpButton, new Insets(10));
        GridPane.setMargin(rentBikeButton, new Insets(10));
        GridPane.setMargin(returnBikeButton, new Insets(10));
        GridPane.setMargin(maintenanceReportButton, new Insets(10));
        GridPane.setMargin(logoutButton, new Insets(10));

        Scene scene = new Scene(grid, 400, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private ColumnConstraints createColumnConstraint() {
        ColumnConstraints column = new ColumnConstraints();
        column.setHgrow(Priority.ALWAYS);
        column.setFillWidth(true);
        return column;
    }
    
    private void showStationQueryPage(Stage primaryStage) {
        StationQuery stationQuery = new StationQuery(currentUserPhoneNumber);
        stationQuery.start(primaryStage);
    }

    private void showRentalRecordsPage(Stage primaryStage) {
        RentalRecords rentalRecords = new RentalRecords(currentUserPhoneNumber);
        rentalRecords.start(primaryStage);
    }

    private void showEasyCardTopUpPage(Stage primaryStage) {
        EasyCardTopUp easyCardTopUpPage = new EasyCardTopUp(currentUserPhoneNumber);
        easyCardTopUpPage.start(primaryStage);
    }

    private void showRentBikePage(Stage primaryStage) {
        // TODO: 打開租車頁面
        System.out.println("租車頁面");
    }

    private void showReturnBikePage(Stage primaryStage) {
        // TODO: 打開還車頁面
        System.out.println("還車頁面");
    }

    private void showMaintenanceReportPage(Stage primaryStage) {
        MaintenanceReportPage maintenanceReportPage = new MaintenanceReportPage(currentUserPhoneNumber);
        maintenanceReportPage.showMaintenanceReportPage(primaryStage);
    }
    
    private void logout(Stage primaryStage) {
        primaryStage.close();
        Platform.runLater(() -> {
            Main main = new Main();
            Stage newStage = new Stage();
            main.start(newStage);
        });
    }
}
