package com.example;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
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

    public void register(String email, String password) {
        String json = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/signup")
                .post(body)
                .addHeader("apikey", SUPABASE_API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                System.out.println("Registration failed.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                System.out.println("Registration response: " + response.body().string());
            }
        });
    }

    public void login(String email, String password, LoginCallback callback) {
        String json = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/token?grant_type=password")
                .post(body)
                .addHeader("apikey", SUPABASE_API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                System.out.println("Login failed.");
                Platform.runLater(callback::onFailure);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    System.out.println("Login response: " + response.body().string());
                    Platform.runLater(callback::onSuccess);
                } else {
                    System.out.println("Login failed with response: " + response.body().string());
                    Platform.runLater(callback::onFailure);
                }
            }
        });
    }

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

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }

        JSONArray jsonArray = new JSONArray(response.body().string());
        List<JSONObject> resultList = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            resultList.add(jsonArray.getJSONObject(i));
        }
        return resultList;
    }


    public List<String> fetchCities(String bikeType) throws IOException {
        Set<String> cities = new HashSet<>();

        // URL for calling the RPC function
        HttpUrl.Builder urlBuilder = HttpUrl.parse(SUPABASE_URL + "/rest/v1/rpc/distinct_cities").newBuilder();
        
        String url = urlBuilder.build().toString();

        // JSON body for the RPC request
        String jsonBody = "{\"bike_type\":\"" + bikeType + "\"}";

        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }

        String responseBody = response.body().string();
        System.out.println("Response Body: " + responseBody);

        JSONArray jsonArray = new JSONArray(responseBody);

        for (int i = 0; i < jsonArray.length(); i++) {
            String city = jsonArray.getJSONObject(i).optString("city");
            if (city != null) {
                cities.add(city);
            }
        }

        return new ArrayList<>(cities);
    }

    public int fetchTotalDataCount(String city, String bikeType) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(SUPABASE_URL + "/rest/v1/stations").newBuilder();
        urlBuilder.addQueryParameter("select", "*");
        urlBuilder.addQueryParameter("bike_type", "eq." + bikeType);
        if (city != null && !city.equals("All")) {
            urlBuilder.addQueryParameter("city", "eq." + city);
        }

        String url = urlBuilder.build().toString();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Prefer", "count=exact")
                .build();

        Response response = client.newCall(request).execute();
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
