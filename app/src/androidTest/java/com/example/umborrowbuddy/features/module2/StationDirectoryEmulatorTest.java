package com.example.umborrowbuddy.features.module2;

import android.Manifest;
import android.content.Intent;
import android.app.Activity;
import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.inspector.WindowInspector;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.MonitoringInstrumentation;
import androidx.test.filters.SdkSuppress;

import com.example.umborrowbuddy.MainActivity;
import com.example.umborrowbuddy.R;
import com.example.umborrowbuddy.features.module1.auth.LoginActivity;
import com.example.umborrowbuddy.features.module1.profile.ProfileActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
public class StationDirectoryEmulatorTest {
    private static FirebaseAuth auth;
    private static FirebaseFirestore firestore;
    private final MonitoringInstrumentation instrumentation = (MonitoringInstrumentation)
            InstrumentationRegistry.getInstrumentation();

    @BeforeClass
    public static void connectOnlyToLocalEmulators() {
        assumeTrue("Start and seed the local emulators, then pass m2EmulatorTests=true",
                "true".equals(InstrumentationRegistry.getArguments().getString("m2EmulatorTests")));
        assertEquals("um-borrow-buddy", FirebaseApp.getInstance().getOptions().getProjectId());
        auth = FirebaseAuth.getInstance();
        auth.useEmulator("127.0.0.1", 9098);
        firestore = FirebaseFirestore.getInstance();
        firestore.setFirestoreSettings(new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false).build());
        firestore.useEmulator("127.0.0.1", 8085);
        assertEquals("127.0.0.1:8085", firestore.getFirestoreSettings().getHost());
    }

    @Before
    public void signInToTheLocalReporterFixture() throws Exception {
        instrumentation.useDefaultInterceptingActivityFactory();
        instrumentation.getUiAutomation().grantRuntimePermission("com.example.umborrowbuddy",
                Manifest.permission.ACCESS_COARSE_LOCATION);
        Tasks.await(auth.signInWithEmailAndPassword("m2-reporter@siswa.um.edu.my",
                "M2-local-test-Password1"), 15, TimeUnit.SECONDS);
        assertTrue(auth.getCurrentUser().isEmailVerified());
    }

    @After
    public void signOutAndRestoreLocalFixture() throws Exception {
        auth.signOut();
        Tasks.await(firestore.enableNetwork(), 10, TimeUnit.SECONDS);
        patchField("fsktm-001", "status", "UNKNOWN");
        for (String id : new String[]{"fsktm-001", "fsktm-002", "fsktm-003"}) {
            patchField(id, "faculty", "FSKTM");
        }
    }

    @Test
    public void repositoryReceivesServerRecordsAndConfirmedStatusChanges() throws Exception {
        CountDownLatch loaded = new CountDownLatch(1);
        CountDownLatch changed = new CountDownLatch(1);
        AtomicReference<List<Station>> latest = new AtomicReference<>();
        AtomicBoolean failed = new AtomicBoolean();
        ListenerRegistration registration = new StationRepository(firestore).listen(new StationRepository.Callback() {
            @Override
            public void onStations(List<Station> stations, boolean fromCache) {
                if (fromCache) return;
                latest.set(stations);
                loaded.countDown();
                if (stations.stream().anyMatch(station -> station.id.equals("fsktm-001")
                        && station.status == Station.Status.AVAILABLE)) changed.countDown();
            }

            @Override
            public void onFailure() {
                failed.set(true);
                loaded.countDown();
                changed.countDown();
            }
        });
        try {
            assertTrue(loaded.await(15, TimeUnit.SECONDS));
            assertFalse(failed.get());
            assertEquals(3, latest.get().size());
            patchField("fsktm-001", "status", "AVAILABLE");
            assertTrue(changed.await(15, TimeUnit.SECONDS));
            assertFalse(failed.get());
        } finally {
            registration.remove();
        }
    }

    @Test
    public void permissionDenialIsReportedAsAnError() throws Exception {
        auth.signOut();
        CountDownLatch denied = new CountDownLatch(1);
        ListenerRegistration registration = new StationRepository(firestore).listen(new StationRepository.Callback() {
            @Override public void onStations(List<Station> stations, boolean fromCache) { }
            @Override public void onFailure() { denied.countDown(); }
        });
        try {
            assertTrue(denied.await(15, TimeUnit.SECONDS));
        } finally {
            registration.remove();
        }
    }

    @Test
    public void offlineDirectoryUsesSavedStationsWithCacheMetadata() throws Exception {
        CountDownLatch loaded = new CountDownLatch(1);
        CountDownLatch cached = new CountDownLatch(1);
        AtomicBoolean offline = new AtomicBoolean();
        ListenerRegistration registration = new StationRepository(firestore).listen(new StationRepository.Callback() {
            @Override
            public void onStations(List<Station> stations, boolean fromCache) {
                if (stations.size() == 3) {
                    if (!fromCache) loaded.countDown();
                    else if (offline.get()) cached.countDown();
                }
            }

            @Override public void onFailure() { }
        });
        try {
            assertTrue(loaded.await(15, TimeUnit.SECONDS));
            offline.set(true);
            Tasks.await(firestore.disableNetwork(), 10, TimeUnit.SECONDS);
            assertTrue(cached.await(10, TimeUnit.SECONDS));
        } finally {
            registration.remove();
        }
    }

    @Test
    public void openedDetailsFollowConfirmedStatusChanges() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitDirectory(scenario, 3, 0);
            openEntranceSheet(scenario);
            patchField("fsktm-001", "status", "AVAILABLE");
            awaitSheetText("✓ Working");
        }
    }

    @Test
    public void reportingUsesTheSelectedStationAndHandlesTheMissingModule() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitDirectory(scenario, 3, 0);
            openEntranceSheet(scenario);
            clickWindowText("Report an issue");
            awaitWindowText(0, "Issue reporting isn’t available yet.");
            AtomicReference<Intent> reportIntent = new AtomicReference<>();
            Instrumentation.ActivityMonitor reportMonitor = new Instrumentation.ActivityMonitor() {
                @Override
                public Instrumentation.ActivityResult onStartActivity(Intent intent) {
                    if ("com.example.umborrowbuddy.action.REPORT_ISSUE".equals(intent.getAction())) {
                        reportIntent.set(intent);
                        return new Instrumentation.ActivityResult(Activity.RESULT_OK, null);
                    }
                    return null;
                }
            };
            instrumentation.addMonitor(reportMonitor);
            try {
                clickWindowText("Report an issue");
                assertNotNull(reportIntent.get());
                assertEquals("com.example.umborrowbuddy", reportIntent.get().getPackage());
                assertEquals("fsktm-001", reportIntent.get()
                        .getStringExtra("com.example.umborrowbuddy.extra.STATION_ID"));
            } finally {
                instrumentation.removeMonitor(reportMonitor);
            }
        }
    }

    @Test
    public void realReporterProfileReturnAndLogoutKeepTheMapGuarded() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitDirectory(scenario, 3, 0);
            scenario.onActivity(activity -> activity.findViewById(R.id.m2_list_tab).performClick());
            Bitmap screenshot = instrumentation.getUiAutomation().takeScreenshot();
            assertNotNull(screenshot);
            try (FileOutputStream output = new FileOutputStream(new File(
                    instrumentation.getTargetContext().getCacheDir(), "m2_reporter_emulator.png"))) {
                assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output));
            } finally {
                screenshot.recycle();
            }
            Instrumentation.ActivityMonitor profileMonitor = instrumentation.addMonitor(
                    ProfileActivity.class.getName(), null, false);
            try {
                scenario.onActivity(activity -> activity.findViewById(R.id.m1_main_profile_btn).performClick());
                Activity profile = instrumentation.waitForMonitorWithTimeout(profileMonitor, 10000);
                assertNotNull(profile);
                instrumentation.runOnMainSync(profile::finish);
                awaitDirectory(scenario, 3, 0);
                scenario.onActivity(activity -> assertEquals(View.VISIBLE,
                        activity.findViewById(R.id.m2_list_panel).getVisibility()));
            } finally {
                instrumentation.removeMonitor(profileMonitor);
            }
            Instrumentation.ActivityMonitor loginMonitor = instrumentation.addMonitor(
                    LoginActivity.class.getName(), null, false);
            try {
                scenario.onActivity(activity -> activity.findViewById(R.id.m1_main_logout_btn).performClick());
                Activity login = instrumentation.waitForMonitorWithTimeout(loginMonitor, 10000);
                assertNotNull(login);
                assertNull(auth.getCurrentUser());
                instrumentation.runOnMainSync(login::finish);
            } finally {
                instrumentation.removeMonitor(loginMonitor);
            }
        }
    }

    @Test
    public void emptyInvalidAndCachedStatesAreVisibleAndRecover() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitDirectory(scenario, 3, 0);
            java.util.Map<String, String> saved = new java.util.HashMap<>();
            for (String id : new String[]{"fsktm-001", "fsktm-002", "fsktm-003"}) {
                String path = "refill_stations/" + id;
                saved.put(path, "{\"fields\":" + new org.json.JSONObject(adminRequest("GET", path, null))
                        .getJSONObject("fields").toString() + "}");
                adminRequest("DELETE", path, null);
            }
            try {
                awaitDirectory(scenario, 0, R.string.m2_no_stations);
            } finally {
                for (java.util.Map.Entry<String, String> entry : saved.entrySet()) {
                    adminRequest("PATCH", entry.getKey(), entry.getValue());
                }
            }
            awaitDirectory(scenario, 3, 0);
            patchField("fsktm-001", "status", "INVALID");
            awaitDirectory(scenario, 0, R.string.m2_data_unavailable);
            scenario.onActivity(activity -> activity.findViewById(R.id.m2_data_retry).performClick());
            patchField("fsktm-001", "status", "UNKNOWN");
            awaitDirectory(scenario, 3, 0);
            Tasks.await(firestore.disableNetwork(), 10, TimeUnit.SECONDS);
            awaitDirectory(scenario, 3, R.string.m2_saved_stations);
            Tasks.await(firestore.enableNetwork(), 10, TimeUnit.SECONDS);
            awaitDirectory(scenario, 3, 0);
        }
    }

    @Test
    public void campusFiltersPersistAndDirectionsUseSelectedStation() throws Exception {
        String path = "refill_stations/campus-test";
        adminRequest("PATCH", path, "{\"fields\":{"
                + "\"name\":{\"stringValue\":\"Synthetic college station\"},"
                + "\"building\":{\"stringValue\":\"Fixture building\"},"
                + "\"floor\":{\"stringValue\":\"Ground\"},"
                + "\"landmark\":{\"stringValue\":\"Fixture entrance\"},"
                + "\"college\":{\"stringValue\":\"College 1\"},"
                + "\"status\":{\"stringValue\":\"REPORTED\"},"
                + "\"waterTypes\":{\"arrayValue\":{\"values\":[{\"stringValue\":\"COLD\"},{\"stringValue\":\"FILTERED\"}]}},"
                + "\"lastCleanedAt\":{\"timestampValue\":\"2026-11-01T00:00:00Z\"},"
                + "\"lastFilterChangedAt\":{\"timestampValue\":\"2026-10-01T00:00:00Z\"},"
                + "\"location\":{\"geoPointValue\":{\"latitude\":3.12,\"longitude\":101.65}}}}");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitDirectory(scenario, 4, 0);
            scenario.onActivity(activity -> activity.findViewById(R.id.m2_filters).performClick());
            selectFilter(0, 1); // FSKTM only.
            clickWindowText("Apply");
            awaitDirectory(scenario, 3, 0);
            scenario.onActivity(activity -> activity.findViewById(R.id.m2_filters).performClick());
            selectFilter(1, 1); // Faculty + college have no intersection in this fixture.
            clickWindowText("Apply");
            awaitWindowText(0, "No stations match your filters.");
            scenario.onActivity(activity -> activity.findViewById(R.id.m2_filters).performClick());
            clickWindowText("Clear filters");
            awaitDirectory(scenario, 4, 0);
            scenario.onActivity(activity -> activity.findViewById(R.id.m2_filters).performClick());
            selectFilter(1, 1); // Residential college, first non-All value.
            selectFilter(2, 1); // Cold.
            clickWindowText("Apply");
            awaitDirectory(scenario, 1, 0);
            scenario.recreate();
            awaitDirectory(scenario, 1, 0);
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.m2_list_tab).performClick();
                ((LinearLayout) activity.findViewById(R.id.m2_station_list)).getChildAt(0).performClick();
            });
            awaitSheetText("! Reported");
            awaitSheetText("Water: Cold, Filtered");
            java.text.DateFormat dates = android.text.format.DateFormat.getMediumDateFormat(instrumentation.getTargetContext());
            awaitSheetText("Last cleaned: " + dates.format(new java.util.Date(1793491200000L)));
            awaitSheetText("Filter changed: " + dates.format(new java.util.Date(1790812800000L)));
            awaitSheetText("No open reports.");
            saveScreenshot("m2_campus_details.png");
            AtomicReference<Intent> directions = new AtomicReference<>();
            Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor() {
                @Override public Instrumentation.ActivityResult onStartActivity(Intent intent) {
                    if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                        directions.set(intent);
                        return new Instrumentation.ActivityResult(Activity.RESULT_OK, null);
                    }
                    return null;
                }
            };
            instrumentation.addMonitor(monitor);
            try {
                clickWindowText("Walking directions");
                assertNotNull(directions.get());
                assertEquals("https", directions.get().getData().getScheme());
                assertEquals("www.google.com", directions.get().getData().getHost());
                assertEquals("1", directions.get().getData().getQueryParameter("api"));
                assertEquals("walking", directions.get().getData().getQueryParameter("travelmode"));
                assertEquals("3.12,101.65", directions.get().getData().getQueryParameter("destination"));
            } finally {
                instrumentation.removeMonitor(monitor);
            }
        } finally {
            adminRequest("DELETE", path, null);
        }
    }

    @Test
    public void openReportsRefreshWithoutShowingClosedReports() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitDirectory(scenario, 3, 0);
            openEntranceSheet(scenario);
            awaitReportContaining("Local fixture: low pressure");
            adminRequest("PATCH", "maintenance_reports/local-open?updateMask.fieldPaths=status",
                    "{\"fields\":{\"status\":{\"stringValue\":\"RESOLVED\"}}}");
            awaitSheetText("No open reports.");
            instrumentation.runOnMainSync(() -> {
                for (View window : WindowInspector.getGlobalWindowViews()) {
                    assertNull(findText(window, "Closed fixture hidden"));
                }
            });
        } finally {
            adminRequest("PATCH", "maintenance_reports/local-open?updateMask.fieldPaths=status",
                    "{\"fields\":{\"status\":{\"stringValue\":\"OPEN\"}}}");
        }
    }

    @Test
    public void nearestWorkingExcludesCloserReportedStationAndShowsMissingMetadata() throws Exception {
        patchField("fsktm-001", "status", "AVAILABLE");
        patchField("fsktm-002", "status", "REPORTED");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitDirectory(scenario, 3, 0);
            scenario.onActivity(activity -> {
                StationDirectoryFragment fragment = (StationDirectoryFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.m2_directory_host);
                assertNotNull(fragment);
                try {
                    // Deliver a deterministic location at the reported station through the normal callback path.
                    activity.findViewById(R.id.m2_nearest).performClick();
                    java.lang.reflect.Method receive = StationDirectoryFragment.class.getDeclaredMethod(
                            "receiveLocation", android.location.Location.class, boolean.class);
                    receive.setAccessible(true);
                    android.location.Location location = new android.location.Location("test");
                    location.setLatitude(3.128488);
                    location.setLongitude(101.650050);
                    receive.invoke(fragment, location, true);
                } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            });
            awaitSheetText("Block B Entrance");
            awaitSheetText("✓ Working");
            awaitSheetText("Water: Not recorded");
            awaitSheetText("Last cleaned: Not recorded");
            awaitSheetText("Filter changed: Not recorded");
        } finally {
            patchField("fsktm-002", "status", "UNKNOWN");
        }
    }

    @Test
    public void reportErrorsRetryAndOfflineNeverClaimsAuthoritativeEmpty() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitDirectory(scenario, 3, 0);
            openEntranceSheet(scenario);
            awaitReportContaining("Local fixture: low pressure");
            adminRequest("PATCH", "maintenance_reports/local-open?updateMask.fieldPaths=status",
                    "{\"fields\":{\"status\":{\"stringValue\":\"INVALID\"}}}");
            awaitSheetText("Reports unavailable.");
            adminRequest("PATCH", "maintenance_reports/local-open?updateMask.fieldPaths=status",
                    "{\"fields\":{\"status\":{\"stringValue\":\"OPEN\"}}}");
            clickWindowText("Retry");
            awaitReportContaining("Local fixture: low pressure");
            Tasks.await(firestore.disableNetwork(), 10, TimeUnit.SECONDS);
            awaitSheetText("Showing saved reports.");
            Tasks.await(firestore.enableNetwork(), 10, TimeUnit.SECONDS);
        } finally {
            adminRequest("PATCH", "maintenance_reports/local-open?updateMask.fieldPaths=status",
                    "{\"fields\":{\"status\":{\"stringValue\":\"OPEN\"}}}");
        }
    }

    private void saveScreenshot(String filename) throws Exception {
        Bitmap screenshot = instrumentation.getUiAutomation().takeScreenshot();
        assertNotNull(screenshot);
        try (FileOutputStream output = new FileOutputStream(new File(
                instrumentation.getTargetContext().getCacheDir(), filename))) {
            assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally { screenshot.recycle(); }
    }

    private static void selectFilter(int index, int selection) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            java.util.List<android.widget.Spinner> spinners = new java.util.ArrayList<>();
            for (View window : WindowInspector.getGlobalWindowViews()) collectSpinners(window, spinners);
            assertEquals(3, spinners.size());
            spinners.get(index).setSelection(selection);
        });
    }

    private static void collectSpinners(View root, java.util.List<android.widget.Spinner> result) {
        if (root instanceof android.widget.Spinner) result.add((android.widget.Spinner) root);
        else if (root instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++)
                collectSpinners(((ViewGroup) root).getChildAt(i), result);
        }
    }

    private static void awaitReportContaining(String description) {
        long deadline = SystemClock.elapsedRealtime() + 10000;
        AtomicBoolean found = new AtomicBoolean();
        do {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for (View window : WindowInspector.getGlobalWindowViews()) {
                    if (containsText(window.findViewById(R.id.m2_station_details), description)) found.set(true);
                }
            });
            if (found.get()) return;
            SystemClock.sleep(100);
        } while (SystemClock.elapsedRealtime() < deadline);
        assertTrue("Report not displayed", found.get());
    }

    private static boolean containsText(View root, String text) {
        if (root instanceof TextView && ((TextView) root).getText().toString().contains(text)) return true;
        if (root instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++)
                if (containsText(((ViewGroup) root).getChildAt(i), text)) return true;
        }
        return false;
    }

    private static String adminRequest(String method, String path, String body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:8085/v1/projects/"
                + "um-borrow-buddy/databases/(default)/documents/" + path).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Authorization", "Bearer owner");
            connection.setRequestProperty("Content-Type", "application/json");
            if (body != null) {
                connection.setDoOutput(true);
                try (java.io.OutputStream output = connection.getOutputStream()) {
                    output.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            assertEquals(200, connection.getResponseCode());
            try (java.io.InputStream input = connection.getInputStream();
                    java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                return output.toString("UTF-8");
            }
        } finally { connection.disconnect(); }
    }

    private static void awaitDirectory(ActivityScenario<MainActivity> scenario, int rows, int message) {
        AtomicBoolean ready = new AtomicBoolean();
        long deadline = SystemClock.elapsedRealtime() + 15000;
        do {
            scenario.onActivity(activity -> {
                LinearLayout list = activity.findViewById(R.id.m2_station_list);
                TextView state = activity.findViewById(R.id.m2_data_error);
                ready.set(list != null && list.getChildCount() == rows && state != null
                        && (message == 0 ? state.getVisibility() == View.GONE
                        : state.getText().toString().equals(activity.getString(message))));
            });
            if (ready.get()) return;
            SystemClock.sleep(100);
        } while (SystemClock.elapsedRealtime() < deadline);
        assertTrue("Directory did not reach the expected station state", ready.get());
    }

    private static void openEntranceSheet(ActivityScenario<MainActivity> scenario) {
        AtomicBoolean opened = new AtomicBoolean();
        scenario.onActivity(activity -> {
            activity.findViewById(R.id.m2_list_tab).performClick();
            LinearLayout list = activity.findViewById(R.id.m2_station_list);
            for (int i = 0; i < list.getChildCount(); i++) {
                TextView row = (TextView) list.getChildAt(i);
                if (row.getText().toString().startsWith("Block B Entrance\n")) {
                    row.performClick();
                    opened.set(true);
                    break;
                }
            }
        });
        assertTrue(opened.get());
    }

    private static void clickWindowText(String text) {
        AtomicBoolean clicked = new AtomicBoolean();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (View window : WindowInspector.getGlobalWindowViews()) {
                TextView match = findText(window, text);
                if (match != null && match.isShown()) {
                    match.performClick();
                    clicked.set(true);
                    break;
                }
            }
        });
        assertTrue("No clickable window text: " + text, clicked.get());
    }

    private static void patchField(String id, String field, String value) throws Exception {
        URL url = new URL("http://127.0.0.1:8085/v1/projects/um-borrow-buddy/databases/(default)"
                + "/documents/refill_stations/" + id + "?updateMask.fieldPaths=" + field);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("PATCH");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Authorization", "Bearer owner");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            byte[] body = ("{\"fields\":{\"" + field + "\":{\"stringValue\":\"" + value + "\"}}}")
                    .getBytes(StandardCharsets.UTF_8);
            try (java.io.OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }
            assertEquals(200, connection.getResponseCode());
            connection.getInputStream().close();
        } finally {
            connection.disconnect();
        }
    }

    private static void awaitSheetText(String text) {
        awaitWindowText(R.id.m2_station_details, text);
    }

    private static void awaitWindowText(int rootId, String text) {
        long deadline = SystemClock.elapsedRealtime() + 10000;
        AtomicBoolean found = new AtomicBoolean();
        do {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for (View window : WindowInspector.getGlobalWindowViews()) {
                    View root = rootId == 0 ? window : window.findViewById(rootId);
                    TextView match = findText(root, text);
                    if (match != null && match.isShown()) found.set(true);
                }
            });
            if (found.get()) return;
            SystemClock.sleep(100);
        } while (SystemClock.elapsedRealtime() < deadline);
        assertTrue("Window did not display: " + text, found.get());
    }

    private static TextView findText(View root, String text) {
        if (root instanceof TextView && text.contentEquals(((TextView) root).getText())) {
            return (TextView) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView match = findText(group.getChildAt(i), text);
                if (match != null) return match;
            }
        }
        return null;
    }
}
