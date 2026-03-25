package com.student.engagement.system.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.student.engagement.system.R;
import com.student.engagement.system.models.SessionRequest;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class SessionActivity extends AppCompatActivity {
    private EditText etSubject;
    private EditText etStudentGroup;
    private Spinner spCameraIndex;
    private EditText etStartTime;
    private EditText etEndTime;
    private EditText etNumberOfObservations;
    private Button btnSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_session);
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
        etSubject = findViewById(R.id.etSubject);
        etStudentGroup = findViewById(R.id.etStudentGroup);
        spCameraIndex = findViewById(R.id.spCamera);
        etStartTime = findViewById(R.id.etStartTime);
        etEndTime = findViewById(R.id.etEndTime);
        etNumberOfObservations = findViewById(R.id.etNumberOfObservations);
        btnSession = findViewById(R.id.btnCreateSession);
    }

    private void initializeEvents() {
        btnSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ValidationResult results = validateSession();
                if(results.validForm){
                    return;
                }else {
                    switch (results.field){
                        case SUBJECT:
                            etSubject.setError(results.message);
                            break;

                        case STUDENT_GROUP:
                            etStudentGroup.setError(results.message);
                            break;

                        case START_TIME:
                            etStartTime.setError(results.message);
                            break;

                        case END_TIME:
                            etEndTime.setError(results.message);
                            break;

                        case NUMBER_OF_OBSERVATIONS:
                            etNumberOfObservations.setError(results.message);
                    }
                }
            }
        });
    }

    private void handleIntent() {

    }

    private ValidationResult validateSession(){
        String subject = etSubject.getText().toString().trim();
        if(subject.isEmpty()){
            return ValidationResult.error(Field.SUBJECT, "Subject is required!");
        }

        String studentGroup= etStudentGroup.getText().toString().trim();
        if(studentGroup.isEmpty()){
            return ValidationResult.error(Field.STUDENT_GROUP, "Group is required!");
        }

        String startTimeStr = etStartTime.getText().toString().trim();
        if(startTimeStr.isEmpty()){
            return ValidationResult.error(Field.START_TIME, "Start time is required!");
        }

        LocalTime startTime = parseTime(startTimeStr);

        String endTimeStr = etEndTime.getText().toString().trim();
        if(endTimeStr.isEmpty()){
            return ValidationResult.error(Field.END_TIME, "End time is required!");
        }

        LocalTime endTime = parseTime(endTimeStr);

        if (endTime != null && endTime.isBefore(startTime)) {
            return ValidationResult.error(Field.END_TIME, "End time must be after start time!");
        }

        String numberOfObservationsStr = etNumberOfObservations.getText().toString().trim();
        if(numberOfObservationsStr.isEmpty()){
            return ValidationResult.error(Field.NUMBER_OF_OBSERVATIONS, "Number of runs is required!");
        }

        int numberOfObservations = Integer.parseInt(numberOfObservationsStr);
        if(numberOfObservations <= 0){
            return ValidationResult.error(Field.NUMBER_OF_OBSERVATIONS, "Number of observations must be a positive number!");
        }

        int cameraIndex = spCameraIndex.getSelectedItemPosition();

        SessionRequest sessionRequest = new SessionRequest(subject, studentGroup, cameraIndex, startTimeStr, endTimeStr, numberOfObservations);

        return ValidationResult.validForm();
    }


    private enum Field {SUBJECT, STUDENT_GROUP, START_TIME, END_TIME, NUMBER_OF_OBSERVATIONS, GENERIC};

    private static class ValidationResult{
        final boolean validForm;
        final Field field;
        final String message;

        private ValidationResult(boolean validForm, Field field, String message) {
            this.validForm = validForm;
            this.field = field;
            this.message = message;
        }

        static ValidationResult validForm(){
            return new ValidationResult(true, Field.GENERIC, null);
        }

        static ValidationResult error(Field field, String message){
            return new ValidationResult(false, field, message);
        }
    }

    private LocalTime parseTime(String hour) {
        try {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
            return LocalTime.parse(hour, dtf);
        }catch (Exception e){
            return null;
        }
    }
}