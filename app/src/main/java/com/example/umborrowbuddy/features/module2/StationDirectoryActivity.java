package com.example.umborrowbuddy.features.module2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.umborrowbuddy.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.layers.CircleLayer;
import org.maplibre.android.style.layers.SymbolLayer;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.FeatureCollection;
import org.maplibre.geojson.Point;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.maplibre.android.style.expressions.Expression.get;
import static org.maplibre.android.style.layers.PropertyFactory.circleColor;
import static org.maplibre.android.style.layers.PropertyFactory.circleRadius;
import static org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor;
import static org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth;
import static org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap;
import static org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement;
import static org.maplibre.android.style.layers.PropertyFactory.iconImage;

public final class StationDirectoryActivity extends AppCompatActivity {
    private static final String BASEMAP = "https://tiles.openfreemap.org/styles/liberty";
    private static final LatLng FSKTM = new LatLng(3.12832, 101.65039);
    private static final double CAMPUS_ZOOM = 18;
    private static final int NEARBY_METERS = 500;
    private static final String STATIONS = "m2-stations";
    private static final String USER_LOCATION = "m2-user-location";
    private static final String PIN_GROUP = "group";
    private static final String PIN_IMAGE = "image";
    private static final String LOCATION_ASKED = "m2_location_asked";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable mapTimeout = this::showMapError;
    private boolean recenterAfterPermission;
    private final ActivityResultLauncher<String[]> requestPermissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (hasLocationPermission()) {
                    findLocation(recenterAfterPermission);
                } else if (recenterAfterPermission) {
                    showLocationMessage(R.string.m2_location_unavailable);
                }
            });

    private MapView mapView;
    private MapLibreMap map;
    private View mapPanel;
    private View listPanel;
    private View mapFeedback;
    private TextView mapMessage;
    private View retry;
    private LinearLayout stationList;
    private List<Station> stations = Collections.emptyList();
    private List<List<Station>> pinGroups = Collections.emptyList();
    private Location userLocation;
    private CancellationSignal locationRequest;
    private Runnable locationTimeout;
    private boolean initialCenter;
    private boolean waitingForMap;
    private boolean stationLayersReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapLibre.getInstance(this);
        WindowCompat.enableEdgeToEdge(getWindow());
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars((getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES);
        setContentView(R.layout.m2_activity_station_directory);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.m2_root), (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        initialCenter = savedInstanceState == null;
        mapView = findViewById(R.id.m2_map_view);
        mapPanel = findViewById(R.id.m2_map_panel);
        listPanel = findViewById(R.id.m2_list_panel);
        mapFeedback = findViewById(R.id.m2_map_feedback);
        mapMessage = findViewById(R.id.m2_map_message);
        retry = findViewById(R.id.m2_retry);
        stationList = findViewById(R.id.m2_station_list);
        TextView legend = findViewById(R.id.m2_legend);
        legend.setText(getString(R.string.m2_legend, getString(R.string.m2_available),
                getString(R.string.m2_out_of_service), getString(R.string.m2_unknown)));

        try {
            stations = readStations();
            pinGroups = Station.groupPins(stations);
        } catch (IOException | JSONException | IllegalArgumentException error) {
            findViewById(R.id.m2_data_error).setVisibility(View.VISIBLE);
        }
        renderList();

        MaterialButtonToggleGroup switcher = findViewById(R.id.m2_view_switch);
        switcher.addOnButtonCheckedListener((group, id, checked) -> {
            if (!checked) return;
            boolean showMap = id == R.id.m2_map_tab;
            mapPanel.setVisibility(showMap ? View.VISIBLE : View.INVISIBLE);
            listPanel.setVisibility(showMap ? View.GONE : View.VISIBLE);
            scheduleMapTimeout();
        });
        retry.setOnClickListener(view -> loadMap());
        findViewById(R.id.m2_my_location).setOnClickListener(view -> requestLocation(true));

        mapView.onCreate(savedInstanceState);
        mapView.addOnDidFailLoadingMapListener(message -> showMapError());
        mapView.addOnDidFinishRenderingMapListener(fully -> {
            if (fully && stationLayersReady) {
                waitingForMap = false;
                handler.removeCallbacks(mapTimeout);
                mapFeedback.setVisibility(View.GONE);
                mapView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
            }
        });
        mapView.getMapAsync(readyMap -> {
            map = readyMap;
            if (savedInstanceState == null) center(FSKTM);
            if (userLocation != null && initialCenter) centerOnNearbyUser();
            map.addOnCameraMoveStartedListener(reason -> {
                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                    initialCenter = false;
                }
            });
            map.addOnMapClickListener(this::onPinClick);
            loadMap();
        });
    }

    private List<Station> readStations() throws IOException, JSONException {
        List<Station> result = new ArrayList<>();
        try (InputStream input = getAssets().open("m2_fsktm_stations.json");
                ByteArrayOutputStream content = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = input.read(buffer)) != -1) content.write(buffer, 0, count);
            JSONArray records = new JSONArray(new String(content.toByteArray(), StandardCharsets.UTF_8));
            for (int i = 0; i < records.length(); i++) {
                JSONObject record = records.getJSONObject(i);
                result.add(new Station(record.getString("stationId"), record.getString("name"),
                        record.getString("building"), record.getString("floor"),
                        record.getString("landmark"), record.getDouble("latitude"),
                        record.getDouble("longitude"),
                        Station.Status.valueOf(record.getString("status"))));
            }
        }
        return result;
    }

    private void loadMap() {
        stationLayersReady = false;
        waitingForMap = true;
        mapFeedback.setVisibility(View.VISIBLE);
        mapView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        mapMessage.setText(R.string.m2_loading);
        retry.setVisibility(View.GONE);
        scheduleMapTimeout();
        map.setStyle(new Style.Builder().fromUri(BASEMAP), style -> {
            addMapLayers(style);
            stationLayersReady = true;
        });
    }

    private void scheduleMapTimeout() {
        handler.removeCallbacks(mapTimeout);
        if (waitingForMap && mapPanel.getVisibility() == View.VISIBLE) {
            handler.postDelayed(mapTimeout, 15_000);
        }
    }

    private void showMapError() {
        waitingForMap = false;
        handler.removeCallbacks(mapTimeout);
        mapFeedback.setVisibility(View.VISIBLE);
        mapView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        mapMessage.setText(R.string.m2_map_unavailable);
        retry.setVisibility(View.VISIBLE);
    }

    private void addMapLayers(Style style) {
        style.addSource(new GeoJsonSource(USER_LOCATION, locationFeatures()));
        style.addLayer(new CircleLayer(USER_LOCATION, USER_LOCATION).withProperties(
                circleRadius(8f), circleColor(0xff1565c0),
                circleStrokeColor(Color.WHITE), circleStrokeWidth(2f)));

        style.addImage("AVAILABLE", badge("✓", 0xff176145));
        style.addImage("OUT_OF_SERVICE", badge("×", 0xffa62f36));
        style.addImage("UNKNOWN", badge("?", 0xff4c5562));
        List<Feature> pins = new ArrayList<>();
        for (int i = 0; i < pinGroups.size(); i++) {
            List<Station> group = pinGroups.get(i);
            Station first = group.get(0);
            String icon = first.status.name();
            if (group.size() > 1) {
                icon = "count-" + i;
                style.addImage(icon, badge(Integer.toString(group.size()), 0xff34487c));
            }
            Feature pin = Feature.fromGeometry(Point.fromLngLat(first.longitude, first.latitude));
            pin.addNumberProperty(PIN_GROUP, i);
            pin.addStringProperty(PIN_IMAGE, icon);
            pins.add(pin);
        }
        style.addSource(new GeoJsonSource(STATIONS, FeatureCollection.fromFeatures(pins)));
        style.addLayer(new SymbolLayer(STATIONS, STATIONS).withProperties(
                iconImage(get(PIN_IMAGE)), iconAllowOverlap(true), iconIgnorePlacement(true)));
    }

    private Bitmap badge(String symbol, int color) {
        int size = Math.round(32 * getResources().getDisplayMetrics().density);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        bitmap.setDensity(getResources().getDisplayMetrics().densityDpi);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        float center = size / 2f;
        paint.setColor(Color.WHITE);
        canvas.drawCircle(center, center, center, paint);
        paint.setColor(color);
        canvas.drawCircle(center, center, center * 0.87f, paint);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(size * 0.52f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(symbol, center, center - (paint.ascent() + paint.descent()) / 2, paint);
        return bitmap;
    }

    private boolean onPinClick(LatLng coordinate) {
        PointF tap = map.getProjection().toScreenLocation(coordinate);
        List<Feature> hits = map.queryRenderedFeatures(tap, STATIONS);
        if (hits.isEmpty()) return false;
        showDetails(pinGroups.get(hits.get(0).getNumberProperty(PIN_GROUP).intValue()));
        return true;
    }

    private void renderList() {
        List<Station> ordered = new ArrayList<>(stations);
        ordered.sort(userLocation == null ? Station.byName()
                : Station.byDistance(userLocation.getLatitude(), userLocation.getLongitude()));
        stationList.removeAllViews();
        for (Station station : ordered) {
            MaterialButton row = (MaterialButton) getLayoutInflater()
                    .inflate(R.layout.m2_station_row, stationList, false);
            String position = stationPosition(station);
            if (userLocation != null) {
                position = getString(R.string.m2_station_distance, position,
                        Math.round(Station.distanceMeters(userLocation.getLatitude(),
                                userLocation.getLongitude(), station.latitude, station.longitude)));
            }
            row.setText(getString(R.string.m2_station_row, station.name, position,
                    station.landmark, getString(statusLabel(station.status))));
            row.setOnClickListener(view -> showDetails(Collections.singletonList(station)));
            stationList.addView(row);
        }
    }

    private void showDetails(List<Station> group) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = getLayoutInflater().inflate(R.layout.m2_station_sheet, null);
        LinearLayout details = sheet.findViewById(R.id.m2_station_details);
        if (group.size() > 1) {
            addDetail(details, getResources().getQuantityString(R.plurals.m2_station_count,
                    group.size(), group.size()), true);
        }
        for (Station station : group) {
            addDetail(details, station.name, true);
            addDetail(details, stationPosition(station), false);
            addDetail(details, getString(statusLabel(station.status)), false);
            addDetail(details, station.landmark, false);
        }
        dialog.setContentView(sheet);
        dialog.show();
    }

    private void addDetail(LinearLayout details, String value, boolean heading) {
        TextView line = new TextView(this);
        line.setText(value);
        line.setTextAppearance(heading ? com.google.android.material.R.style.TextAppearance_Material3_TitleMedium
                : com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        ViewCompat.setAccessibilityHeading(line, heading);
        int spacing = Math.round((heading ? 12 : 4) * getResources().getDisplayMetrics().density);
        line.setPadding(0, spacing, 0, spacing);
        details.addView(line);
    }

    private String stationPosition(Station station) {
        return getString(R.string.m2_station_position, station.building, station.floor);
    }

    private int statusLabel(Station.Status status) {
        switch (status) {
            case AVAILABLE: return R.string.m2_available;
            case OUT_OF_SERVICE: return R.string.m2_out_of_service;
            default: return R.string.m2_unknown;
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocation(boolean recenter) {
        if (hasLocationPermission()) {
            findLocation(recenter);
        } else if (recenter && getPreferences(MODE_PRIVATE).getBoolean(LOCATION_ASKED, false)
                && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
                && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            Snackbar.make(mapPanel, R.string.m2_location_settings, Snackbar.LENGTH_LONG)
                    .setAction(R.string.m2_settings, view -> startActivity(new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", getPackageName(), null))))
                    .show();
        } else {
            getPreferences(MODE_PRIVATE).edit().putBoolean(LOCATION_ASKED, true).apply();
            recenterAfterPermission = recenter;
            requestPermissions.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    @SuppressLint("MissingPermission")
    private void findLocation(boolean recenter) {
        cancelLocation();
        LocationManager manager = getSystemService(LocationManager.class);
        String provider = manager.isProviderEnabled("fused") ? "fused"
                : manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ? LocationManager.NETWORK_PROVIDER
                : LocationManager.GPS_PROVIDER;
        if (!manager.isProviderEnabled(provider)) {
            receiveLocation(null, recenter);
            return;
        }
        CancellationSignal request = new CancellationSignal();
        locationRequest = request;
        locationTimeout = () -> {
            cancelLocation();
            receiveLocation(null, recenter);
        };
        handler.postDelayed(locationTimeout, 12_000);
        try {
            androidx.core.location.LocationManagerCompat.getCurrentLocation(manager, provider,
                    request, ContextCompat.getMainExecutor(this), location -> {
                        if (locationRequest != request) return;
                        cancelLocation();
                        receiveLocation(location, recenter);
                    });
        } catch (IllegalArgumentException | SecurityException error) {
            cancelLocation();
            receiveLocation(null, recenter);
        }
    }

    private void cancelLocation() {
        if (locationRequest != null) {
            locationRequest.cancel();
            locationRequest = null;
            handler.removeCallbacks(locationTimeout);
        }
    }

    private void receiveLocation(Location location, boolean recenter) {
        userLocation = location;
        renderList();
        Style style = map == null ? null : map.getStyle();
        GeoJsonSource source = style == null ? null : style.getSourceAs(USER_LOCATION);
        if (source != null) source.setGeoJson(locationFeatures());
        if (location == null) {
            if (recenter) showLocationMessage(R.string.m2_location_unavailable);
        } else if (recenter && map != null) {
            initialCenter = false;
            center(new LatLng(location.getLatitude(), location.getLongitude()));
        } else if (initialCenter && map != null) {
            centerOnNearbyUser();
        }
    }

    private FeatureCollection locationFeatures() {
        if (userLocation == null) return FeatureCollection.fromFeatures(new Feature[0]);
        return FeatureCollection.fromFeatures(new Feature[]{Feature.fromGeometry(
                Point.fromLngLat(userLocation.getLongitude(), userLocation.getLatitude()))});
    }

    private void centerOnNearbyUser() {
        initialCenter = false;
        double lat = userLocation.getLatitude();
        double lon = userLocation.getLongitude();
        if (Station.distanceMeters(FSKTM.getLatitude(), FSKTM.getLongitude(), lat, lon) <= NEARBY_METERS) {
            center(new LatLng(lat, lon));
        }
    }

    private void center(LatLng target) {
        map.setCameraPosition(new CameraPosition.Builder().target(target).zoom(CAMPUS_ZOOM).build());
    }

    private void showLocationMessage(int message) {
        Snackbar.make(mapPanel, message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
        scheduleMapTimeout();
        if (hasLocationPermission() || !getPreferences(MODE_PRIVATE).getBoolean(LOCATION_ASKED, false)) {
            if (locationRequest == null) requestLocation(false);
        } else if (userLocation != null) {
            receiveLocation(null, false);
        }
    }

    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { mapView.onPause(); super.onPause(); }

    @Override
    protected void onStop() {
        cancelLocation();
        handler.removeCallbacks(mapTimeout);
        mapView.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        mapView.onSaveInstanceState(state);
    }
}
