package com.student.engagement.system.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.student.engagement.system.validation.FormField;
import com.student.engagement.system.utils.FormUtils;
import com.student.engagement.system.validation.FormValidation;
import com.student.engagement.system.utils.ViewUtils;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SecurityActivity extends AppCompatActivity {

    private static final String TAG = "SECURITY";

    public enum SecurityField implements FormField {NEW_PASSWORD, CONFIRM_PASSWORD}

    private TextInputLayout tilNewPassword, tilConfirmPassword;

    private TextInputEditText etNewPassword, etConfirmPassword;

    private MaterialButton btnSave, btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        tilNewPassword = FormUtils.findParentLayout(etNewPassword);
        tilConfirmPassword = FormUtils.findParentLayout(etConfirmPassword);

        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void initializeEvents() {
        btnCancel.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            FormUtils.clearErrors(tilNewPassword, tilConfirmPassword);

            FormValidation<SecurityField> result = validateForm();

            if (result.isValid) {
                performUpdatePassword(FormUtils.getText(etNewPassword));
            } else {
                handleValidationError(result);
            }
        });

        ViewUtils.onDone(etConfirmPassword, () -> btnSave.performClick());

        FormUtils.clearErrorOnTyping(etNewPassword, tilNewPassword);
        FormUtils.clearErrorOnTyping(etConfirmPassword, tilConfirmPassword);
    }

    private void handleValidationError(FormValidation<SecurityField> result) {
        if (result.field == SecurityField.NEW_PASSWORD) {
            FormUtils.setError(tilNewPassword, etNewPassword, result.message);
        } else if (result.field == SecurityField.CONFIRM_PASSWORD) {
            FormUtils.setError(tilConfirmPassword, etConfirmPassword, result.message);
        }
    }

    private void performUpdatePassword(String newPassword) {
        ViewUtils.setLoading(btnSave, "Updating...");

        AuthService authService = SupabaseClient.getInstance(this).create(AuthService.class);
        Map<String, String> body = new HashMap<>();
        body.put("password", newPassword);

        authService.updatePassword(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {

                if (response.isSuccessful()) {
                    Toast.makeText(SecurityActivity.this, "Password updated! Please login again.", Toast.LENGTH_LONG).show();

                    SessionManager.clearSession(SecurityActivity.this);
                    SupabaseClient.reset();

                    Intent intent = new Intent(SecurityActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Log.e(TAG, "onResponse: Update failed. Error body: " + response.errorBody());
                    ViewUtils.resetButton(btnSave, "Save Changes");
                    Toast.makeText(SecurityActivity.this, "Update failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "onFailure: Network Error -> " + t.getMessage());
                ViewUtils.resetButton(btnSave, "Save Changes");
                Toast.makeText(SecurityActivity.this, "Network Error. Check connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private FormValidation<SecurityField> validateForm() {
        String pass = FormUtils.getText(etNewPassword);
        String conf = FormUtils.getText(etConfirmPassword);

        if (pass.isEmpty()) {
            return FormValidation.error(SecurityField.NEW_PASSWORD, "New password is required");
        }

        if (pass.length() < 6) {
            return FormValidation.error(SecurityField.NEW_PASSWORD, "Password must be at least 6 characters");
        }

        if (conf.isEmpty()) {
            return FormValidation.error(SecurityField.CONFIRM_PASSWORD, "Please confirm your password!");
        }

        if (!pass.equals(conf)) {
            return FormValidation.error(SecurityField.CONFIRM_PASSWORD, "Passwords do not match");
        }
        return FormValidation.valid();
    }
}