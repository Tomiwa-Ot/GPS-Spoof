package com.android.gpsspoof;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.library.BuildConfig;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

//    private Location location;
//    private LocationManager manager;
    private MapView map;
    private Marker currentMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if(!isMockLocationEnabled(getApplicationContext())) {
            Toast.makeText(this, "Enable mock location for this app in developer settings", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        }

//        location = new Location(LocationManager.GPS_PROVIDER);
//        manager = (LocationManager) getSystemService(LOCATION_SERVICE);
//        manager.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false, true, true, true, 1, 1);
//        manager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        GeoPoint startPoint = new GeoPoint(51.0, -2.0);
        map.getController().setZoom(15.0);
        map.getController().setCenter(startPoint);
        // Add tap listener
        map.setOnTouchListener((v, event) -> false); // Required for gestures
        map.getOverlays().add(new org.osmdroid.views.overlay.MapEventsOverlay(
                getBaseContext(),
                new org.osmdroid.events.MapEventsReceiver() {
                    @Override
                    public boolean singleTapConfirmedHelper(GeoPoint p) {
                        showMarker(p);
                        return true;
                    }

                    @Override
                    public boolean longPressHelper(GeoPoint p) {
                        return false;
                    }
                }));


    }

    private void showMarker(GeoPoint point) {
        spoofLocation(point.getLatitude(), point.getLongitude(), point.getAltitude());

        if (currentMarker != null) {
            map.getOverlays().remove(currentMarker);
            currentMarker = null;
        }

        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setTitle("Selected Location");
        marker.setSubDescription("Lat: " + point.getLatitude() + "\nLon: " + point.getLongitude());
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(marker);
        currentMarker = marker;

        map.invalidate();

        Toast.makeText(this, "Lat: " + point.getLatitude() + "\nLon: " + point.getLongitude(), Toast.LENGTH_SHORT).show();
    }

    public void spoofLocation (double latitude, double longitude, double altitude) {
        MockLocationService.latitude = latitude;
        MockLocationService.longitude = longitude;
        MockLocationService.altitude = altitude;

        Intent intent = new Intent(this, MockLocationService.class);
        startService(intent);
    }


    private boolean isMockLocationEnabled(Context context) {
        if (Settings.Secure.getInt(
                context.getContentResolver(),
                Settings.Secure.ALLOW_MOCK_LOCATION, 0) != 0) {
            return true;
        }

        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }
}