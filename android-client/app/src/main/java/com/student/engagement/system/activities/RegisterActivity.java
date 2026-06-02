package com.student.engagement.system.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.student.engagement.system.validation.FormField;
import com.student.engagement.system.utils.FormUtils;
import com.student.engagement.system.validation.FormValidation;
import com.student.engagement.system.utils.ViewUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "REGISTER";

    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@ie\\.ase\\.ro$";

    public enum RegisterField implements FormField {FIRST_NAME, LAST_NAME, EMAIL, PASSWORD, CONFIRM_PASSWORD}

    private TextInputLayout tilFirstName, tilLastName, tilEmail, tilPassword, tilConfirmPassword;

    private TextInputEditText etFirstName, etLastName, etEmail, etPassword, etConfirmPassword;

    private MaterialButton btnRegister;

    private TextView tvLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        tilFirstName = FormUtils.findParentLayout(etFirstName);
        tilLastName = FormUtils.findParentLayout(etLastName);
        tilEmail = FormUtils.findParentLayout(etEmail);
        tilPassword = FormUtils.findParentLayout(etPassword);
        tilConfirmPassword = FormUtils.findParentLayout(etConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLoginLink);
    }

    private void initializeEvents() {
        Runnable registerAction = () -> {
            FormUtils.clearErrors(tilFirstName, tilLastName, tilEmail, tilPassword, tilConfirmPassword);

            FormValidation<RegisterField> result = validateRegister();

            if (result.isValid) {
                performRegistration();
            } else {
                handleValidationError(result);
            }
        };

        btnRegister.setOnClickListener(v -> registerAction.run());

        ViewUtils.onDone(etConfirmPassword, registerAction);

        tvLogin.setOnClickListener(v -> finish());

        FormUtils.clearErrorOnTyping(etFirstName, tilFirstName);
        FormUtils.clearErrorOnTyping(etLastName, tilLastName);
        FormUtils.clearErrorOnTyping(etEmail, tilEmail);
        FormUtils.clearErrorOnTyping(etPassword, tilPassword);
        FormUtils.clearErrorOnTyping(etConfirmPassword, tilConfirmPassword);
    }

    private void handleValidationError(FormValidation<RegisterField> result) {
        switch (result.field) {
            case FIRST_NAME:
                FormUtils.setError(tilFirstName, etFirstName, result.message);
                break;
            case LAST_NAME:
                FormUtils.setError(tilLastName, etLastName, result.message);
                break;
            case EMAIL:
                FormUtils.setError(tilEmail, etEmail, result.message);
                break;
            case PASSWORD:
                FormUtils.setError(tilPassword, etPassword, result.message);
                break;
            case CONFIRM_PASSWORD:
                FormUtils.setError(tilConfirmPassword, etConfirmPassword, result.message);
                break;
        }
    }

    private void performRegistration() {
        ViewUtils.setLoading(btnRegister, "Creating account...");

        String firstName = FormUtils.getText(etFirstName);
        String lastName = FormUtils.getText(etLastName);
        String email = FormUtils.getText(etEmail);
        String password = FormUtils.getText(etPassword);

        AuthService service = SupabaseClient.getInstance(this).create(AuthService.class);
        RegisterRequest request = new RegisterRequest(firstName, lastName, email, password);

        service.register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                ViewUtils.resetButton(btnRegister, "Create Account");

                if (response.isSuccessful() && response.body() != null) {
                    processRegisterSuccess(response.body());
                } else {
                    Log.e(TAG, "onResponse: Registration failed.");
                    FormUtils.setError(tilEmail, etEmail, "Email already exists!");
                    Toast.makeText(RegisterActivity.this, "Registration failed. This email might already be registered.", Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "onFailure: Network error -> " + t.getMessage());
                ViewUtils.resetButton(btnRegister, "Create Account");
                Toast.makeText(RegisterActivity.this, "Network error. Check your connection.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void processRegisterSuccess(AuthResponse data) {
        if (data.getUser() == null) {
            Log.e(TAG, "User object is null in response.");
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
            return;
        }

        if (data.getAccessToken() == null) {
            Toast.makeText(this, "Account created! Please check your email to confirm.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

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
        overridePendingTransition(0,0);
    }

    private FormValidation<RegisterField> validateRegister() {
        String firstName = FormUtils.getText(etFirstName);
        if (firstName.isEmpty()) {
            return FormValidation.error(RegisterField.FIRST_NAME, "First name is required!");
        }

        String lastName = FormUtils.getText(etLastName);
        if (lastName.isEmpty()) {
            return FormValidation.error(RegisterField.LAST_NAME, "Last name is required!");
        }

        String email = FormUtils.getText(etEmail);
        if (email.isEmpty()) {
            return FormValidation.error(RegisterField.EMAIL, "Email is required!");
        }
        if (!email.matches(EMAIL_REGEX)){
            return FormValidation.error(RegisterField.EMAIL, "Please use your @ie.ase.ro address!");
        }

        String password = FormUtils.getText(etPassword);
        if (password.isEmpty()) {
            return FormValidation.error(RegisterField.PASSWORD, "Password is required!");
        }
        if (password.length() < 6) {
            return FormValidation.error(RegisterField.PASSWORD, "Password must be at least 6 characters!");
        }

        String confirm = FormUtils.getText(etConfirmPassword);
        if (confirm.isEmpty()){
            return FormValidation.error(RegisterField.CONFIRM_PASSWORD, "Please confirm your password!");
        }
        if (!confirm.equals(password)) {
            return FormValidation.error(RegisterField.CONFIRM_PASSWORD, "Passwords do not match!");
        }
        return FormValidation.valid();
    }
}