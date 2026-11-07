package com.example.umborrowbuddy.features.module1.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.umborrowbuddy.R;
import com.example.umborrowbuddy.features.module1.auth.AuthGuard;
import com.example.umborrowbuddy.features.module1.auth.AuthRepository;
import com.example.umborrowbuddy.features.module1.auth.LoginActivity;
import com.example.umborrowbuddy.features.module1.auth.RoleRouter;
import com.example.umborrowbuddy.features.module1.auth.VerifyEmailActivity;
import com.example.umborrowbuddy.shared.model.UserProfile;
import com.example.umborrowbuddy.shared.model.UserRole;
import com.google.firebase.auth.FirebaseUser;

/**
 * Handles User Profile creation, viewing, and editing.
 * Displays read-only user role and enforces session guards.
 */
public class ProfileActivity extends AppCompatActivity {

    private TextView titleText;
    private TextView emailText;
    private TextView roleText;
    private EditText displayNameInput;
    private EditText facultyInput;
    private EditText collegeInput;

    private Button dashboardButton;
    private Button saveButton;
    private Button editButton;
    private Button cancelButton;
    private Button logoutButton;
    private ProgressBar progress;
    private TextView feedback;

    private AuthRepository authRepository;
    private UserProfile existingProfile;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.m1_activity_profile);

        View root = findViewById(R.id.m1_profile_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets keyboard = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right,
                    Math.max(bars.bottom, keyboard.bottom));
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);

        authRepository = new AuthRepository();

        titleText = findViewById(R.id.m1_profile_title);
        emailText = findViewById(R.id.m1_profile_email);
        roleText = findViewById(R.id.m1_profile_role);
        displayNameInput = findViewById(R.id.m1_profile_display_name);
        facultyInput = findViewById(R.id.m1_profile_faculty);
        collegeInput = findViewById(R.id.m1_profile_college);

        dashboardButton = findViewById(R.id.m1_profile_dashboard_btn);
        saveButton = findViewById(R.id.m1_profile_save_btn);
        editButton = findViewById(R.id.m1_profile_edit_btn);
        cancelButton = findViewById(R.id.m1_profile_cancel_btn);
        logoutButton = findViewById(R.id.m1_profile_logout_btn);
        progress = findViewById(R.id.m1_profile_progress);
        feedback = findViewById(R.id.m1_profile_feedback);

        dashboardButton.setOnClickListener(v -> {
            FirebaseUser user = authRepository.getCurrentUser();
            if (user == null) { kickToLogin(); return; }
            setLoading(true);
            authRepository.getUserProfile(user.getUid(), new AuthRepository.Callback<UserProfile>() {
                @Override public void onSuccess(UserProfile profile) {
                    setLoading(false);
                    if (profile == null) {
                        feedback.setText("Complete and save your profile first.");
                        return;
                    }
                    RoleRouter.routeProfile(ProfileActivity.this, profile);
                }
                @Override public void onFailure(String errorMessage) {
                    setLoading(false);
                    feedback.setText(errorMessage);
                }
            });
        });

        saveButton.setOnClickListener(v -> handleSaveProfile());
        editButton.setOnClickListener(v -> enterEditMode());
        cancelButton.setOnClickListener(v -> cancelEditMode());
        logoutButton.setOnClickListener(v -> handleLogout());
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Guard check: cannot be bypassed by Back button or deep linking
        AuthGuard.AuthState authState = AuthGuard.checkAuthState();
        if (authState == AuthGuard.AuthState.NOT_LOGGED_IN || authState == AuthGuard.AuthState.NON_UM_EMAIL) {
            kickToLogin();
            return;
        }
        if (authState == AuthGuard.AuthState.UNVERIFIED_EMAIL) {
            kickToVerifyEmail();
            return;
        }

        FirebaseUser user = authRepository.getCurrentUser();
        if (user != null) {
            emailText.setText(user.getEmail());
            loadProfile(user.getUid());
        }
    }

    private void loadProfile(String uid) {
        setLoading(true);
        authRepository.getUserProfile(uid, new AuthRepository.Callback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile profile) {
                setLoading(false);
                existingProfile = profile;
                String roleStr = profile != null ? profile.getRole() : UserRole.REPORTER.name();
                roleText.setText(roleStr);
                if (profile != null && profile.isComplete()) {
                    populateFields(profile);
                    enterViewMode();
                } else {
                    enterCreationMode();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                roleText.setText("Role unavailable");
                feedback.setText(errorMessage);
            }
        });
    }

    private void populateFields(UserProfile profile) {
        displayNameInput.setText(profile.getDisplayName());
        facultyInput.setText(profile.getFaculty());
        collegeInput.setText(profile.getResidentialCollege());
        if (profile.getRole() != null) {
            roleText.setText(profile.getRole());
        }
    }

    private void enterViewMode() {
        isEditMode = false;
        titleText.setText(R.string.m1_profile_title_view);
        setFieldsEnabled(false);
        saveButton.setVisibility(View.GONE);
        cancelButton.setVisibility(View.GONE);
        editButton.setVisibility(View.VISIBLE);
    }

    private void enterCreationMode() {
        isEditMode = true;
        titleText.setText(R.string.m1_profile_title_create);
        setFieldsEnabled(true);
        saveButton.setVisibility(View.VISIBLE);
        cancelButton.setVisibility(View.GONE);
        editButton.setVisibility(View.GONE);
    }

    private void enterEditMode() {
        isEditMode = true;
        titleText.setText(R.string.m1_profile_title_edit);
        setFieldsEnabled(true);
        saveButton.setVisibility(View.VISIBLE);
        cancelButton.setVisibility(View.VISIBLE);
        editButton.setVisibility(View.GONE);
    }

    private void cancelEditMode() {
        if (existingProfile != null) {
            populateFields(existingProfile);
            enterViewMode();
        }
    }

    private void setFieldsEnabled(boolean enabled) {
        displayNameInput.setEnabled(enabled);
        facultyInput.setEnabled(enabled);
        collegeInput.setEnabled(enabled);
    }

    private void handleSaveProfile() {
        clearFieldErrors();
        feedback.setText("");

        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            kickToLogin();
            return;
        }

        // Keep role from existing profile or default to REPORTER; never allow user self-assignment
        String roleStr = existingProfile != null ? existingProfile.getRole() : UserRole.REPORTER.name();
        UserProfile profile = new UserProfile(
                user.getUid(),
                user.getEmail(),
                displayNameInput.getText().toString().trim(),
                facultyInput.getText().toString().trim(),
                collegeInput.getText().toString().trim(),
                roleStr
        );

        String validationError = profile.validate();
        if (validationError != null) {
            feedback.setText(validationError);
            if (profile.getDisplayName() == null || profile.getDisplayName().isEmpty() || profile.getDisplayName().length() > 60) {
                displayNameInput.setError(validationError);
                displayNameInput.requestFocus();
            } else if (profile.getFaculty() == null || profile.getFaculty().isEmpty() || profile.getFaculty().length() > 100) {
                facultyInput.setError(validationError);
                facultyInput.requestFocus();
            } else {
                collegeInput.setError(validationError);
                collegeInput.requestFocus();
            }
            return;
        }

        setLoading(true);
        authRepository.saveUserProfile(profile, new AuthRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                setLoading(false);
                existingProfile = profile;
                roleText.setText(profile.getRole());
                feedback.setText(R.string.m1_profile_saved_success);
                enterViewMode();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                feedback.setText(errorMessage);
            }
        });
    }

    private void clearFieldErrors() {
        displayNameInput.setError(null);
        facultyInput.setError(null);
        collegeInput.setError(null);
    }

    private void handleLogout() {
        authRepository.logout();
        kickToLogin();
    }

    private void kickToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void kickToVerifyEmail() {
        Intent intent = new Intent(this, VerifyEmailActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!loading);
        editButton.setEnabled(!loading);
        cancelButton.setEnabled(!loading);
        logoutButton.setEnabled(!loading);
        dashboardButton.setEnabled(!loading);
        if (isEditMode) {
            setFieldsEnabled(!loading);
        }
    }
}
