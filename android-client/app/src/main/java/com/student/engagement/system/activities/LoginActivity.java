package com.student.engagement.system.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
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
import com.student.engagement.system.validation.FormField;
import com.student.engagement.system.utils.FormUtils;
import com.student.engagement.system.validation.FormValidation;
import com.student.engagement.system.utils.ViewUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LOGIN";

    public enum LoginField implements FormField {EMAIL, PASSWORD}

    private TextInputLayout tilEmail, tilPassword;

    private TextInputEditText etEmail, etPassword;

    private MaterialButton btnLogin;

    private TextView tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isLogged = SessionManager.isLoggedIn(this);

        if (isLogged) {
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

        etEmail.requestFocus();
    }

    private void initializeControls() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegisterLink);
        tilEmail = FormUtils.findParentLayout(etEmail);
        tilPassword = FormUtils.findParentLayout(etPassword);
    }

    private void initializeEvents() {
        Runnable loginAction = () -> {
            FormUtils.clearErrors(tilEmail, tilPassword);

            FormValidation<LoginField> result = validateLogin();

            if (result.isValid) {
                performLogin();
            } else {
                handleValidationError(result);
            }
        };

        btnLogin.setOnClickListener(v -> loginAction.run());

        ViewUtils.onDone(etPassword, loginAction);

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        FormUtils.clearErrorOnTyping(etEmail, tilEmail);
        FormUtils.clearErrorOnTyping(etPassword, tilPassword);
    }

    private void handleValidationError(FormValidation<LoginField> result) {
        if (result.field == LoginField.EMAIL) {
            FormUtils.setError(tilEmail, etEmail, result.message);
        } else if (result.field == LoginField.PASSWORD) {
            FormUtils.setError(tilPassword, etPassword, result.message);
        }
    }

    private void performLogin() {
        ViewUtils.setLoading(btnLogin, "Logging in...");

        String email = FormUtils.getText(etEmail);
        String password = FormUtils.getText(etPassword);

        AuthService authService = SupabaseClient.getInstance(this).create(AuthService.class);
        LoginRequest request = new LoginRequest(email, password);

        authService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                ViewUtils.resetButton(btnLogin, "Login");

                if (response.isSuccessful() && response.body() != null) {
                    processLoginSuccess(response.body());
                } else {
                    Log.e(TAG, "Login failed -> HTTP code: " + response.code());
                    processLoginFailure(response);
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Log.e(TAG, "onFailure -> " + t.getMessage(), t);
                ViewUtils.resetButton(btnLogin, "Login");
                Toast.makeText(LoginActivity.this, "Connection error!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void processLoginSuccess(AuthResponse data) {
        if (data.getUser() == null) {
            Toast.makeText(this, "Unexpected error occurred!", Toast.LENGTH_LONG).show();
            return;
        }

        String firstName = "";
        String lastName = "";

        if (data.getUser().getUserMetadata() != null) {
            firstName = data.getUser().getUserMetadata().getFirstName();
            lastName = data.getUser().getUserMetadata().getLastName();
        }

        SessionManager.saveSession(
                this,
                data.getAccessToken(),
                data.getRefreshToken(),
                data.getUser().getId(),
                firstName,
                lastName,
                data.getUser().getEmail()
        );
        goToMainActivity();
    }

    private void processLoginFailure(Response<AuthResponse> response) {
        FormUtils.clearErrors(tilEmail, tilPassword);

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

        if(errorMessage.toLowerCase().contains("email")) {
            FormUtils.setError(tilEmail, etEmail, errorMessage);
        } else {
            FormUtils.setError(tilPassword, etPassword, errorMessage);
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }


    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
    }

    private FormValidation<LoginField> validateLogin() {
        String email = FormUtils.getText(etEmail);
        if (email.isEmpty()) {
            return FormValidation.error(LoginField.EMAIL, "Email is required!");
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return FormValidation.error(LoginField.EMAIL, "Enter a valid email!");
        }

        String password = FormUtils.getText(etPassword);
        if (password.isEmpty()) {
            return FormValidation.error(LoginField.PASSWORD, "Password is required!");
        }
        return FormValidation.valid();
    }
}