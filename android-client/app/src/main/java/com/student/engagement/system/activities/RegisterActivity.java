package com.student.engagement.system.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.student.engagement.system.R;
import com.student.engagement.system.models.RegisterRequest;

public class RegisterActivity extends AppCompatActivity {
    private EditText etFirstName;
    private EditText etLastName;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeControls();
        initializeEvents();
        handleIntent();
    }

    private void initializeControls() {
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLoginLink);
    }

    private void initializeEvents() {
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ValidationResult results = validateRegister();
                if(results.validForm){
                    RegisterRequest registerRequest = new RegisterRequest(etFirstName.getText().toString().trim(),
                            etLastName.getText().toString().trim(), etEmail.getText().toString().trim(), etPassword.getText().toString().trim());

                    Intent intent = new Intent(RegisterActivity.this, SessionActivity.class);
                    startActivity(intent);
                    finish();
                }else{
                    switch (results.field){
                        case  FIRST_NAME:
                            etFirstName.setError(results.message);
                            break;

                        case LAST_NAME:
                            etLastName.setError(results.message);
                            break;

                        case EMAIL:
                            etEmail.setError(results.message);
                            break;

                        case PASSWORD:
                            etPassword.setError(results.message);
                            break;

                        case CONFIRM_PASSWORD:
                            etConfirmPassword.setError(results.message);
                            break;
                    }
                }
            }
        });
    }

    private void handleIntent() {
        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private ValidationResult validateRegister(){
        String firstName = etFirstName.getText().toString().trim();
        if(firstName.isEmpty()){
            return ValidationResult.error(Field.FIRST_NAME, "First name is required!");
        }

        String lastName = etLastName.getText().toString().trim();
        if(lastName.isEmpty()){
            return ValidationResult.error(Field.LAST_NAME, "Last name is required!");
        }

        String email = etEmail.getText().toString().trim();
        if(email.isEmpty()){
            return ValidationResult.error(Field.EMAIL, "Email is required!");
        }

        String password = etPassword.getText().toString().trim();
        if(password.isEmpty()){
            return ValidationResult.error(Field.PASSWORD, "Password is required!");
        }

        String confirmPassword = etConfirmPassword.getText().toString().trim();
        if(confirmPassword.isEmpty()){
            return ValidationResult.error(Field.CONFIRM_PASSWORD, "Confirm password is required!");
        }

        if(!confirmPassword.equals(password)){
            return ValidationResult.error(Field.CONFIRM_PASSWORD, "Passwords do not match!");
        }

        return ValidationResult.valid();
    }

    private enum Field {FIRST_NAME, LAST_NAME, EMAIL, PASSWORD, CONFIRM_PASSWORD, GENERIC};

    private static class ValidationResult{
        final boolean validForm;
        final Field field;
        final String message;
        private ValidationResult(boolean validForm, Field field, String message) {
            this.validForm = validForm;
            this.field = field;
            this.message = message;
        }
        static ValidationResult valid(){
            return new ValidationResult(true, Field.GENERIC, null);
        }
        static ValidationResult error(Field field, String message){
            return new ValidationResult(false, field, message);
        }
    }
}