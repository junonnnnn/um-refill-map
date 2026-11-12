package com.example.umborrowbuddy.features.module2;

import android.view.View;
import android.os.ParcelFileDescriptor;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.lifecycle.Lifecycle;

import com.example.umborrowbuddy.R;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.io.FileInputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapView;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class StationDirectoryFragmentTest {
    @Before
    public void allowApproximateLocationWithoutAConfirmationDialog() throws Exception {
        try (ParcelFileDescriptor output = InstrumentationRegistry.getInstrumentation()
                .getUiAutomation().executeShellCommand("pm grant com.example.umborrowbuddy "
                        + "android.permission.ACCESS_COARSE_LOCATION");
                FileInputStream input = new FileInputStream(output.getFileDescriptor())) {
            byte[] buffer = new byte[1024];
            while (input.read(buffer) != -1) { }
        }
    }

    @Test
    public void directoryIsHostedOnceAndRestoresListAfterRecreation() {
        try (ActivityScenario<StationDirectoryActivity> scenario =
                ActivityScenario.launch(StationDirectoryActivity.class)) {
            scenario.onActivity(activity -> {
                assertEquals(1, activity.getSupportFragmentManager().getFragments().size());
                activity.findViewById(R.id.m2_list_tab).performClick();
                assertEquals(View.VISIBLE, activity.findViewById(R.id.m2_list_panel).getVisibility());
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertEquals(1, activity.getSupportFragmentManager().getFragments().size());
                assertEquals(View.VISIBLE, activity.findViewById(R.id.m2_list_panel).getVisibility());
                assertEquals(View.INVISIBLE, activity.findViewById(R.id.m2_map_panel).getVisibility());
            });
        }
    }

    @Test
    public void directoryRestoresSelectionAfterAccessIsRechecked() {
        try (ActivityScenario<StationDirectoryActivity> scenario =
                ActivityScenario.launch(StationDirectoryActivity.class)) {
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.m2_list_tab).performClick();
                androidx.fragment.app.Fragment fragment = activity.getSupportFragmentManager()
                        .getFragments().get(0);
                activity.getSupportFragmentManager().beginTransaction()
                        .setMaxLifecycle(fragment, Lifecycle.State.CREATED).commitNow();
                assertEquals(null, fragment.getView());
                activity.getSupportFragmentManager().beginTransaction()
                        .setMaxLifecycle(fragment, Lifecycle.State.RESUMED).commitNow();
                assertEquals(View.VISIBLE, activity.findViewById(R.id.m2_list_panel).getVisibility());
            });
        }
    }

    @Test
    public void cameraSurvivesProfileRevalidation() throws Exception {
        try (ActivityScenario<StationDirectoryActivity> scenario =
                ActivityScenario.launch(StationDirectoryActivity.class)) {
            scenario.recreate();
            CountDownLatch positioned = new CountDownLatch(1);
            scenario.onActivity(activity -> ((MapView) activity.findViewById(R.id.m2_map_view))
                    .getMapAsync(map -> {
                        map.setCameraPosition(new CameraPosition.Builder()
                                .target(new LatLng(3.13, 101.651)).zoom(17.2).bearing(20).build());
                        positioned.countDown();
                    }));
            assertTrue(positioned.await(10, TimeUnit.SECONDS));
            CountDownLatch restored = new CountDownLatch(1);
            AtomicReference<CameraPosition> actual = new AtomicReference<>();
            scenario.onActivity(activity -> {
                androidx.fragment.app.Fragment fragment = activity.getSupportFragmentManager()
                        .getFragments().get(0);
                activity.getSupportFragmentManager().beginTransaction()
                        .setMaxLifecycle(fragment, Lifecycle.State.CREATED).commitNow();
                activity.getSupportFragmentManager().beginTransaction()
                        .setMaxLifecycle(fragment, Lifecycle.State.RESUMED).commitNow();
                ((MapView) activity.findViewById(R.id.m2_map_view)).getMapAsync(map -> {
                    actual.set(map.getCameraPosition());
                    restored.countDown();
                });
            });
            assertTrue(restored.await(10, TimeUnit.SECONDS));
            assertEquals(3.13, actual.get().target.getLatitude(), 0.000001);
            assertEquals(101.651, actual.get().target.getLongitude(), 0.000001);
            assertEquals(17.2, actual.get().zoom, 0.001);
            assertEquals(20, actual.get().bearing, 0.001);
        }
    }
}
