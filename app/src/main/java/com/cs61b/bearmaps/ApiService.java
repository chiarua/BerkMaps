package com.cs61b.bearmaps;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ApiService {
    private static final String BASE_URL = "http://localhost:8080";
    private final OkHttpClient client;
    private final Gson gson;

    public ApiService() {
        client = new OkHttpClient();
        gson = new Gson();
    }

    public interface SearchCallback {
        void onSearchResult(List<Location> locations);
        void onError(String message);
    }

    public void searchLocation(String query, final SearchCallback callback) {
        String url = BASE_URL + "/search?term=" + query + "&full=true";
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Search failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("Server error: " + response.code());
                    return;
                }

                String jsonData = response.body().string();
                Type locationType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> locationData = gson.fromJson(jsonData, locationType);
                
                // 将Map转换为Location对象
                List<Location> locations = new ArrayList<>();
                for (Map<String, Object> data : locationData) {
                    Location location = new Location(
                            ((Number) data.get("lat")).doubleValue(),
                            ((Number) data.get("lon")).doubleValue(),
                            (String) data.get("name"),
                            ((Number) data.get("id")).longValue()
                    );
                    locations.add(location);
                }

                callback.onSearchResult(locations);
            }
        });
    }

    public void getRoute(double startLat, double startLon, 
                        double endLat, double endLon, 
                        final RouteCallback callback) {
        String url = String.format("%s/route?start_lat=%f&start_lon=%f&end_lat=%f&end_lon=%f",
                BASE_URL, startLat, startLon, endLat, endLon);

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Fail to get route: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("Server error: " + response.code());
                    return;
                }

                String jsonData = response.body().string();
                RouteResult result = gson.fromJson(jsonData, RouteResult.class);
                callback.onRouteResult(result);
            }
        });
    }

    public interface RouteCallback {
        void onRouteResult(RouteResult result);
        void onError(String message);
    }
}