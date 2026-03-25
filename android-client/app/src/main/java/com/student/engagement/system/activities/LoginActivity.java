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
import com.student.engagement.system.models.LoginRequest;

public class LoginActivity extends AppCompatActivity {
    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private TextView tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
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
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegisterLink);
    }

    private void initializeEvents() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ValidationResult results = validateLogin();
                if(results.validForm){
                    LoginRequest loginRequest = new LoginRequest(etEmail.getText().toString().trim(), etPassword.getText().toString().trim());

                    Intent intent = new Intent(LoginActivity.this, SessionActivity.class);
                    startActivity(intent);
                    finish();
                }else{
                    switch (results.field){
                        case EMAIL:
                            etEmail.setError(results.message);
                            break;

                        case PASSWORD:
                            etPassword.setError(results.message);
                            break;
                    }
                }
            }
        });
    }

    private void handleIntent() {
        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private ValidationResult validateLogin(){
        String email = etEmail.getText().toString().trim();
        if(email.isEmpty()){
            return ValidationResult.error(Field.EMAIL, "Email is required!");
        }

        String password = etPassword.getText().toString().trim();
        if(password.isEmpty()){
            return ValidationResult.error(Field.PASSWORD, "Password is required!");
        }

        return ValidationResult.valid();
    }

    private enum Field {EMAIL, PASSWORD, GENERIC};

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

        static  ValidationResult error(Field field, String message){
            return new ValidationResult(false, field, message);
        }
    }
}