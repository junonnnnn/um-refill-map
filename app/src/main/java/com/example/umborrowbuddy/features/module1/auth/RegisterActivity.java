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

import java.util.Objects;

/**
 * Handles UM student and staff account registration using Firebase Authentication.
 * Restricts registration strictly to @siswa.um.edu.my and @um.edu.my domains.
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private Button registerButton;
    private Button toLoginButton;
    private ProgressBar progress;
    private TextView feedback;

    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.m1_activity_register);

        View root = findViewById(R.id.m1_register_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets keyboard = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right,
                    Math.max(bars.bottom, keyboard.bottom));
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);

        authRepository = new AuthRepository();

        emailInput = findViewById(R.id.m1_register_email);
        passwordInput = findViewById(R.id.m1_register_password);
        confirmPasswordInput = findViewById(R.id.m1_register_confirm_password);
        registerButton = findViewById(R.id.m1_register_btn);
        toLoginButton = findViewById(R.id.m1_register_to_login);
        progress = findViewById(R.id.m1_register_progress);
        feedback = findViewById(R.id.m1_register_feedback);

        passwordInput.setSaveEnabled(false);
        confirmPasswordInput.setSaveEnabled(false);
        setLoading(false);

        registerButton.setOnClickListener(v -> handleRegister());
        toLoginButton.setOnClickListener(v -> finish());
    }

    private void handleRegister() {
        emailInput.setError(null);
        passwordInput.setError(null);
        confirmPasswordInput.setError(null);
        feedback.setText("");

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        if (!UmEmailValidator.isValidUmEmail(email)) {
            emailInput.setError(getString(R.string.m1_invalid_email));
            emailInput.requestFocus();
            feedback.setText(R.string.m1_check_fields);
            return;
        }

        UmPasswordValidator.ValidationResult passwordResult = UmPasswordValidator.validate(password);
        if (passwordResult != UmPasswordValidator.ValidationResult.VALID) {
            switch (passwordResult) {
                case EMPTY:
                    passwordInput.setError(getString(R.string.m1_password_required));
                    break;
                case TOO_SHORT:
                    passwordInput.setError(getString(R.string.m1_password_min_length));
                    break;
                case MISSING_UPPERCASE:
                    passwordInput.setError(getString(R.string.m1_password_requires_uppercase));
                    break;
                case MISSING_LOWERCASE:
                    passwordInput.setError(getString(R.string.m1_password_requires_lowercase));
                    break;
                case MISSING_NUMBER:
                    passwordInput.setError(getString(R.string.m1_password_requires_number));
                    break;
            }
            passwordInput.requestFocus();
            feedback.setText(R.string.m1_check_fields);
            return;
        }

        if (!Objects.equals(password, confirmPassword)) {
            confirmPasswordInput.setError(getString(R.string.m1_password_mismatch));
            confirmPasswordInput.requestFocus();
            feedback.setText(R.string.m1_check_fields);
            return;
        }

        setLoading(true);
        authRepository.register(email, password, new AuthRepository.Callback<AuthRepository.RegistrationResult>() {
            @Override
            public void onSuccess(AuthRepository.RegistrationResult result) {
                setLoading(false);
                Intent intent = new Intent(RegisterActivity.this, VerifyEmailActivity.class);
                if (!result.isVerificationEmailSent()) {
                    intent.putExtra(VerifyEmailActivity.EXTRA_VERIFICATION_FAILED_MESSAGE, result.getVerificationErrorMessage());
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                feedback.setText(errorMessage);
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!loading);
        toLoginButton.setEnabled(!loading);
        emailInput.setEnabled(!loading);
        passwordInput.setEnabled(!loading);
        confirmPasswordInput.setEnabled(!loading);
    }
}
