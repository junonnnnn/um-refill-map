package com.example.umborrowbuddy;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.example.umborrowbuddy.features.module1.auth.AuthGuard;
import com.example.umborrowbuddy.features.module1.auth.AuthRepository;
import com.example.umborrowbuddy.features.module1.auth.LoginActivity;
import com.example.umborrowbuddy.features.module1.auth.RoleRouter;
import com.example.umborrowbuddy.features.module1.profile.ProfileActivity;
import com.example.umborrowbuddy.features.module2.StationDirectoryFragment;
import com.example.umborrowbuddy.shared.model.UserProfile;
import com.example.umborrowbuddy.shared.model.UserRole;

/** Canonical reporter destination, gated by the server profile before loading Module 2. */
public class MainActivity extends AppCompatActivity {
    private TextView emailText;
    private TextView roleText;
    private Button profileButton;
    private TextView feedback;
    private View progress;
    private View retry;
    private View directoryHost;
    private AuthRepository authRepository;
    private int accessRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars((getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        authRepository = new AuthRepository();
        emailText = findViewById(R.id.m1_main_email);
        roleText = findViewById(R.id.m1_main_role);
        profileButton = findViewById(R.id.m1_main_profile_btn);
        feedback = findViewById(R.id.m1_main_feedback);
        progress = findViewById(R.id.m2_profile_loading);
        retry = findViewById(R.id.m2_profile_retry);
        directoryHost = findViewById(R.id.m2_directory_host);
        pauseDirectory();

        profileButton.setOnClickListener(view -> startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.m1_main_logout_btn).setOnClickListener(view -> {
            authRepository.logout();
            kickToLogin();
        });
        retry.setOnClickListener(view -> checkReporterProfile());
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkReporterProfile();
    }

    private void checkReporterProfile() {
        int request = ++accessRequest;
        directoryHost.setVisibility(View.GONE);
        emailText.setText("");
        roleText.setText("");
        profileButton.setEnabled(false);
        feedback.setText(R.string.m1_loading_profile);
        progress.setVisibility(View.VISIBLE);
        retry.setVisibility(View.GONE);
        loadCurrentProfile(new AuthRepository.Callback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile profile) {
                if (!isCurrentRequest(request)) return;
                progress.setVisibility(View.GONE);
                feedback.setText("");
                if (profile == null || profile.getRoleEnum() != UserRole.REPORTER) {
                    RoleRouter.routeProfile(MainActivity.this, profile);
                    return;
                }
                emailText.setText(profile.getEmail());
                roleText.setText(getString(R.string.m1_role_display, profile.getRoleEnum().name()));
                profileButton.setEnabled(true);
                Fragment directory = getSupportFragmentManager().findFragmentById(R.id.m2_directory_host);
                if (directory == null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.m2_directory_host, StationDirectoryFragment.class, null).commitNow();
                } else {
                    getSupportFragmentManager().beginTransaction()
                            .setMaxLifecycle(directory, Lifecycle.State.RESUMED).commitNow();
                }
                directoryHost.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(String errorMessage) {
                if (!isCurrentRequest(request)) return;
                progress.setVisibility(View.GONE);
                feedback.setText(errorMessage);
                retry.setVisibility(View.VISIBLE);
            }
        });
    }

    protected void loadCurrentProfile(AuthRepository.Callback<UserProfile> callback) {
        int request = accessRequest;
        String uid = authRepository.getCurrentUid();
        if (!hasCurrentSession(uid)) {
            kickToLogin();
            return;
        }
        authRepository.getUserProfile(uid, new AuthRepository.Callback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile profile) {
                if (!isCurrentRequest(request)) return;
                if (hasCurrentSession(uid)) callback.onSuccess(profile);
                else kickToLogin();
            }

            @Override
            public void onFailure(String errorMessage) {
                if (!isCurrentRequest(request)) return;
                if (hasCurrentSession(uid)) callback.onFailure(errorMessage);
                else kickToLogin();
            }
        });
    }

    private boolean hasCurrentSession(String uid) {
        return uid != null && uid.equals(authRepository.getCurrentUid())
                && AuthGuard.checkAuthState() == AuthGuard.AuthState.AUTHENTICATED_AND_VERIFIED;
    }

    private boolean isCurrentRequest(int request) {
        return request == accessRequest && !isFinishing() && !isDestroyed()
                && !getSupportFragmentManager().isStateSaved();
    }

    private void pauseDirectory() {
        Fragment directory = getSupportFragmentManager().findFragmentById(R.id.m2_directory_host);
        if (directory != null) {
            // Revalidation must also gate a Fragment restored from already-saved Activity state.
            getSupportFragmentManager().beginTransaction()
                    .setMaxLifecycle(directory, Lifecycle.State.CREATED).commitNowAllowingStateLoss();
        }
    }

    @Override
    protected void onStop() {
        ++accessRequest;
        directoryHost.setVisibility(View.GONE);
        pauseDirectory();
        super.onStop();
    }

    private void kickToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
