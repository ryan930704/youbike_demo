package com.dataTableInstance;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.json.JSONObject;
import com.example.AuthService;
import com.example.Dashboard;

import java.io.IOException;
import java.util.List;

public class RentalRecords {

	private final AuthService authService = new AuthService();
	private TableView<JSONObject> table;
	private ObservableList<JSONObject> data;
	private int currentPage = 0;
	private final int pageSize = 50;
	private Label pageInfoLabel;
	private int totalDataCount = 0;
    private String currentUserPhoneNumber;
	
    public RentalRecords(String currentUserPhoneNumber) {
        this.currentUserPhoneNumber = currentUserPhoneNumber;
    }

	@SuppressWarnings("unchecked")
	public void start(Stage primaryStage) {
		primaryStage.setTitle("租還車紀錄查詢");

		GridPane grid = new GridPane();
		grid.setVgap(10);
		grid.setHgap(10);
		grid.setPadding(new Insets(20, 20, 20, 20));

		Button fetchDataButton = new Button("Fetch Data");
		fetchDataButton.setOnAction(e -> {
			currentPage = 0;
			fetchData(currentPage * pageSize, (currentPage + 1) * pageSize - 1);
		});

		Button nextPageButton = new Button("下一頁");
		nextPageButton.setOnAction(e -> {
			if ((currentPage + 1) * pageSize < totalDataCount) {
				currentPage++;
				fetchData(currentPage * pageSize, (currentPage + 1) * pageSize - 1);
			}
		});

		Button prevPageButton = new Button("上一頁");
		prevPageButton.setOnAction(e -> {
			if (currentPage > 0) {
				currentPage--;
				fetchData(currentPage * pageSize, (currentPage + 1) * pageSize - 1);
			}
		});

		pageInfoLabel = new Label("第 1 頁 / 總共 1 頁");

		HBox buttonBox = new HBox(10);
		buttonBox.getChildren().addAll(prevPageButton, nextPageButton, pageInfoLabel);
		buttonBox.setSpacing(10);
		buttonBox.setAlignment(Pos.CENTER);

		grid.add(fetchDataButton, 0, 0);
		grid.add(buttonBox, 0, 1, 2, 1);

		table = new TableView<>();
		data = FXCollections.observableArrayList();
		table.setItems(data);

		TableColumn<JSONObject, String> rentalTimeCol = new TableColumn<>("時間");
		rentalTimeCol
				.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().optString("rental_time", "N/A")));

		TableColumn<JSONObject, String> rentalStationCol = new TableColumn<>("起點");
		rentalStationCol.setCellValueFactory(
				cellData -> new SimpleStringProperty(cellData.getValue().optString("rental_station", "N/A")));

		TableColumn<JSONObject, String> returnStationCol = new TableColumn<>("終點");
		returnStationCol.setCellValueFactory(
				cellData -> new SimpleStringProperty(cellData.getValue().optString("return_station", "N/A")));

		TableColumn<JSONObject, String> bikeNumberCol = new TableColumn<>("車號");
		bikeNumberCol
				.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().optString("bike_number", "N/A")));

		TableColumn<JSONObject, String> amountCol = new TableColumn<>("金額");
		amountCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().optString("amount", "N/A")));

		table.getColumns().addAll(rentalTimeCol, rentalStationCol, returnStationCol, bikeNumberCol, amountCol);

		grid.add(table, 0, 2, 2, 1);
		
		// 新增返回按鈕
		Button backButton = new Button("返回主頁面");
		backButton.setOnAction(e -> {
			primaryStage.close();
			new Dashboard(currentUserPhoneNumber).start(new Stage());
		});

		grid.add(backButton, 0, 5);  

		Scene scene = new Scene(grid, 1000, 600);
		primaryStage.setScene(scene);
		primaryStage.show();

		fetchData(currentPage * pageSize, (currentPage + 1) * pageSize - 1);
	}

	private void fetchData(int start, int end) {
		new Thread(() -> {
			try {
				totalDataCount = authService.fetchTotalRentalRecordsCount();
				List<JSONObject> fetchedData = authService.fetchRentalRecords(start, end);
				Platform.runLater(() -> {
					data.clear();
					data.addAll(fetchedData);
					table.scrollTo(0);
					updatePageInfo();
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}

	private void updatePageInfo() {
		int totalPages = (totalDataCount + pageSize - 1) / pageSize;
		pageInfoLabel.setText(String.format("第 %d 頁 / 總共 %d 頁，共 %d 筆資料", currentPage + 1, totalPages, totalDataCount));
	}
}
