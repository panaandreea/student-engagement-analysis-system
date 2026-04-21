package com.student.engagement.system.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.student.engagement.system.networking.SupabaseClient;
import com.student.engagement.system.services.AuthService;
import com.student.engagement.system.session.SessionManager;
import com.student.engagement.system.utils.Field;
import com.student.engagement.system.utils.ValidationResult;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SecurityActivity extends AppCompatActivity {
    private static final String TAG = "SECURITY";
    public enum SecurityField implements Field {NEW_PASSWORD, CONFIRM_PASSWORD}
    private TextInputLayout tilNewPassword, tilConfirmPassword;
    private TextInputEditText etNewPassword, etConfirmPassword;
    private MaterialButton btnSave, btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Initializing SecurityActivity");

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_security);

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
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        tilNewPassword = findParentTextInputLayout(etNewPassword);
        tilConfirmPassword = findParentTextInputLayout(etConfirmPassword);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void initializeEvents() {
        btnCancel.setOnClickListener(v -> {
            Log.d(TAG, "Cancel clicked. Closing activity.");
            finish();
        });

        btnSave.setOnClickListener(v -> {
            Log.d(TAG, "Save clicked. Starting validation...");
            clearErrors();

            ValidationResult<SecurityField> result = validateForm();

            if (result.isValid) {
                Log.i(TAG, "Validation passed. Sending update request to Supabase.");
                updatePassword(etNewPassword.getText().toString().trim());
            } else {
                Log.w(TAG, "Validation failed: " + result.message);
                handleValidationError(result);
            }
        });
    }

    private void clearErrors() {
        if (tilNewPassword != null) tilNewPassword.setError(null);
        if (tilConfirmPassword != null) tilConfirmPassword.setError(null);
    }

    private void handleValidationError(ValidationResult<SecurityField> result) {
        if (result.field == SecurityField.NEW_PASSWORD && tilNewPassword != null) {
            tilNewPassword.setError(result.message);
            etNewPassword.requestFocus();
        } else if (result.field == SecurityField.CONFIRM_PASSWORD && tilConfirmPassword != null) {
            tilConfirmPassword.setError(result.message);
            etConfirmPassword.requestFocus();
        }
    }

    private void updatePassword(String newPassword) {
        btnSave.setEnabled(false);
        btnSave.setText("Updating...");

        AuthService authService = SupabaseClient.getInstance(this).create(AuthService.class);
        Map<String, String> body = new HashMap<>();
        body.put("password", newPassword);

        Log.d(TAG, "updatePassword: Executing API call...");
        authService.updatePassword(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                Log.d(TAG, "onResponse: Code " + response.code());

                if (response.isSuccessful()) {
                    Log.i(TAG, "Password updated successfully. Logging out for security.");
                    Toast.makeText(SecurityActivity.this, "Password updated! Please login again.", Toast.LENGTH_LONG).show();

                    SessionManager.clearSession(SecurityActivity.this);
                    SupabaseClient.reset();

                    Intent intent = new Intent(SecurityActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Log.e(TAG, "onResponse: Update failed. Error body: " + response.errorBody());
                    resetButton();
                    Toast.makeText(SecurityActivity.this, "Update failed. Try again later.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "onFailure: Network Error -> " + t.getMessage());
                resetButton();
                Toast.makeText(SecurityActivity.this, "Network Error. Check connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetButton() {
        btnSave.setEnabled(true);
        btnSave.setText("Save Changes");
    }

    private ValidationResult<SecurityField> validateForm() {
        String pass = etNewPassword.getText().toString().trim();
        String conf = etConfirmPassword.getText().toString().trim();

        if (pass.isEmpty()) {
            return ValidationResult.error(SecurityField.NEW_PASSWORD, "New password is required");
        }
        if (pass.length() < 6) {
            return ValidationResult.error(SecurityField.NEW_PASSWORD, "Password must be at least 6 characters");
        }
        if (!pass.equals(conf)) {
            return ValidationResult.error(SecurityField.CONFIRM_PASSWORD, "Passwords do not match");
        }

        return ValidationResult.valid();
    }

    private TextInputLayout findParentTextInputLayout(View view) {
        if (view != null && view.getParent() instanceof View) {
            View parent = (View) view.getParent();
            if (parent instanceof TextInputLayout) return (TextInputLayout) parent;
            if (parent.getParent() instanceof TextInputLayout) return (TextInputLayout) parent.getParent();
        }
        return null;
    }
}