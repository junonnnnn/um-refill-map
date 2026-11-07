package com.example.umborrowbuddy.features.module1.auth;

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

import java.util.Locale;

/**
 * Handles password reset email dispatch via Firebase Authentication.
 * Displays neutral messaging to prevent user enumeration, while properly handling network failures.
 */
public class ResetPasswordActivity extends AppCompatActivity {

    private EditText emailInput;
    private Button submitButton;
    private Button toLoginButton;
    private ProgressBar progress;
    private TextView feedback;

    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.m1_activity_reset_password);

        View root = findViewById(R.id.m1_reset_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets keyboard = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right,
                    Math.max(bars.bottom, keyboard.bottom));
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);

        authRepository = new AuthRepository();

        emailInput = findViewById(R.id.m1_reset_email);
        submitButton = findViewById(R.id.m1_reset_btn);
        toLoginButton = findViewById(R.id.m1_reset_to_login);
        progress = findViewById(R.id.m1_reset_progress);
        feedback = findViewById(R.id.m1_reset_feedback);

        setLoading(false);

        submitButton.setOnClickListener(v -> handlePasswordReset());
        toLoginButton.setOnClickListener(v -> finish());
    }

    private void handlePasswordReset() {
        emailInput.setError(null);
        feedback.setText("");

        String email = emailInput.getText().toString().trim();
        if (!UmEmailValidator.isValidUmEmail(email)) {
            emailInput.setError(getString(R.string.m1_invalid_email));
            emailInput.requestFocus();
            feedback.setText(R.string.m1_check_fields);
            return;
        }

        setLoading(true);
        authRepository.sendPasswordReset(email, new AuthRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                setLoading(false);
                // Neutral feedback protects against user enumeration
                feedback.setText(R.string.m1_reset_neutral_notice);
                emailInput.setText("");
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                if (isNetworkFailure(errorMessage)) {
                    // Truly report network disconnection
                    feedback.setText(R.string.m1_network_error);
                } else {
                    // Maintain neutral response if user does not exist or other auth lookup issue
                    feedback.setText(R.string.m1_reset_neutral_notice);
                    emailInput.setText("");
                }
            }
        });
    }

    private boolean isNetworkFailure(String errorMessage) {
        if (errorMessage == null) {
            return false;
        }
        String lower = errorMessage.toLowerCase(Locale.ROOT);
        return lower.contains("network")
                || lower.contains("connection")
                || lower.contains("timeout")
                || lower.contains("offline")
                || lower.contains("unreachable");
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        submitButton.setEnabled(!loading);
        toLoginButton.setEnabled(!loading);
        emailInput.setEnabled(!loading);
    }
}
