package com.example.umborrowbuddy.features.module2;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.lifecycle.Lifecycle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.MonitoringInstrumentation;
import androidx.test.runner.intercepting.InterceptingActivityFactory;

import com.example.umborrowbuddy.MainActivity;
import com.example.umborrowbuddy.R;
import com.example.umborrowbuddy.features.module1.auth.AdminDashboardActivity;
import com.example.umborrowbuddy.features.module1.auth.AuthRepository;
import com.example.umborrowbuddy.features.module1.auth.LoginActivity;
import com.example.umborrowbuddy.features.module1.auth.StaffDashboardActivity;
import com.example.umborrowbuddy.features.module1.profile.ProfileActivity;
import com.example.umborrowbuddy.shared.model.UserProfile;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ReporterLandingTest {
    private final MonitoringInstrumentation instrumentation = (MonitoringInstrumentation)
            InstrumentationRegistry.getInstrumentation();
    private static final List<AuthRepository.Callback<UserProfile>> requests = new ArrayList<>();
    private static Intent destination;

    public static class TestMainActivity extends MainActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            getSupportFragmentManager().setFragmentFactory(new FragmentFactory() {
                @Override
                public Fragment instantiate(ClassLoader loader, String className) {
                    if (StationDirectoryFragment.class.getName().equals(className)) {
                        return StationDirectoryFragment.newDemoInstance();
                    }
                    return super.instantiate(loader, className);
                }
            });
            super.onCreate(savedInstanceState);
        }

        @Override
        protected void loadCurrentProfile(AuthRepository.Callback<UserProfile> callback) {
            requests.add(callback);
        }

        @Override
        public void startActivity(Intent intent) {
            destination = intent;
        }
    }

    @Before
    public void useControlledProfileResponses() throws Exception {
        requests.clear();
        destination = null;
        instrumentation.interceptActivityUsing(new InterceptingActivityFactory() {
            @Override
            public boolean shouldIntercept(ClassLoader loader, String className, Intent intent) {
                return MainActivity.class.getName().equals(className);
            }

            @Override
            public Activity create(ClassLoader loader, String className, Intent intent) {
                return new TestMainActivity();
            }
        });
        try (ParcelFileDescriptor output = instrumentation.getUiAutomation().executeShellCommand(
                "pm grant com.example.umborrowbuddy android.permission.ACCESS_COARSE_LOCATION");
                FileInputStream input = new FileInputStream(output.getFileDescriptor())) {
            byte[] buffer = new byte[1024];
            while (input.read(buffer) != -1) { }
        }
    }

    @After
    public void restoreActivityCreation() {
        instrumentation.useDefaultInterceptingActivityFactory();
    }

    @Test
    public void actualSignedOutReporterRedirectsToLogin() {
        instrumentation.useDefaultInterceptingActivityFactory();
        assertNull(FirebaseAuth.getInstance().getCurrentUser());
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                LoginActivity.class.getName(), null, false);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            Activity login = instrumentation.waitForMonitorWithTimeout(monitor, 10000);
            assertNotNull(login);
            instrumentation.waitForIdleSync();
            assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
            instrumentation.runOnMainSync(login::finish);
        } finally {
            instrumentation.removeMonitor(monitor);
        }
    }

    @Test
    public void reporterWaitsForProfileAndKeepsProfileAction() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue(activity.getSupportFragmentManager().getFragments().isEmpty());
                requests.get(0).onSuccess(profile("REPORTER"));
                assertEquals(View.VISIBLE, activity.findViewById(R.id.m2_directory_host).getVisibility());
                assertEquals(1, activity.getSupportFragmentManager().getFragments().size());
                activity.findViewById(R.id.m1_main_profile_btn).performClick();
                assertEquals(ProfileActivity.class.getName(), destination.getComponent().getClassName());
            });
        }
    }

    @Test
    public void readFailureStaysClosedAndRetryCanRecover() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                requests.get(0).onFailure("Profile read failed");
                assertTrue(activity.getSupportFragmentManager().getFragments().isEmpty());
                assertEquals("Profile read failed", ((TextView) activity.findViewById(
                        R.id.m1_main_feedback)).getText().toString());
                activity.findViewById(R.id.m2_profile_retry).performClick();
                assertEquals(2, requests.size());
                requests.get(1).onSuccess(profile("REPORTER"));
                assertEquals(View.VISIBLE, activity.findViewById(R.id.m2_directory_host).getVisibility());
            });
        }
    }

    @Test
    public void missingProfileRoutesToCreation() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> requests.get(0).onSuccess(null));
            assertEquals(ProfileActivity.class.getName(), destination.getComponent().getClassName());
        }
    }

    @Test
    public void staffAndAdminKeepTheirCanonicalDestinations() {
        String[] roles = {"MAINTENANCE_STAFF", "ADMIN"};
        Class<?>[] destinations = {StaffDashboardActivity.class, AdminDashboardActivity.class};
        for (int i = 0; i < roles.length; i++) {
            requests.clear();
            String role = roles[i];
            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                scenario.onActivity(activity -> requests.get(0).onSuccess(profile(role)));
                assertEquals(destinations[i].getName(), destination.getComponent().getClassName());
            }
        }
    }

    @Test
    public void stoppedRequestCannotOpenTheMap() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            AuthRepository.Callback<UserProfile> oldRequest = requests.get(0);
            scenario.moveToState(Lifecycle.State.CREATED);
            instrumentation.runOnMainSync(() -> oldRequest.onSuccess(profile("REPORTER")));
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(activity -> {
                assertTrue(activity.getSupportFragmentManager().getFragments().isEmpty());
                assertEquals(2, requests.size());
            });
        }
    }

    @Test
    public void recreationRechecksAccessAndRestoresList() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                requests.get(0).onSuccess(profile("REPORTER"));
                activity.findViewById(R.id.m2_list_tab).performClick();
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertEquals(View.GONE, activity.findViewById(R.id.m2_directory_host).getVisibility());
                assertNull(activity.getSupportFragmentManager().getFragments().get(0).getView());
                requests.get(1).onSuccess(profile("REPORTER"));
                assertEquals(View.VISIBLE, activity.findViewById(R.id.m2_list_panel).getVisibility());
                assertEquals(1, activity.getSupportFragmentManager().getFragments().size());
            });
        }
    }

    @Test
    public void logoutReturnsToLoginAndClearsTheTask() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                requests.get(0).onSuccess(profile("REPORTER"));
                activity.findViewById(R.id.m1_main_logout_btn).performClick();
            });
            assertEquals(LoginActivity.class.getName(), destination.getComponent().getClassName());
            assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK,
                    destination.getFlags());
        }
    }

    private static UserProfile profile(String role) {
        UserProfile profile = new UserProfile("test-uid", "test@siswa.um.edu.my",
                "Reporter test", "FSKTM", "Off-campus");
        profile.setRole(role);
        return profile;
    }
}
