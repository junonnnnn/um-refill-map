package com.example.umborrowbuddy.features.module1.auth;

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
import com.example.umborrowbuddy.shared.model.UserProfile;
import com.google.firebase.auth.FirebaseUser;

/**
 * Handles UM student and staff login using Firebase Authentication.
 * Enforces email domain validation, verification checks, and role-based navigation.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private Button registerButton;
    private Button resetButton;
    private ProgressBar progress;
    private TextView feedback;

    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.m1_activity_login);

        View root = findViewById(R.id.m1_login_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets keyboard = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right,
                    Math.max(bars.bottom, keyboard.bottom));
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);

        authRepository = new AuthRepository();

        emailInput = findViewById(R.id.m1_email);
        passwordInput = findViewById(R.id.m1_password);
        loginButton = findViewById(R.id.m1_login);
        registerButton = findViewById(R.id.m1_register);
        resetButton = findViewById(R.id.m1_forgot_password);
        progress = findViewById(R.id.m1_progress);
        feedback = findViewById(R.id.m1_feedback);

        // Do not persist the password through Android's view-state saving.
        passwordInput.setSaveEnabled(false);
        setLoading(false);

        loginButton.setOnClickListener(view -> handleLogin());
        registerButton.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
        resetButton.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, ResetPasswordActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check active session on launch
        AuthGuard.AuthState authState = AuthGuard.checkAuthState();
        if (authState == AuthGuard.AuthState.AUTHENTICATED_AND_VERIFIED) {
            routeUserToRoleDestination();
        } else if (authState == AuthGuard.AuthState.UNVERIFIED_EMAIL) {
            navigateToVerifyEmail();
        }
    }

    /**
     * Validate input locally and authenticate with Firebase.
     */
    private void handleLogin() {
        emailInput.setError(null);
        passwordInput.setError(null);
        feedback.setText("");

        String email = emailInput.getText().toString().trim();
        // Keep passwords exactly as entered: never trim or log them.
        String password = passwordInput.getText().toString();

        if (!UmEmailValidator.isValidUmEmail(email)) {
            emailInput.setError(getString(R.string.m1_invalid_email));
            emailInput.requestFocus();
            feedback.setText(R.string.m1_check_fields);
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError(getString(R.string.m1_password_required));
            passwordInput.requestFocus();
            feedback.setText(R.string.m1_check_fields);
            return;
        }

        setLoading(true);
        authRepository.login(email, password, new AuthRepository.Callback<FirebaseUser>() {
            @Override
            public void onSuccess(FirebaseUser user) {
                setLoading(false);
                if (user != null && user.isEmailVerified()) {
                    routeUserToRoleDestination();
                } else {
                    navigateToVerifyEmail();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                feedback.setText(errorMessage);
            }
        });
    }

    private void routeUserToRoleDestination() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            return;
        }
        setLoading(true);
        feedback.setText("");
        authRepository.getUserProfile(user.getUid(), new AuthRepository.Callback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile profile) {
                setLoading(false);
                RoleRouter.routeProfile(LoginActivity.this, profile);
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                feedback.setText("Failed to load user role: " + errorMessage);
            }
        });
    }

    private void navigateToVerifyEmail() {
        Intent intent = new Intent(LoginActivity.this, VerifyEmailActivity.class);
        startActivity(intent);
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!loading);
        registerButton.setEnabled(!loading);
        resetButton.setEnabled(!loading);
        emailInput.setEnabled(!loading);
        passwordInput.setEnabled(!loading);
    }
}
