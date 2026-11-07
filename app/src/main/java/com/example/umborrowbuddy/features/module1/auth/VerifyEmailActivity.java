package com.example.umborrowbuddy.features.module1.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
 * Enforces email verification before granting access to protected features.
 */
public class VerifyEmailActivity extends AppCompatActivity {

    public static final String EXTRA_VERIFICATION_FAILED_MESSAGE = "extra_verification_failed_message";

    private Button checkVerifiedButton;
    private Button resendButton;
    private Button logoutButton;
    private ProgressBar progress;
    private TextView feedback;

    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.m1_activity_verify_email);

        View root = findViewById(R.id.m1_verify_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets keyboard = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right,
                    Math.max(bars.bottom, keyboard.bottom));
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);

        authRepository = new AuthRepository();

        TextView emailDisplay = findViewById(R.id.m1_verify_email_display);
        checkVerifiedButton = findViewById(R.id.m1_verify_check_btn);
        resendButton = findViewById(R.id.m1_verify_resend_btn);
        logoutButton = findViewById(R.id.m1_verify_logout_btn);
        progress = findViewById(R.id.m1_verify_progress);
        feedback = findViewById(R.id.m1_verify_feedback);

        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            redirectToLogin();
            return;
        }

        emailDisplay.setText(user.getEmail());
        setLoading(false);

        String failedMessage = getIntent().getStringExtra(EXTRA_VERIFICATION_FAILED_MESSAGE);
        if (failedMessage != null && !failedMessage.isEmpty()) {
            feedback.setText(getString(R.string.m1_verify_send_failed, failedMessage));
        }

        checkVerifiedButton.setOnClickListener(v -> checkVerificationStatus());
        resendButton.setOnClickListener(v -> resendVerificationEmail());
        logoutButton.setOnClickListener(v -> handleLogout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Automatically check verification when user returns from email app
        checkVerificationStatus();
    }

    private void checkVerificationStatus() {
        setLoading(true);
        feedback.setText("");
        authRepository.checkEmailVerification(new AuthRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean isVerified) {
                setLoading(false);
                if (Boolean.TRUE.equals(isVerified)) {
                    routeVerifiedUser();
                } else {
                    feedback.setText(R.string.m1_verify_pending);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                feedback.setText(errorMessage);
            }
        });
    }

    private void routeVerifiedUser() {
        String uid = authRepository.getCurrentUid();
        setLoading(true);
        feedback.setText("");
        authRepository.getUserProfile(uid, new AuthRepository.Callback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile profile) {
                setLoading(false);
                RoleRouter.routeProfile(VerifyEmailActivity.this, profile);
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                feedback.setText(errorMessage);
            }
        });
    }

    private void resendVerificationEmail() {
        setLoading(true);
        feedback.setText("");
        authRepository.sendEmailVerification(new AuthRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                setLoading(false);
                feedback.setText(R.string.m1_verify_resend_success);
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                feedback.setText(errorMessage);
            }
        });
    }

    private void handleLogout() {
        authRepository.logout();
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        checkVerifiedButton.setEnabled(!loading);
        resendButton.setEnabled(!loading);
        logoutButton.setEnabled(!loading);
    }
}
