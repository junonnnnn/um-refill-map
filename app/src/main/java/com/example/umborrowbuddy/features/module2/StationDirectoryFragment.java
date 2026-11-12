package com.example.umborrowbuddy.features.module2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.example.umborrowbuddy.R;
import com.google.firebase.firestore.ListenerRegistration;
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

public final class StationDirectoryFragment extends Fragment {
    private static final String BASEMAP = "https://tiles.openfreemap.org/styles/liberty";
    private static final LatLng FSKTM = new LatLng(3.12832, 101.65039);
    private static final double CAMPUS_ZOOM = 18;
    private static final int NEARBY_METERS = 500;
    private static final String STATIONS = "m2-stations";
    private static final String USER_LOCATION = "m2-user-location";
    private static final String PIN_STATION_ID = "stationId";
    private static final String PIN_IMAGE = "image";
    private static final String LOCATION_ASKED = "m2_location_asked";
    private static final String DEMO_MODE = "m2_demo_mode";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable mapTimeout = this::showMapError;
    private boolean recenterAfterPermission;
    private final ActivityResultLauncher<String[]> requestPermissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (hasLocationPermission()) {
                    findLocation(recenterAfterPermission);
                } else if (recenterAfterPermission) {
                    pendingNearest = false;
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
    private Bundle savedMapState;
    private ListenerRegistration stationSubscription;
    private int dataRequest;
    private boolean demoMode;
    private String facultyFilter;
    private String collegeFilter;
    private Station.WaterType waterFilter;
    private boolean pendingNearest;
    private final List<ListenerRegistration> reportSubscriptions = new ArrayList<>();
    private int reportRequest;

    private BottomSheetDialog detailsDialog;
    private List<Station> displayedStations = Collections.emptyList();
    private final List<String> detailStationIds = new ArrayList<>();

    public StationDirectoryFragment() {
        super(R.layout.m2_activity_station_directory);
    }

    static StationDirectoryFragment newDemoInstance() {
        StationDirectoryFragment fragment = new StationDirectoryFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean(DEMO_MODE, true);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapLibre.getInstance(requireContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle mapState = savedMapState == null ? savedInstanceState : savedMapState;
        initialCenter = mapState == null;
        mapView = view.findViewById(R.id.m2_map_view);
        mapPanel = view.findViewById(R.id.m2_map_panel);
        listPanel = view.findViewById(R.id.m2_list_panel);
        mapFeedback = view.findViewById(R.id.m2_map_feedback);
        mapMessage = view.findViewById(R.id.m2_map_message);
        retry = view.findViewById(R.id.m2_retry);
        stationList = view.findViewById(R.id.m2_station_list);
        if (mapState != null) {
            facultyFilter = mapState.getString("m2_faculty");
            collegeFilter = mapState.getString("m2_college");
            String water = mapState.getString("m2_water");
            waterFilter = water == null ? null : Station.WaterType.valueOf(water);
        }
        view.findViewById(R.id.m2_filters).setOnClickListener(button -> showFilters());
        view.findViewById(R.id.m2_nearest).setOnClickListener(button -> {
            pendingNearest = true;
            requestLocation(true);
        });
        TextView legend = view.findViewById(R.id.m2_legend);
        legend.setText(getString(R.string.m2_legend, getString(R.string.m2_available),
                getString(R.string.m2_reported), getString(R.string.m2_out_of_service), getString(R.string.m2_unknown))
                + "\n" + getString(R.string.m2_shared_pin_hint));

        demoMode = getArguments() != null && getArguments().getBoolean(DEMO_MODE);
        if (demoMode) loadDemoStations();
        else renderList();
        view.findViewById(R.id.m2_data_retry).setOnClickListener(button -> {
            if (demoMode) loadDemoStations();
            else {
                stopStationUpdates();
                startStationUpdates();
            }
        });

        MaterialButtonToggleGroup switcher = view.findViewById(R.id.m2_view_switch);
        switcher.addOnButtonCheckedListener((group, id, checked) -> {
            if (!checked) return;
            boolean showMap = id == R.id.m2_map_tab;
            mapPanel.setVisibility(showMap ? View.VISIBLE : View.INVISIBLE);
            listPanel.setVisibility(showMap ? View.GONE : View.VISIBLE);
            scheduleMapTimeout();
        });
        retry.setOnClickListener(button -> loadMap());
        view.findViewById(R.id.m2_my_location).setOnClickListener(button -> requestLocation(true));

        mapView.onCreate(mapState);
        mapView.addOnDidFailLoadingMapListener(message -> showMapError());
        mapView.addOnDidFinishRenderingMapListener(fully -> {
            if (fully && stationLayersReady) {
                waitingForMap = false;
                handler.removeCallbacks(mapTimeout);
                mapFeedback.setVisibility(View.GONE);
                mapView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
            }
        });
        MapView currentMapView = mapView;
        mapView.getMapAsync(readyMap -> {
            if (mapView != currentMapView) return;
            map = readyMap;
            if (mapState == null) {
                if (demoMode) center(FSKTM);
                else map.setCameraPosition(new CameraPosition.Builder()
                        .target(new LatLng(3.120, 101.654)).zoom(14).build());
            }
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
        try (InputStream input = requireContext().getAssets().open("m2_fsktm_stations.json");
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

    private void loadDemoStations() {
        try {
            applyStations(readStations());
            showDataState(0, false);
        } catch (IOException | JSONException | IllegalArgumentException error) {
            showDataState(R.string.m2_data_unavailable, true);
        }
    }

    private void startStationUpdates() {
        int request = ++dataRequest;
        showDataState(R.string.m2_loading_stations, false);
        stationSubscription = new StationRepository().listen(new StationRepository.Callback() {
            @Override
            public void onStations(List<Station> records, boolean fromCache) {
                if (request != dataRequest || getView() == null) return;
                applyStations(records);
                if (fromCache) {
                    showDataState(records.isEmpty() ? R.string.m2_no_saved_stations
                            : R.string.m2_saved_stations, records.isEmpty());
                } else {
                    showDataState(records.isEmpty() ? R.string.m2_no_stations : 0, false);
                }
            }

            @Override
            public void onFailure() {
                if (request != dataRequest || getView() == null) return;
                applyStations(Collections.emptyList());
                showDataState(R.string.m2_data_unavailable, true);
            }
        });
    }

    private void stopStationUpdates() {
        ++dataRequest;
        if (stationSubscription != null) {
            stationSubscription.remove();
            stationSubscription = null;
        }
    }

    private void applyStations(List<Station> records) {
        stations = records;
        refreshDirectory();
    }

    private List<Station> visibleStations() {
        return Station.matching(stations, facultyFilter, collegeFilter, waterFilter);
    }

    private void refreshDirectory() {
        pinGroups = Station.groupPins(visibleStations());
        renderList();
        Style style = map == null ? null : map.getStyle();
        if (style != null && stationLayersReady) updateStationPins(style);
        if (detailsDialog != null && detailsDialog.isShowing()) {
            List<Station> selected = new ArrayList<>();
            for (Station station : stations) {
                if (detailStationIds.contains(station.id)) selected.add(station);
            }
            if (selected.isEmpty()) detailsDialog.dismiss();
            else if (!sameDetails(selected) || reportSubscriptions.isEmpty() && !demoMode) {
                LinearLayout details = detailsDialog.findViewById(R.id.m2_station_details);
                details.removeAllViews();
                populateDetails(details, selected);
            }
        }
    }

    private boolean sameDetails(List<Station> selected) {
        if (selected.size() != displayedStations.size()) return false;
        for (int i = 0; i < selected.size(); i++) {
            if (!selected.get(i).hasSameDetails(displayedStations.get(i))) return false;
        }
        return true;
    }

    private void showDataState(int message, boolean canRetry) {
        TextView state = requireView().findViewById(R.id.m2_data_error);
        state.setVisibility(message == 0 ? View.GONE : View.VISIBLE);
        if (message != 0) state.setText(message);
        requireView().findViewById(R.id.m2_data_retry)
                .setVisibility(canRetry ? View.VISIBLE : View.GONE);
    }

    private void loadMap() {
        stationLayersReady = false;
        waitingForMap = true;
        mapFeedback.setVisibility(View.VISIBLE);
        mapView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        mapMessage.setText(R.string.m2_loading);
        retry.setVisibility(View.GONE);
        scheduleMapTimeout();
        MapView currentMapView = mapView;
        map.setStyle(new Style.Builder().fromUri(BASEMAP), style -> {
            if (mapView != currentMapView) return;
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
        style.addImage("REPORTED", badge("!", 0xff996000));
        style.addImage("OUT_OF_SERVICE", badge("×", 0xffa62f36));
        style.addImage("UNKNOWN", badge("?", 0xff4c5562));
        updateStationPins(style);
    }

    private void updateStationPins(Style style) {
        List<Feature> pins = new ArrayList<>();
        for (int i = 0; i < pinGroups.size(); i++) {
            List<Station> group = pinGroups.get(i);
            Station first = group.get(0);
            Station.Status status = Station.pinStatus(group);
            String icon = status.name();
            if (group.size() > 1) {
                icon = "count-" + group.size() + "-" + status.name();
                if (style.getImage(icon) == null) {
                    style.addImage(icon, badge(Integer.toString(group.size()), statusColor(status)));
                }
            }
            Feature pin = Feature.fromGeometry(Point.fromLngLat(first.longitude, first.latitude));
            pin.addStringProperty(PIN_STATION_ID, first.id);
            pin.addStringProperty(PIN_IMAGE, icon);
            pins.add(pin);
        }
        GeoJsonSource source = style.getSourceAs(STATIONS);
        if (source == null) {
            style.addSource(new GeoJsonSource(STATIONS, FeatureCollection.fromFeatures(pins)));
            style.addLayer(new SymbolLayer(STATIONS, STATIONS).withProperties(
                    iconImage(get(PIN_IMAGE)), iconAllowOverlap(true), iconIgnorePlacement(true)));
        } else {
            source.setGeoJson(FeatureCollection.fromFeatures(pins));
        }
    }

    private int statusColor(Station.Status status) {
        switch (status) {
            case AVAILABLE: return 0xff176145;
            case REPORTED: return 0xff996000;
            case OUT_OF_SERVICE: return 0xffa62f36;
            default: return 0xff4c5562;
        }
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
        List<Station> group = Station.findPinGroup(pinGroups,
                hits.get(0).getStringProperty(PIN_STATION_ID));
        if (group.isEmpty()) return false;
        showDetails(group);
        return true;
    }

    private void renderList() {
        List<Station> ordered = new ArrayList<>(visibleStations());
        ordered.sort(userLocation == null ? Station.byName()
                : Station.byDistance(userLocation.getLatitude(), userLocation.getLongitude()));
        stationList.removeAllViews();
        TextView summary = requireView().findViewById(R.id.m2_filter_summary);
        List<String> filters = new ArrayList<>();
        if (facultyFilter != null) filters.add(getString(R.string.m2_faculty_detail, facultyFilter));
        if (collegeFilter != null) filters.add(getString(R.string.m2_college_detail, collegeFilter));
        if (waterFilter != null) filters.add(getString(R.string.m2_water_detail, waterLabel(waterFilter)));
        boolean noMatches = ordered.isEmpty() && !stations.isEmpty();
        String filterText = android.text.TextUtils.join(" · ", filters);
        summary.setText(noMatches ? getString(R.string.m2_no_matches) : filterText);
        summary.setVisibility(filters.isEmpty() && !noMatches ? View.GONE : View.VISIBLE);
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
        if (detailsDialog != null) detailsDialog.dismiss();
        detailStationIds.clear();
        for (Station station : group) detailStationIds.add(station.id);
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        detailsDialog = dialog;
        dialog.setOnDismissListener(ignored -> {
            stopReportUpdates();
            if (detailsDialog == dialog) detailsDialog = null;
        });
        View sheet = getLayoutInflater().inflate(R.layout.m2_station_sheet, null);
        LinearLayout details = sheet.findViewById(R.id.m2_station_details);
        populateDetails(details, group);
        ViewCompat.setOnApplyWindowInsetsListener(sheet, (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars()
                            | androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, 0, bars.right, bars.bottom);
            return insets;
        });
        dialog.setContentView(sheet);
        dialog.show();
        ViewCompat.requestApplyInsets(sheet);
    }

    private void populateDetails(LinearLayout details, List<Station> group) {
        stopReportUpdates();
        displayedStations = new ArrayList<>(group);
        if (group.size() > 1) {
            addDetail(details, getResources().getQuantityString(R.plurals.m2_station_count,
                    group.size(), group.size()), true);
        }
        for (Station station : group) {
            addDetail(details, station.name, true);
            addDetail(details, stationPosition(station), false);
            addDetail(details, getString(statusLabel(station.status)), false);
            addDetail(details, station.landmark, false);
            if (!station.faculty.isEmpty()) addDetail(details,
                    getString(R.string.m2_faculty_detail, station.faculty), false);
            if (!station.college.isEmpty()) addDetail(details,
                    getString(R.string.m2_college_detail, station.college), false);
            List<String> types = new ArrayList<>();
            for (Station.WaterType type : station.waterTypes) types.add(waterLabel(type));
            addDetail(details, getString(R.string.m2_water_detail, types.isEmpty()
                    ? getString(R.string.m2_not_recorded) : android.text.TextUtils.join(", ", types)), false);
            addDetail(details, getString(R.string.m2_cleaned_detail, dateLabel(station.lastCleanedAt)), false);
            addDetail(details, getString(R.string.m2_filter_changed_detail, dateLabel(station.lastFilterChangedAt)), false);
            MaterialButton directions = new MaterialButton(requireContext());
            directions.setText(R.string.m2_walking_directions);
            directions.setOnClickListener(view -> {
                try {
                    startActivity(walkingDirections(station));
                } catch (ActivityNotFoundException unavailable) {
                    Snackbar.make(view, R.string.m2_directions_unavailable, Snackbar.LENGTH_LONG).show();
                }
            });
            details.addView(directions);
            addDetail(details, getString(R.string.m2_open_reports), true);
            LinearLayout reports = new LinearLayout(requireContext());
            reports.setOrientation(LinearLayout.VERTICAL);
            details.addView(reports);
            if (demoMode) addDetail(reports, getString(R.string.m2_reports_demo), false);
            else startReportUpdates(station.id, reports);
            if (!demoMode) {
                MaterialButton report = new MaterialButton(requireContext());
                report.setText(R.string.m2_report_issue);
                report.setContentDescription(getString(R.string.m2_report_issue_for_station, station.name));
                report.setOnClickListener(view -> {
                    try {
                        startActivity(StationReportContract.forStation(requireContext(), station.id));
                        detailsDialog.dismiss();
                    } catch (ActivityNotFoundException unavailable) {
                        Snackbar.make(view, R.string.m2_reporting_unavailable, Snackbar.LENGTH_LONG).show();
                    }
                });
                details.addView(report);
            }
        }
    }

    private void addDetail(LinearLayout details, String value, boolean heading) {
        TextView line = new TextView(requireContext());
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
            case REPORTED: return R.string.m2_reported;
            case OUT_OF_SERVICE: return R.string.m2_out_of_service;
            default: return R.string.m2_unknown;
        }
    }

    static Intent walkingDirections(Station station) {
        Uri uri = Uri.parse("https://www.google.com/maps/dir/").buildUpon()
                .appendQueryParameter("api", "1")
                .appendQueryParameter("destination", station.latitude + "," + station.longitude)
                .appendQueryParameter("travelmode", "walking").build();
        return new Intent(Intent.ACTION_VIEW, uri);
    }

    private String dateLabel(java.util.Date date) {
        return date == null ? getString(R.string.m2_not_recorded)
                : android.text.format.DateFormat.getMediumDateFormat(requireContext()).format(date);
    }

    private String waterLabel(Station.WaterType type) {
        switch (type) {
            case COLD: return getString(R.string.m2_cold);
            case HOT: return getString(R.string.m2_hot);
            default: return getString(R.string.m2_filtered);
        }
    }

    private void stopReportUpdates() {
        ++reportRequest;
        for (ListenerRegistration subscription : reportSubscriptions) subscription.remove();
        reportSubscriptions.clear();
    }

    private void startReportUpdates(String stationId, LinearLayout container) {
        int request = reportRequest;
        container.removeAllViews();
        addDetail(container, getString(R.string.m2_loading_reports), false);
        reportSubscriptions.add(new StationRepository().listenReports(stationId, new StationRepository.ReportCallback() {
            @Override public void onReports(List<StationReport> reports, boolean fromCache) {
                if (request != reportRequest || getView() == null) return;
                container.removeAllViews();
                if (fromCache) addDetail(container, getString(reports.isEmpty()
                        ? R.string.m2_no_saved_reports : R.string.m2_reports_saved), false);
                else if (reports.isEmpty()) addDetail(container, getString(R.string.m2_no_open_reports), false);
                for (StationReport report : reports) {
                    addDetail(container, getString(R.string.m2_report_summary,
                            getString(report.status == StationReport.Status.OPEN
                                    ? R.string.m2_report_open : R.string.m2_report_in_progress),
                            dateLabel(report.createdAt), report.description), false);
                }
            }
            @Override public void onFailure() {
                if (request != reportRequest || getView() == null) return;
                container.removeAllViews();
                addDetail(container, getString(R.string.m2_reports_unavailable), false);
                MaterialButton retryReports = new MaterialButton(requireContext());
                retryReports.setText(R.string.m2_retry);
                retryReports.setOnClickListener(view -> {
                    // Retry the sheet subscriptions together so old callbacks cannot replace new results.
                    if (detailsDialog != null) {
                        stopReportUpdates();
                        refreshDirectory();
                    }
                });
                container.addView(retryReports);
            }
        }));
    }

    private void showFilters() {
        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        form.setPadding(padding, 0, padding, 0);
        List<String> faculties = categoryValues(true);
        List<String> colleges = categoryValues(false);
        Spinner faculty = filterSpinner(form, R.string.m2_faculty, faculties, facultyFilter);
        Spinner college = filterSpinner(form, R.string.m2_college, colleges, collegeFilter);
        List<String> waters = new ArrayList<>();
        for (Station.WaterType type : Station.WaterType.values()) waters.add(waterLabel(type));
        Spinner water = filterSpinner(form, R.string.m2_water_type, waters,
                waterFilter == null ? null : waterLabel(waterFilter));
        android.widget.ScrollView scroll = new android.widget.ScrollView(requireContext());
        scroll.addView(form);
        new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.m2_filters).setView(scroll)
                .setPositiveButton(R.string.m2_apply, (dialog, which) -> {
                    facultyFilter = faculty.getSelectedItemPosition() == 0 ? null
                            : faculties.get(faculty.getSelectedItemPosition() - 1);
                    collegeFilter = college.getSelectedItemPosition() == 0 ? null
                            : colleges.get(college.getSelectedItemPosition() - 1);
                    waterFilter = water.getSelectedItemPosition() == 0 ? null
                            : Station.WaterType.values()[water.getSelectedItemPosition() - 1];
                    refreshDirectory();
                }).setNeutralButton(R.string.m2_clear_filters, (dialog, which) -> {
                    facultyFilter = null;
                    collegeFilter = null;
                    waterFilter = null;
                    refreshDirectory();
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private List<String> categoryValues(boolean faculty) {
        java.util.Set<String> values = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Station station : stations) {
            String value = faculty ? station.faculty : station.college;
            if (!value.isEmpty()) values.add(value);
        }
        String selected = faculty ? facultyFilter : collegeFilter;
        if (selected != null) values.add(selected);
        return new ArrayList<>(values);
    }

    private Spinner filterSpinner(LinearLayout form, int label, List<String> values, String selected) {
        TextView title = new TextView(requireContext());
        title.setText(label);
        form.addView(title);
        Spinner spinner = new Spinner(requireContext());
        spinner.setId(View.generateViewId());
        title.setLabelFor(spinner.getId());
        List<String> choices = new ArrayList<>();
        choices.add(getString(R.string.m2_all));
        choices.addAll(values);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selected == null ? 0 : choices.indexOf(selected));
        spinner.setMinimumHeight(Math.round(48 * getResources().getDisplayMetrics().density));
        form.addView(spinner);
        return spinner;
    }

    private void showNearestWorking() {
        Station nearest = Station.nearestWorking(visibleStations(), userLocation.getLatitude(), userLocation.getLongitude());
        if (nearest == null) {
            showLocationMessage(R.string.m2_no_working);
            return;
        }
        if (map != null) {
            initialCenter = false;
            center(new LatLng(nearest.latitude, nearest.longitude));
        }
        showDetails(Collections.singletonList(nearest));
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocation(boolean recenter) {
        if (hasLocationPermission()) {
            findLocation(recenter);
        } else if (recenter && requireActivity().getPreferences(Context.MODE_PRIVATE).getBoolean(LOCATION_ASKED, false)
                && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
                && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            pendingNearest = false;
            Snackbar.make(requireView(), R.string.m2_location_settings, Snackbar.LENGTH_LONG)
                    .setAction(R.string.m2_settings, view -> startActivity(new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", requireContext().getPackageName(), null))))
                    .show();
        } else {
            requireActivity().getPreferences(Context.MODE_PRIVATE).edit().putBoolean(LOCATION_ASKED, true).apply();
            recenterAfterPermission = recenter;
            requestPermissions.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    @SuppressLint("MissingPermission")
    private void findLocation(boolean recenter) {
        cancelLocation();
        LocationManager manager = requireContext().getSystemService(LocationManager.class);
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
                    request, ContextCompat.getMainExecutor(requireContext()), location -> {
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
        if (pendingNearest) {
            pendingNearest = false;
            if (location != null) {
                showNearestWorking();
                return;
            }
        }
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
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
        scheduleMapTimeout();
        if (!demoMode) startStationUpdates();
        if (detailsDialog != null && detailsDialog.isShowing()) refreshDirectory();
        if (hasLocationPermission() || !requireActivity().getPreferences(Context.MODE_PRIVATE).getBoolean(LOCATION_ASKED, false)) {
            if (locationRequest == null) requestLocation(false);
        } else if (userLocation != null) {
            receiveLocation(null, false);
        }
    }

    @Override public void onResume() { super.onResume(); mapView.onResume(); }
    @Override public void onPause() { mapView.onPause(); super.onPause(); }

    @Override
    public void onStop() {
        stopStationUpdates();
        stopReportUpdates();
        pendingNearest = false;
        cancelLocation();
        handler.removeCallbacks(mapTimeout);
        mapView.onStop();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        stopReportUpdates();
        handler.removeCallbacksAndMessages(null);
        if (detailsDialog != null) {
            detailsDialog.dismiss();
            detailsDialog = null;
        }
        detailStationIds.clear();
        savedMapState = new Bundle();
        mapView.onSaveInstanceState(savedMapState);
        saveFilters(savedMapState);
        mapView.onDestroy();
        mapView = null;
        map = null;
        super.onDestroyView();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        saveFilters(state);
        if (mapView != null) mapView.onSaveInstanceState(state);
        else if (savedMapState != null) state.putAll(savedMapState);
    }

    private void saveFilters(Bundle state) {
        state.putString("m2_faculty", facultyFilter);
        state.putString("m2_college", collegeFilter);
        state.putString("m2_water", waterFilter == null ? null : waterFilter.name());
    }
}
