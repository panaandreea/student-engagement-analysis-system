package com.student.engagement.system.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.student.engagement.system.R;
import com.student.engagement.system.models.requests.SessionRequest;
import com.student.engagement.system.models.requests.TeacherSubjectRequest;
import com.student.engagement.system.models.response.SubjectResponse;
import com.student.engagement.system.models.response.TeacherSubjectResponse;
import com.student.engagement.system.networking.SupabaseClient;
import com.student.engagement.system.services.SessionService;
import com.student.engagement.system.services.SubjectService;
import com.student.engagement.system.session.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SessionFragment extends Fragment {
    private static final String TAG = "SESSION";
    private AutoCompleteTextView spSubject, spSessionType, spSessionIntervals;
    private TextInputEditText etStudentGroup, etDate, etTotalObservationsPlanned;
    private TextInputLayout tilSubject, tilGroup, tilDate, tilInterval, tilType, tilObs;
    private MaterialButton btnCreate;
    private Calendar selectedDate = Calendar.getInstance();
    private List<SubjectResponse> subjectsList = new ArrayList<>();


    public SessionFragment() {
        super(R.layout.fragment_session);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: Initializing Session UI");

        initializeControls(view);
        setupStaticDropdowns();
        fetchSubjectsFromApi();

        btnCreate.setOnClickListener(v -> {
            Log.d(TAG, "Create button clicked");
            createSessionFlow();
        });
    }

    private void initializeControls(View view) {
        spSubject = view.findViewById(R.id.spSubject);
        spSessionType = view.findViewById(R.id.spSessionType);
        spSessionIntervals = view.findViewById(R.id.spSessionIntervals);
        etStudentGroup = view.findViewById(R.id.etStudentGroup);
        etDate = view.findViewById(R.id.etDate);
        etTotalObservationsPlanned = view.findViewById(R.id.etTotalObservationsPlanned);
        btnCreate = view.findViewById(R.id.btnCreateSession);

        tilSubject = findParentTextInputLayout(spSubject);
        tilGroup = findParentTextInputLayout(etStudentGroup);
        tilDate = findParentTextInputLayout(etDate);
        tilInterval = findParentTextInputLayout(spSessionIntervals);
        tilType = findParentTextInputLayout(spSessionType);
        tilObs = findParentTextInputLayout(etTotalObservationsPlanned);

        etDate.setOnClickListener(v -> showDatePicker());
        if (tilDate != null) {
            tilDate.setEndIconOnClickListener(v -> showDatePicker());
        }

        etTotalObservationsPlanned.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                createSessionFlow();
                return true;
            }
            return false;
        });
    }

    private TextInputLayout findParentTextInputLayout(View view) {
        ViewParent parent = view.getParent();
        while (parent != null) {
            if (parent instanceof TextInputLayout) {
                return (TextInputLayout) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private void setupStaticDropdowns() {
        String[] types = getResources().getStringArray(R.array.session_types);
        spSessionType.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, types));

        String[] intervals = getResources().getStringArray(R.array.session_intervals);
        spSessionIntervals.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, intervals));
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            selectedDate.set(year, month, day);

            String formatted = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(selectedDate.getTime());
            etDate.setText(formatted);

            Log.d(TAG, "Date selected: " + formatted);

            if (tilDate != null) tilDate.setError(null);
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH));

        dialog.show();
    }

    private void fetchSubjectsFromApi() {
        Log.d(TAG, "Fetching subjects from API...");

        SubjectService service = SupabaseClient.getInstance(requireContext()).create(SubjectService.class);
        service.getSubjects().enqueue(new Callback<List<SubjectResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<SubjectResponse>> call, @NonNull Response<List<SubjectResponse>> response) {
                Log.d(TAG, "Subjects response: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    subjectsList = response.body();
                    Log.d(TAG, "Subjects loaded: " + subjectsList.size());

                    ArrayAdapter<SubjectResponse> adapter =
                            new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, subjectsList);
                    spSubject.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<SubjectResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "Subjects Load Error: " + t.getMessage(), t);
            }
        });
    }

    private void createSessionFlow() {
        clearErrors();
        if (!validateForm()) {
            Log.w(TAG, "Validation failed");
            return;
        }

        btnCreate.setEnabled(false);
        btnCreate.setText("Connecting...");

        String teacherId = SessionManager.getUserId(requireContext());
        String selectedName = spSubject.getText().toString();

        Log.d(TAG, "Teacher ID: " + teacherId);
        Log.d(TAG, "Selected subject name: " + selectedName);

        SubjectResponse selectedSubject = null;
        for (SubjectResponse s : subjectsList) {
            if (s.toString().equals(selectedName)) {
                selectedSubject = s;
                break;
            }
        }

        if (selectedSubject == null) {
            if (tilSubject != null) tilSubject.setError("Invalid subject selected");
            resetButton();
            return;
        }

        Log.d(TAG, "Selected subject ID: " + selectedSubject.getId());

        linkTeacherToSubject(teacherId, selectedSubject.getId());
    }

    private void linkTeacherToSubject(String teacherId, String subjectId) {
        Log.d(TAG, "Link teacher -> subject");

        SessionService service = SupabaseClient.getInstance(requireContext()).create(SessionService.class);
        service.assignSubjectToTeacher(new TeacherSubjectRequest(teacherId, subjectId)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                Log.d(TAG, "assignSubjectToTeacher response: " + response.code());
                fetchTeacherSubjectId(teacherId, subjectId);
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "assignSubjectToTeacher FAILED: " + t.getMessage(), t);
                resetButton();
            }
        });
    }

    private void fetchTeacherSubjectId(String teacherId, String subjectId) {
        Log.d(TAG, "Fetching teacher_subject ID...");

        SessionService service = SupabaseClient.getInstance(requireContext()).create(SessionService.class);
        service.getTeacherSubject("eq." + teacherId, "eq." + subjectId, "id").enqueue(new Callback<List<TeacherSubjectResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<TeacherSubjectResponse>> call, @NonNull Response<List<TeacherSubjectResponse>> response) {
                Log.d(TAG, "teacher_subject response: " + response.code());

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    String id = response.body().get(0).getId();
                    Log.d(TAG, "teacher_subject_id: " + id);
                    sendFinalSession(id);
                } else {
                    Log.e(TAG, "teacher_subject NOT found!");
                    resetButton();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<TeacherSubjectResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "fetchTeacherSubjectId FAILED: " + t.getMessage(), t);
                resetButton();
            }
        });
    }

    private void sendFinalSession(String teacherSubjectId) {
        SessionService service = SupabaseClient.getInstance(requireContext()).create(SessionService.class);

        try {
            String intervalText = spSessionIntervals.getText().toString();

            String[] parts = intervalText.split(" - ");
            String[] startT = parts[0].split(":");
            String[] endT = parts[1].split(":");

            Calendar start = (Calendar) selectedDate.clone();
            Calendar end = (Calendar) selectedDate.clone();

            start.set(Calendar.HOUR_OF_DAY, Integer.parseInt(startT[0]));
            start.set(Calendar.MINUTE, Integer.parseInt(startT[1]));
            end.set(Calendar.HOUR_OF_DAY, Integer.parseInt(endT[0]));
            end.set(Calendar.MINUTE, Integer.parseInt(endT[1]));

            Log.d(TAG, "Start time UTC: " + formatToIsoUTC(start));
            Log.d(TAG, "End time UTC: " + formatToIsoUTC(end));

            SessionRequest request = new SessionRequest(
                    teacherSubjectId,
                    etStudentGroup.getText().toString(),
                    spSessionType.getText().toString(),
                    formatToIsoUTC(start),
                    formatToIsoUTC(end),
                    Integer.parseInt(etTotalObservationsPlanned.getText().toString())
            );

            service.createSession(request).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    Log.d(TAG, "createSession response: " + response.code());

                    if (response.isSuccessful()) {
                        Toast.makeText(requireContext(), "Session Created!", Toast.LENGTH_SHORT).show();
                        clearForm();
                    }

                    resetButton();
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Log.e(TAG, "createSession FAILED: " + t.getMessage(), t);
                    resetButton();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Exception in sendFinalSession: " + e.getMessage(), e);
            resetButton();
        }
    }

    private String formatToIsoUTC(Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(calendar.getTime());
    }

    private void clearErrors() {
        TextInputLayout[] layouts = {tilSubject, tilGroup, tilDate, tilInterval, tilType, tilObs};
        for (TextInputLayout l : layouts) {
            if (l != null) l.setError(null);
        }
    }

    private void resetButton() {
        Log.d(TAG, "Reset button");
        btnCreate.setEnabled(true);
        btnCreate.setText("Start Teaching Session");
    }

    private void clearForm() {
        etStudentGroup.setText("");
        etDate.setText("");
        etTotalObservationsPlanned.setText("");
        spSubject.setText("", false);
        spSessionType.setText("", false);
        spSessionIntervals.setText("", false);

        clearErrors();
    }

    private boolean validateForm() {
        boolean valid = true;

        if (spSubject.getText().toString().isEmpty()) {
            Log.w(TAG, "Subject empty");
            if (tilSubject != null)
                tilSubject.setError("Required");
            valid = false;
        }

        if (etStudentGroup.getText().toString().isEmpty()) {
            Log.w(TAG, "Group empty");
            if (tilGroup != null)
                tilGroup.setError("Required");
            valid = false;
        }

        if (etDate.getText().toString().isEmpty()) {
            Log.w(TAG, "Date empty");
            if (tilDate != null)
                tilDate.setError("Required");
            valid = false;
        }

        if (spSessionIntervals.getText().toString().isEmpty()) {
            Log.w(TAG, "Interval empty");
            if (tilInterval != null)
                tilInterval.setError("Required");
            valid = false;
        }

        if (spSessionType.getText().toString().isEmpty()) {
            Log.w(TAG, "Type empty");
            if (tilType != null)
                tilType.setError("Required");
            valid = false;
        }

        if (etTotalObservationsPlanned.getText().toString().isEmpty()) {
            Log.w(TAG, "Observations empty");
            if (tilObs != null)
                tilObs.setError("Required");
            valid = false;
        }

        Log.d(TAG, "Validation result: " + valid);
        return valid;
    }
}