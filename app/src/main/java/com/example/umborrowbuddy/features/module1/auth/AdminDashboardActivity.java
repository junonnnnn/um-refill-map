package com.example.umborrowbuddy.features.module1.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.umborrowbuddy.R;
import com.example.umborrowbuddy.features.module1.profile.ProfileActivity;
import com.example.umborrowbuddy.shared.model.UserProfile;
import com.example.umborrowbuddy.shared.model.UserRole;
import com.google.firebase.auth.FirebaseUser;

/**
 * System Admin Control Panel destination.
 * Enforces session authentication, email verification, and ADMIN role checks.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private TextView emailText;
    private TextView roleText;
    private Button profileButton;
    private Button logoutButton;
    private TextView feedback;

    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.m1_activity_admin_dashboard);

        View root = findViewById(R.id.m1_admin_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets keyboard = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right,
                    Math.max(bars.bottom, keyboard.bottom));
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);

        authRepository = new AuthRepository();

        emailText = findViewById(R.id.m1_admin_email);
        roleText = findViewById(R.id.m1_admin_role);
        profileButton = findViewById(R.id.m1_admin_profile_btn);
        logoutButton = findViewById(R.id.m1_admin_logout_btn);
        feedback = findViewById(R.id.m1_admin_feedback);

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {
            authRepository.logout();
            kickToLogin();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        AuthGuard.AuthState authState = AuthGuard.checkAuthState();
        if (authState != AuthGuard.AuthState.AUTHENTICATED_AND_VERIFIED) {
            kickToLogin();
            return;
        }

        FirebaseUser user = authRepository.getCurrentUser();
        if (user != null) {
            emailText.setText(user.getEmail());
            authRepository.getUserProfile(user.getUid(), new AuthRepository.Callback<UserProfile>() {
                @Override
                public void onSuccess(UserProfile profile) {
                    if (profile == null) {
                        RoleRouter.routeProfile(AdminDashboardActivity.this, null);
                        return;
                    }
                    UserRole role = profile.getRoleEnum();
                    if (!AuthGuard.isRoleAuthorized(role, UserRole.ADMIN)) {
                        // Unauthorized role attempt; redirect to authorized destination
                        RoleRouter.routeUserByRole(AdminDashboardActivity.this, role);
                        return;
                    }
                    roleText.setText(getString(R.string.m1_role_display, role.name()));
                }

                @Override
                public void onFailure(String errorMessage) {
                    feedback.setText(errorMessage);
                }
            });
        }
    }

    private void kickToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
