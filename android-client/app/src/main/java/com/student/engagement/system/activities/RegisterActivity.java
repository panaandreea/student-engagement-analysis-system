package com.student.engagement.system.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.student.engagement.system.R;
import com.student.engagement.system.models.requests.RegisterRequest;
import com.student.engagement.system.models.response.AuthResponse;
import com.student.engagement.system.networking.SupabaseClient;
import com.student.engagement.system.services.AuthService;
import com.student.engagement.system.session.SessionManager;
import com.student.engagement.system.utils.Field;
import com.student.engagement.system.utils.ValidationResult;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "REGISTER";
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@ie\\.ase\\.ro$";
    public enum RegisterField implements Field {FIRST_NAME, LAST_NAME, EMAIL, PASSWORD, CONFIRM_PASSWORD}
    private TextInputLayout tilFirstName, tilLastName, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etFirstName, etLastName, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnRegister;
    private TextView tvLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Initializing RegisterActivity...");

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        initializeControls();
        initializeEvents();
    }

    private void initializeControls() {
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        tilFirstName = findParentTextInputLayout(etFirstName);
        tilLastName = findParentTextInputLayout(etLastName);
        tilEmail = findParentTextInputLayout(etEmail);
        tilPassword = findParentTextInputLayout(etPassword);
        tilConfirmPassword = findParentTextInputLayout(etConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLoginLink);
    }

    private void initializeEvents() {
        Runnable registerAction = () -> {
            clearErrors();

            ValidationResult<RegisterField> result = validateRegister();

            if (result.isValid) {
                Log.i(TAG, "registerAction: Validation passed. Proceeding to API call.");
                performRegistration();
            } else {
                Log.w(TAG, "registerAction: Validation failed on field " + result.field);
                handleValidationError(result);
            }
        };

        btnRegister.setOnClickListener(v -> registerAction.run());

        etConfirmPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus();
                registerAction.run();
                return true;
            }
            return false;
        });

        tvLogin.setOnClickListener(v -> {
            Log.d(TAG, "Navigating back to Login screen.");
            finish();
        });
    }

    private void clearErrors() {
        tilFirstName.setError(null);
        tilLastName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private void handleValidationError(ValidationResult<RegisterField> result) {
        switch (result.field) {
            case FIRST_NAME:
                tilFirstName.setError(result.message);
                etFirstName.requestFocus();
                break;
            case LAST_NAME:
                tilLastName.setError(result.message);
                etLastName.requestFocus();
                break;
            case EMAIL:
                tilEmail.setError(result.message);
                etEmail.requestFocus();
                break;
            case PASSWORD:
                tilPassword.setError(result.message);
                etPassword.requestFocus();
                break;
            case CONFIRM_PASSWORD:
                tilConfirmPassword.setError(result.message);
                etConfirmPassword.requestFocus();
                break;
        }
    }

    private void performRegistration() {
        btnRegister.setEnabled(false);
        btnRegister.setText("Creating account...");

        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        Log.d(TAG, "performRegistration: Calling Auth Service for " + email);
        AuthService service = SupabaseClient.getInstance(this).create(AuthService.class);
        RegisterRequest request = new RegisterRequest(firstName, lastName, email, password);

        service.register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                resetButton();
                Log.d(TAG, "onResponse: Received status code " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    processRegisterSuccess(response.body());
                } else {
                    Log.e(TAG, "onResponse: Registration failed. Body: " + response.errorBody());
                    Toast.makeText(RegisterActivity.this, "Registration failed. This email might already be registered.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "onFailure: Network error -> " + t.getMessage());
                resetButton();
                Toast.makeText(RegisterActivity.this, "Network error. Check your connection.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void processRegisterSuccess(AuthResponse data) {
        if (data.getUser() == null) {
            Log.e(TAG, "processRegisterSuccess: User object is null in response.");
            return;
        }

        if (data.getAccessToken() == null) {
            Log.i(TAG, "processRegisterSuccess: Account created.");
            Toast.makeText(this, "Account created! Please check your email to confirm.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.i(TAG, "processRegisterSuccess: Direct login success. Saving session.");

        String fName = "";
        String lName = "";
        if (data.getUser().getUserMetadata() != null) {
            fName = data.getUser().getUserMetadata().getFirstName();
            lName = data.getUser().getUserMetadata().getLastName();
        }

        SessionManager.saveSession(
                this,
                data.getAccessToken(),
                data.getRefreshToken(),
                data.getUser().getId(),
                fName,
                lName,
                data.getUser().getEmail()
        );

        SupabaseClient.reset();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void resetButton() {
        btnRegister.setEnabled(true);
        btnRegister.setText("Create Account");
    }

    private ValidationResult<RegisterField> validateRegister() {
        String firstName = etFirstName.getText().toString().trim();
        if (firstName.isEmpty()) return ValidationResult.error(RegisterField.FIRST_NAME, "First name is required!");

        String lastName = etLastName.getText().toString().trim();
        if (lastName.isEmpty()) return ValidationResult.error(RegisterField.LAST_NAME, "Last name is required!");

        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) return ValidationResult.error(RegisterField.EMAIL, "Email is required!");
        if (!email.matches(EMAIL_REGEX)) return ValidationResult.error(RegisterField.EMAIL, "Please use your @ie.ase.ro address!");

        String password = etPassword.getText().toString().trim();
        if (password.isEmpty()) return ValidationResult.error(RegisterField.PASSWORD, "Password is required!");
        if (password.length() < 6) return ValidationResult.error(RegisterField.PASSWORD, "Password must be at least 6 characters!");

        String confirm = etConfirmPassword.getText().toString().trim();
        if (!confirm.equals(password)) return ValidationResult.error(RegisterField.CONFIRM_PASSWORD, "Passwords do not match!");

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