package com.android.gpsspoof;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

public class MockLocationService extends Service {
    private static final String CHANNEL_ID = "MockLocationChannel";
    private static final int NOTIFICATION_ID = 101;

    public static double latitude, longitude, altitude;
    private LocationManager locationManager;
    private Handler handler;
    private Runnable runnable;

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        handler = new Handler();

        // Foreground notification
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GPS Spoofing Active")
                .setContentText("Mocking location: Lat " + latitude + ", Lon " + longitude  )
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
        startForeground(NOTIFICATION_ID, notification);

        // Start periodic location mocking
        runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    spoofLocation();
                    // Update notification with new coordinates
                    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.notify(NOTIFICATION_ID, updateNotification());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                handler.postDelayed(this, 2000); // Repeat every 2 seconds
            }
        };

        handler.post(runnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification updateNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GPS Spoofing Active")
                .setContentText("Mocking location: Latitude " + latitude + " Longitude: " + longitude)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .build();
    }

    public void spoofLocation() {
        try {
            String provider = LocationManager.GPS_PROVIDER;
            locationManager.addTestProvider(
                    provider, false, false, false, false, true, true, true, 1, 1);
            locationManager.setTestProviderEnabled(provider, true);

            Location location = new Location(provider);

            location.setLatitude(latitude);
            location.setLongitude(longitude);
            location.setAltitude(altitude);
            location.setTime(System.currentTimeMillis());
            location.setAccuracy(1);
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                location.setBearingAccuracyDegrees(.1F);
                location.setVerticalAccuracyMeters(.1F);
                location.setSpeedAccuracyMetersPerSecond(.01F);
            }
            locationManager.setTestProviderLocation(provider, location);
        } catch (Exception e) {
            Log.e("eeee", e.toString());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Mock Location Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
}
