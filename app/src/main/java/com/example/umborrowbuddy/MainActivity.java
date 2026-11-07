package com.example.umborrowbuddy;

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

import com.example.umborrowbuddy.features.module1.auth.AuthGuard;
import com.example.umborrowbuddy.features.module1.auth.AuthRepository;
import com.example.umborrowbuddy.features.module1.auth.LoginActivity;
import com.example.umborrowbuddy.features.module1.auth.RoleRouter;
import com.example.umborrowbuddy.features.module1.profile.ProfileActivity;
import com.example.umborrowbuddy.shared.model.UserProfile;
import com.example.umborrowbuddy.shared.model.UserRole;
import com.google.firebase.auth.FirebaseUser;

/**
 * Shared User & Reporter Destination for UM Water Refill Station Reporting System.
 * Enforces session authentication and email verification guard checks.
 */
public class MainActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        authRepository = new AuthRepository();

        emailText = findViewById(R.id.m1_main_email);
        roleText = findViewById(R.id.m1_main_role);
        profileButton = findViewById(R.id.m1_main_profile_btn);
        logoutButton = findViewById(R.id.m1_main_logout_btn);
        feedback = findViewById(R.id.m1_main_feedback);

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
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
                    if (profile == null || profile.getRoleEnum() != UserRole.REPORTER) {
                        RoleRouter.routeProfile(MainActivity.this, profile);
                        return;
                    }
                    UserRole role = profile.getRoleEnum();
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
