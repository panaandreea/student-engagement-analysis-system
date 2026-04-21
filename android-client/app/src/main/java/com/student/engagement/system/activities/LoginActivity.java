package com.student.engagement.system.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.student.engagement.system.R;
import com.student.engagement.system.models.requests.LoginRequest;
import com.student.engagement.system.models.response.AuthResponse;
import com.student.engagement.system.networking.SupabaseClient;
import com.student.engagement.system.services.AuthService;
import com.student.engagement.system.session.SessionManager;
import com.student.engagement.system.utils.Field;
import com.student.engagement.system.utils.ValidationResult;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LOGIN";
    public enum LoginField implements Field {EMAIL, PASSWORD}
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: LoginActivity START");

        boolean isLogged = SessionManager.isLoggedIn(this);
        Log.d(TAG, "Session check - isLoggedIn = " + isLogged);

        if (isLogged) {
            Log.i(TAG, "User already logged in => redirecting to MainActivity");
            goToMainActivity();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        initializeControls();
        initializeEvents();
    }

    private void initializeControls() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegisterLink);
        tilEmail = findParentTextInputLayout(etEmail);
        tilPassword = findParentTextInputLayout(etPassword);
    }

    private void initializeEvents() {
        Runnable loginAction = () -> {
            Log.d(TAG, "Login triggered");

            tilEmail.setError(null);
            tilPassword.setError(null);

            ValidationResult<LoginField> result = validateLogin();

            Log.d(TAG, "Validation result -> valid: " + result.isValid);

            if (result.isValid) {
                performLogin();
            } else {
                Log.w(TAG, "Validation failed -> field: " + result.field + ", message: " + result.message);
                handleValidationError(result);
            }
        };

        btnLogin.setOnClickListener(v -> {
            Log.d(TAG, "Login button clicked");
            loginAction.run();
        });

        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            Log.d(TAG, "Keyboard action -> actionId: " + actionId);
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                Log.d(TAG, "IME_ACTION_DONE detected");
                v.clearFocus();
                loginAction.run();
                return true;
            }
            return false;
        });

        tvRegister.setOnClickListener(v -> {
            Log.d(TAG, "Register clicked -> opening RegisterActivity");
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void handleValidationError(ValidationResult<LoginField> result) {
        Log.d(TAG, "handleValidationError -> field: " + result.field);

        if (result.field == LoginField.EMAIL) {
            tilEmail.setError(result.message);
            etEmail.requestFocus();
        } else if (result.field == LoginField.PASSWORD) {
            tilPassword.setError(result.message);
            etPassword.requestFocus();
        }
    }

    private void performLogin() {
        Log.d(TAG, "performLogin: START");

        btnLogin.setEnabled(false);
        btnLogin.setText("Authenticating...");

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        Log.d(TAG, "Login data -> email: " + email);

        AuthService authService = SupabaseClient.getInstance(this).create(AuthService.class);
        LoginRequest request = new LoginRequest(email, password);

        Log.d(TAG, "Sending login request...");

        authService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                Log.d(TAG, "onResponse -> success: " + response.isSuccessful());

                resetButton();

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Response body received");
                    processLoginSuccess(response.body());
                } else {
                    Log.e(TAG, "Login failed -> HTTP code: " + response.code());
                    processLoginFailure(response);
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Log.e(TAG, "onFailure -> " + t.getMessage(), t);
                resetButton();
                Toast.makeText(LoginActivity.this, "Connection error!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void processLoginSuccess(AuthResponse data) {
        Log.d(TAG, "processLoginSuccess");

        if (data.getUser() == null) {
            Log.e(TAG, "User is null!");
            return;
        }

        String firstName = "";
        String lastName = "";

        if (data.getUser().getUserMetadata() != null) {
            firstName = data.getUser().getUserMetadata().getFirstName();
            lastName = data.getUser().getUserMetadata().getLastName();
        }

        Log.d(TAG, "User data -> email: " + data.getUser().getEmail());

        SessionManager.saveSession(
                this,
                data.getAccessToken(),
                data.getRefreshToken(),
                data.getUser().getId(),
                firstName,
                lastName,
                data.getUser().getEmail()
        );

        Log.d(TAG, "Session saved");

        SupabaseClient.reset();
        Log.d(TAG, "SupabaseClient reset");

        goToMainActivity();
    }

    private void processLoginFailure(Response<AuthResponse> response) {
        Log.d(TAG, "processLoginFailure");

        String errorMessage = "Invalid email or password!";

        try {
            if (response.errorBody() != null) {
                String errorStr = response.errorBody().string();
                Log.e(TAG, "Error body -> " + errorStr);

                if (errorStr.contains("Email not confirmed")) {
                    errorMessage = "Please confirm your email address!";
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing errorBody", e);
        }

        tilPassword.setError(errorMessage);
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void resetButton() {
        btnLogin.setEnabled(true);
        btnLogin.setText("Login");
    }

    private void goToMainActivity() {
        Log.i(TAG, "Navigating to MainActivity");
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private ValidationResult<LoginField> validateLogin() {
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Log.w(TAG, "Email empty");
            return ValidationResult.error(LoginField.EMAIL, "Email is required!");
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Log.w(TAG, "Email invalid format");
            return ValidationResult.error(LoginField.EMAIL, "Enter a valid email!");
        }

        String password = etPassword.getText().toString().trim();
        if (password.isEmpty()) {
            Log.w(TAG, "Password empty");
            return ValidationResult.error(LoginField.PASSWORD, "Password is required!");
        }

        Log.d(TAG, "Validation OK");
        return ValidationResult.valid();
    }

    private TextInputLayout findParentTextInputLayout(View view) {
        if (view.getParent() instanceof View) {
            View parent = (View) view.getParent();
            if (parent instanceof TextInputLayout) {
                return (TextInputLayout) parent;
            } else if (parent.getParent() instanceof TextInputLayout) {
                return (TextInputLayout) parent.getParent();
            }
        }
        return null;
    }
}