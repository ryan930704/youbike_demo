package com.dataTableInstance;

import com.example.AuthService;
import com.example.Dashboard;

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

import java.io.IOException;
import java.util.List;

public class StationQuery {

	private final AuthService authService = new AuthService();
	private TableView<JSONObject> table;
	private ObservableList<JSONObject> data;
	private ComboBox<String> cityDropdown;
	private ComboBox<String> bikeTypeDropdown;
	private int currentPage = 0;
	private final int pageSize = 50;
	private Label pageInfoLabel;
	private int totalDataCount = 0;
    private String currentUserPhoneNumber;
    private String easyCardNumber;
	
    public StationQuery(String currentUserPhoneNumber, String easyCardNumber) {
        this.currentUserPhoneNumber = currentUserPhoneNumber;
        this.easyCardNumber = easyCardNumber;
    }

	/**
	 * 初始化和顯示主界面
	 * 
	 * 設置 GridPane 佈局，創建下拉選單和按鈕，設置按鈕事件處理器 創建並配置 TableView 顯示數據 初始加載城市下拉式選單數據
	 * 
	 * @param primaryStage JavaFX 的 Stage 物件
	 */
	@SuppressWarnings("unchecked")
	public void start(Stage primaryStage) {
		primaryStage.setTitle("站位查詢");

		GridPane grid = new GridPane();
		grid.setVgap(10);
		grid.setHgap(10);
		grid.setPadding(new Insets(20, 20, 20, 20));

		cityDropdown = new ComboBox<>();
		bikeTypeDropdown = new ComboBox<>(FXCollections.observableArrayList("1.0", "2.0"));
		bikeTypeDropdown.setValue("1.0");

		Button fetchDataButton = new Button("Fetch Data");
		fetchDataButton.setOnAction(e -> {
			currentPage = 0;
			String city = cityDropdown.getValue();
			String bikeType = bikeTypeDropdown.getValue();
			fetchData(city, bikeType, currentPage * pageSize, (currentPage + 1) * pageSize - 1);
		});

		Button nextPageButton = new Button("下一頁");
		nextPageButton.setOnAction(e -> {
			if ((currentPage + 1) * pageSize < totalDataCount) {
				currentPage++;
				String city = cityDropdown.getValue();
				String bikeType = bikeTypeDropdown.getValue();
				fetchData(city, bikeType, currentPage * pageSize, (currentPage + 1) * pageSize - 1);
			}
		});

		Button prevPageButton = new Button("上一頁");
		prevPageButton.setOnAction(e -> {
			if (currentPage > 0) {
				currentPage--;
				String city = cityDropdown.getValue();
				String bikeType = bikeTypeDropdown.getValue();
				fetchData(city, bikeType, currentPage * pageSize, (currentPage + 1) * pageSize - 1);
			}
		});

		pageInfoLabel = new Label("第 1 頁 / 總共 1 頁");

		bikeTypeDropdown.setOnAction(e -> updateCityDropdown(cityDropdown, bikeTypeDropdown.getValue()));

		HBox buttonBox = new HBox(10);
		buttonBox.getChildren().addAll(prevPageButton, nextPageButton, pageInfoLabel);
		buttonBox.setSpacing(10);
		buttonBox.setAlignment(Pos.CENTER);

		grid.add(new Label("City:"), 0, 0);
		grid.add(cityDropdown, 1, 0);
		grid.add(new Label("Bike Type:"), 0, 1);
		grid.add(bikeTypeDropdown, 1, 1);
		grid.add(fetchDataButton, 0, 2);
		grid.add(buttonBox, 0, 3, 2, 1);

		table = new TableView<>();
		data = FXCollections.observableArrayList();
		table.setItems(data);

		TableColumn<JSONObject, String> cityCol = new TableColumn<>("縣市");
		cityCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().optString("city", "N/A")));

		TableColumn<JSONObject, String> regionCol = new TableColumn<>("區域");
		regionCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().optString("region", "N/A")));

		TableColumn<JSONObject, String> nameCol = new TableColumn<>("站點名稱");
		nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().optString("name", "N/A")));
		nameCol.setPrefWidth(250);

		TableColumn<JSONObject, String> totalDocksCol = new TableColumn<>("車位數");
		totalDocksCol
				.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().optString("total_docks", "N/A")));

		TableColumn<JSONObject, String> availableBikesCol = new TableColumn<>("可借車輛");
		availableBikesCol.setCellValueFactory(
				cellData -> new SimpleStringProperty(cellData.getValue().optString("available_bikes", "N/A")));

		TableColumn<JSONObject, String> availableDocksCol = new TableColumn<>("可停空位");
		availableDocksCol.setCellValueFactory(
				cellData -> new SimpleStringProperty(cellData.getValue().optString("available_docks", "N/A")));

		TableColumn<JSONObject, String> bikeTypeCol = new TableColumn<>("車種版本，1.0 或 2.0");
		bikeTypeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().optString("bike_type", "N/A")));

		table.getColumns().addAll(cityCol, regionCol, nameCol, totalDocksCol, availableBikesCol, availableDocksCol, bikeTypeCol);

		grid.add(table, 0, 4, 2, 1);

		// 新增返回按鈕
		Button backButton = new Button("返回主頁面");
		backButton.setOnAction(e -> {
		    primaryStage.close();
		    new Dashboard(currentUserPhoneNumber, easyCardNumber).start(new Stage());
		});

		grid.add(backButton, 0, 5);  

		Scene scene = new Scene(grid, 1000, 600);
		primaryStage.setScene(scene);
		primaryStage.show();

		updateCityDropdown(cityDropdown, bikeTypeDropdown.getValue());
	}

	/**
	 * 根據選擇的車種版本更新城市下拉式選單
	 * 
	 * 從 Server 獲取城市列表並更新下拉式選單 初始化時加載數據
	 * 
	 * @param cityDropdown 城市下拉選單
	 * @param bikeType     選擇的車種版本
	 */
	private void updateCityDropdown(ComboBox<String> cityDropdown, String bikeType) {
		new Thread(() -> {
			try {
				List<String> cities = authService.fetchCities(bikeType);
				Platform.runLater(() -> {
					cityDropdown.getItems().clear();
					cityDropdown.getItems().add("All");
					cityDropdown.getItems().addAll(cities);
					cityDropdown.setValue("All");

					String selectedCity = cityDropdown.getValue();
					fetchData(selectedCity, bikeType, currentPage * pageSize, (currentPage + 1) * pageSize - 1);
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}

	/**
	 * 從 Server 獲取數據並更新 TableView
	 * 
	 * 根據選擇的城市和車種版本，分頁加載數據
	 * 
	 * @param city     選擇的城市
	 * @param bikeType 選擇的車種版本
	 * @param start    數據開始的索引
	 * @param end      數據結束的索引
	 */
	private void fetchData(String city, String bikeType, int start, int end) {
		new Thread(() -> {
			try {
				totalDataCount = authService.fetchTotalDataCount(city, bikeType);
				List<JSONObject> fetchedData = authService.fetchData(city, bikeType, start, end);
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

	/**
	 * 更新頁碼信息
	 * 
	 * 計算總頁數並顯示當前頁碼和總頁數
	 */
	private void updatePageInfo() {
		int totalPages = (totalDataCount + pageSize - 1) / pageSize;
		pageInfoLabel.setText(String.format("第 %d 頁 / 總共 %d 頁，共 %d 筆資料", currentPage + 1, totalPages, totalDataCount));
	}
}
