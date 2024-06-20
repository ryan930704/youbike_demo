package com.example;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javafx.application.Platform;

public class AuthService {

	private static final String SUPABASE_URL;
	private static final String SUPABASE_API_KEY;

	static {
		Properties properties = new Properties();
		try (InputStream input = AuthService.class.getClassLoader().getResourceAsStream("config.properties")) {
			if (input == null) {
				System.out.println("Sorry, unable to find config.properties");
				throw new IOException("config.properties not found");
			}
			properties.load(input);
		} catch (IOException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to load configuration", ex);
		}
		SUPABASE_URL = properties.getProperty("supabase.url");
		SUPABASE_API_KEY = properties.getProperty("supabase.api.key");
	}

	private final OkHttpClient client = new OkHttpClient();

	public interface LoginCallback {
		void onSuccess();

		void onFailure();
	}

	/**
	 * 註冊新用戶
	 * 
	 * @param phoneNumber    用戶的手機號碼
	 * @param password       用戶的密碼
	 * @param idNumber       身分證字號
	 * @param email          電子郵件
	 * @param easyCardNumber 悠遊卡卡號
	 */
	public void register(String phoneNumber, String password, String idNumber, String email, String easyCardNumber) {
		String json = "{\"phone_number\":\"" + phoneNumber + "\",\"password\":\"" + password + "\"," + "\"id_number\":\""
				+ idNumber + "\",\"email\":\"" + email + "\"," + "\"easycard_number\":\"" + easyCardNumber + "\"}";
		RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
		Request request = new Request.Builder().url(SUPABASE_URL + "/rest/v1/members").post(body)
				.addHeader("apikey", SUPABASE_API_KEY).addHeader("Authorization", "Bearer " + SUPABASE_API_KEY).build();

		client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				e.printStackTrace();
				System.out.println("Registration in members table failed.");
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				try (ResponseBody responseBody = response.body()) {
					if (response.isSuccessful()) {
						System.out.println("Registration response: Successful !" + responseBody.string());
						// 插入 easycards 記錄
						insertEasyCardRecord(easyCardNumber);
					} else {
						System.out.println("Registration failed with response: " + responseBody.string());
					}
				}
			}
		});
	}

	/**
	 * 登錄用戶
	 * 
	 * @param phoneNumber 用戶的手機號碼
	 * @param password    用戶的密碼
	 * @param callback    登錄成功或失敗的回調函數
	 */
	public void login(String phoneNumber, String password, LoginCallback callback) {
		HttpUrl.Builder urlBuilder = HttpUrl.parse(SUPABASE_URL + "/rest/v1/members").newBuilder();
		urlBuilder.addQueryParameter("select", "*");
		urlBuilder.addQueryParameter("phone_number", "eq." + phoneNumber);
		urlBuilder.addQueryParameter("password", "eq." + password);

		String url = urlBuilder.build().toString();

		Request request = new Request.Builder().url(url).addHeader("apikey", SUPABASE_API_KEY)
				.addHeader("Authorization", "Bearer " + SUPABASE_API_KEY).build();

		client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				e.printStackTrace();
				System.out.println("Login failed.");
				Platform.runLater(callback::onFailure);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				try (ResponseBody responseBody = response.body()) {
					if (response.isSuccessful()) {
						JSONArray jsonArray = new JSONArray(responseBody.string());
						if (jsonArray.length() > 0) {
							System.out.println("Login successful.");
							Platform.runLater(callback::onSuccess);
						} else {
							System.out.println("Login failed: invalid credentials.");
							Platform.runLater(callback::onFailure);
						}
					} else {
						System.out.println("Login failed with response: " + responseBody.string());
						Platform.runLater(callback::onFailure);
					}
				}
			}
		});
	}

	/**
	 * 獲取符合條件的數據
	 * 
	 * @param city     城市名
	 * @param bikeType 自行車類型
	 * @param start    開始的數據索引
	 * @param end      結束的數據索引
	 * @return 符合條件的數據列表
	 * @throws IOException 如果請求失敗，則拋出異常
	 */
	@SuppressWarnings("exports")
	public List<JSONObject> fetchData(String city, String bikeType, int start, int end) throws IOException {
		HttpUrl.Builder urlBuilder = HttpUrl.parse(SUPABASE_URL + "/rest/v1/stations").newBuilder();
		urlBuilder.addQueryParameter("select", "*");
		urlBuilder.addQueryParameter("bike_type", "eq." + bikeType);
		if (city != null && !city.equals("All")) {
			urlBuilder.addQueryParameter("city", "eq." + city);
		}
		urlBuilder.addQueryParameter("offset", String.valueOf(start));
		urlBuilder.addQueryParameter("limit", String.valueOf(end - start + 1));

		String url = urlBuilder.build().toString();

		Request request = new Request.Builder().url(url).addHeader("apikey", SUPABASE_API_KEY)
				.addHeader("Authorization", "Bearer " + SUPABASE_API_KEY).build();

		try (Response response = client.newCall(request).execute(); ResponseBody responseBody = response.body()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response);
			}

			JSONArray jsonArray = new JSONArray(responseBody.string());
			List<JSONObject> resultList = new ArrayList<>();
			for (int i = 0; i < jsonArray.length(); i++) {
				resultList.add(jsonArray.getJSONObject(i));
			}
			return resultList;
		}
	}

	/**
	 * 獲取所有包含特定自行車類型的城市
	 * 
	 * @param bikeType 自行車類型
	 * @return 包含特定自行車類型的城市列表
	 * @throws IOException 如果請求失敗，則拋出異常
	 */
	public List<String> fetchCities(String bikeType) throws IOException {
		Set<String> cities = new HashSet<>();

		// URL for calling the RPC function
		HttpUrl.Builder urlBuilder = HttpUrl.parse(SUPABASE_URL + "/rest/v1/rpc/distinct_cities").newBuilder();

		String url = urlBuilder.build().toString();

		// JSON body for the RPC request
		String jsonBody = "{\"bike_type\":\"" + bikeType + "\"}";

		RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8"));

		Request request = new Request.Builder().url(url).post(body).addHeader("apikey", SUPABASE_API_KEY)
				.addHeader("Authorization", "Bearer " + SUPABASE_API_KEY).build();

		try (Response response = client.newCall(request).execute(); ResponseBody responseBody = response.body()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response);
			}

			JSONArray jsonArray = new JSONArray(responseBody.string());
			for (int i = 0; i < jsonArray.length(); i++) {
				String city = jsonArray.getJSONObject(i).optString("city");
				if (city != null) {
					cities.add(city);
				}
			}

			return new ArrayList<>(cities);
		}
	}

	/**
	 * 獲取符合條件的數據總數
	 * 
	 * @param city     城市名
	 * @param bikeType 自行車類型
	 * @return 符合條件的數據總數
	 * @throws IOException 如果請求失敗，則拋出異常
	 */
	public int fetchTotalDataCount(String city, String bikeType) throws IOException {
		HttpUrl.Builder urlBuilder = HttpUrl.parse(SUPABASE_URL + "/rest/v1/stations").newBuilder();
		urlBuilder.addQueryParameter("select", "*");
		urlBuilder.addQueryParameter("bike_type", "eq." + bikeType);
		if (city != null && !city.equals("All")) {
			urlBuilder.addQueryParameter("city", "eq." + city);
		}

		String url = urlBuilder.build().toString();

		Request request = new Request.Builder().url(url).addHeader("apikey", SUPABASE_API_KEY)
				.addHeader("Authorization", "Bearer " + SUPABASE_API_KEY).addHeader("Prefer", "count=exact").build();

		try (Response response = client.newCall(request).execute(); ResponseBody responseBody = response.body()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response);
			}

			String countHeader = response.header("Content-Range");
			if (countHeader == null || countHeader.isEmpty()) {
				throw new IOException("Missing Content-Range header in response");
			}

			String[] parts = countHeader.split("/");
			if (parts.length != 2) {
				throw new IOException("Invalid Content-Range header format");
			}

			return Integer.parseInt(parts[1]);
		}
	}

	public int fetchTotalRentalRecordsCount() throws IOException {
		HttpUrl.Builder urlBuilder = HttpUrl.parse(SUPABASE_URL + "/rest/v1/rental_records").newBuilder();
		urlBuilder.addQueryParameter("select", "*");

		String url = urlBuilder.build().toString();

		Request request = new Request.Builder().url(url).addHeader("apikey", SUPABASE_API_KEY)
				.addHeader("Authorization", "Bearer " + SUPABASE_API_KEY).addHeader("Prefer", "count=exact").build();

		try (Response response = client.newCall(request).execute(); ResponseBody responseBody = response.body()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response);
			}

			String countHeader = response.header("Content-Range");
			if (countHeader == null || countHeader.isEmpty()) {
				throw new IOException("Missing Content-Range header in response");
			}

			String[] parts = countHeader.split("/");
			if (parts.length != 2) {
				throw new IOException("Invalid Content-Range header format");
			}

			return Integer.parseInt(parts[1]);
		}
	}

	@SuppressWarnings("exports")
	public List<JSONObject> fetchRentalRecords(int start, int end) throws IOException {
		HttpUrl.Builder urlBuilder = HttpUrl.parse(SUPABASE_URL + "/rest/v1/rental_records").newBuilder();
		urlBuilder.addQueryParameter("select", "*");
		urlBuilder.addQueryParameter("offset", String.valueOf(start));
		urlBuilder.addQueryParameter("limit", String.valueOf(end - start + 1));

		String url = urlBuilder.build().toString();

		Request request = new Request.Builder().url(url).addHeader("apikey", SUPABASE_API_KEY)
				.addHeader("Authorization", "Bearer " + SUPABASE_API_KEY).build();

		try (Response response = client.newCall(request).execute(); ResponseBody responseBody = response.body()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response);
			}

			JSONArray jsonArray = new JSONArray(responseBody.string());
			List<JSONObject> resultList = new ArrayList<>();
			for (int i = 0; i < jsonArray.length(); i++) {
				resultList.add(jsonArray.getJSONObject(i));
			}
			return resultList;
		}
	}

	// 獲取會員信息
	@SuppressWarnings("exports")
	public JSONObject getMemberInfo(String phoneNumber) throws IOException {
		HttpUrl url = HttpUrl.parse(SUPABASE_URL + "/rest/v1/members").newBuilder().addQueryParameter("select", "*")
				.addQueryParameter("phone_number", "eq." + phoneNumber).build();

		Request request = new Request.Builder().url(url).addHeader("apikey", SUPABASE_API_KEY)
				.addHeader("Authorization", "Bearer " + SUPABASE_API_KEY).build();

		try (Response response = client.newCall(request).execute(); ResponseBody responseBody = response.body()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response);
			}

			JSONArray jsonArray = new JSONArray(responseBody.string());
			return jsonArray.getJSONObject(0);
		}
	}

	// 獲取悠遊卡信息
	@SuppressWarnings("exports")
	public JSONObject getEasyCardInfo(String easyCardNumber) throws IOException {
		HttpUrl url = HttpUrl.parse(SUPABASE_URL + "/rest/v1/easycards").newBuilder().addQueryParameter("select", "*")
				.addQueryParameter("card_number", "eq." + easyCardNumber).build();

		Request request = new Request.Builder().url(url).addHeader("apikey", SUPABASE_API_KEY)
				.addHeader("Authorization", "Bearer " + SUPABASE_API_KEY).build();

		try (Response response = client.newCall(request).execute(); ResponseBody responseBody = response.body()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response);
			}

			String responseBodyString = responseBody.string();
			JSONArray jsonArray = new JSONArray(responseBodyString);

			if (jsonArray.length() == 0) {
				throw new IOException("EasyCard not found");
			}

			return jsonArray.getJSONObject(0);
		}
	}

	// 儲值悠遊卡
	public void topUpEasyCard(String cardNumber, double amount) throws IOException {
	    JSONObject easyCardInfo = getEasyCardInfo(cardNumber);
	    double currentBalance = easyCardInfo.getDouble("balance");
	    double newBalance = currentBalance + amount;

	    // 獲取當前時間並格式化
	    String currentDateTime = java.time.LocalDateTime.now().toString();

	    // 更新交易紀錄
	    String transactionRecord = easyCardInfo.optString("transaction_records", "");
	    transactionRecord += currentDateTime + ": 儲值金額: " + amount + " | 新餘額: " + newBalance + "\\n";
	    System.out.println("transactionRecord:" + transactionRecord);

	    // 構建更新的 JSON，將換行符轉義
	    String json = "{\"balance\": " + newBalance + ", \"transaction_records\": \"" + transactionRecord.replace("\n", "\\n") + "\"}";
	    System.out.println("json:" + json);
	    RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));

	    HttpUrl url = HttpUrl.parse(SUPABASE_URL + "/rest/v1/easycards").newBuilder()
	            .addQueryParameter("card_number", "eq." + cardNumber).build();
	    System.out.println("url:" + url + "\n");
	    Request request = new Request.Builder().url(url).patch(body).addHeader("apikey", SUPABASE_API_KEY)
	            .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY).addHeader("Prefer", "return=representation").build();

	    try (Response response = client.newCall(request).execute()) {
	        if (!response.isSuccessful()) {
	            System.out.println("Response: " + response.body().string());
	            throw new IOException("Unexpected code " + response);
	        }
	    }
	}


	// 維修紀錄維護
	public void reportMaintenance(int stationId, int dockNumber, String bikeNumber, String report, String maintenanceItems) {
		String json = "{\"station_id\":" + stationId + ",\"dock_number\":" + dockNumber + ",\"bike_number\":\"" + bikeNumber
				+ "\",\"maintenance_report\":\"" + report + "\",\"maintenance_items\":\"" + maintenanceItems + "\"}";
		RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
		Request request = new Request.Builder().url(SUPABASE_URL + "/rest/v1/docks_maintance_info").post(body)
				.addHeader("apikey", SUPABASE_API_KEY).addHeader("Authorization", "Bearer " + SUPABASE_API_KEY).build();

		client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				e.printStackTrace();
				System.out.println("Maintenance report failed.");
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				try (ResponseBody responseBody = response.body()) {
					if (response.isSuccessful()) {
						System.out.println("Maintenance report successful!");
					} else {
						System.out.println("Maintenance report failed with response: " + responseBody.string());
					}
				}
			}
		});
	}

	// 註冊時新增悠遊卡卡號 EasyCardRecord
	private void insertEasyCardRecord(String easyCardNumber) {
		String transactionRecord = LocalDate.now() + ": 註冊開卡";
		String json = "{\"card_number\":\"" + easyCardNumber + "\",\"balance\":-100.00,\"transaction_records\":\""
				+ transactionRecord + "\"}";
		RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
		Request request = new Request.Builder().url(SUPABASE_URL + "/rest/v1/easycards").post(body)
				.addHeader("apikey", SUPABASE_API_KEY).addHeader("Authorization", "Bearer " + SUPABASE_API_KEY).build();

		client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				e.printStackTrace();
				System.out.println("EasyCard record insertion failed.");
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				try (ResponseBody responseBody = response.body()) {
					if (response.isSuccessful()) {
						System.out.println("EasyCard record insertion response: " + responseBody.string());
					} else {
						System.out.println("EasyCard record insertion failed with response: " + responseBody.string());
					}
				}
			}
		});
	}

	// 獲取站點ID列表
	public List<Integer> fetchStationIds() throws IOException {
		HttpUrl url = HttpUrl.parse(SUPABASE_URL + "/rest/v1/stations").newBuilder().addQueryParameter("select", "id").build();

		Request request = new Request.Builder().url(url).addHeader("apikey", SUPABASE_API_KEY)
				.addHeader("Authorization", "Bearer " + SUPABASE_API_KEY).build();

		try (Response response = client.newCall(request).execute(); ResponseBody responseBody = response.body()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response);
			}

			JSONArray jsonArray = new JSONArray(responseBody.string());
			List<Integer> stationIds = new ArrayList<>();
			for (int i = 0; i < jsonArray.length(); i++) {
				stationIds.add(jsonArray.getJSONObject(i).getInt("id"));
			}
			return stationIds;
		}
	}
	
	// 獲取用戶的悠遊卡卡號
	public String getEasyCardNumber(String phoneNumber) throws IOException {
	    JSONObject memberInfo = getMemberInfo(phoneNumber);
	    return memberInfo.getString("easycard_number");
	}

}
