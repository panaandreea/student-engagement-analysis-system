package com.student.engagement.system.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
import com.student.engagement.system.models.response.SessionResponse;
import com.student.engagement.system.models.response.SubjectResponse;
import com.student.engagement.system.models.response.TeacherSubjectResponse;
import com.student.engagement.system.networking.SupabaseClient;
import com.student.engagement.system.services.ScheduledSessionService;
import com.student.engagement.system.services.SessionService;
import com.student.engagement.system.services.SubjectService;
import com.student.engagement.system.services.TeacherSubjectService;
import com.student.engagement.system.session.SessionManager;
import com.student.engagement.system.utils.DateUtils;
import com.student.engagement.system.validation.FormField;
import com.student.engagement.system.utils.FormUtils;
import com.student.engagement.system.validation.FormValidation;
import com.student.engagement.system.utils.ViewUtils;

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SessionFragment extends Fragment {

    private static final String TAG = "SESSION";

    public enum SessionField implements FormField {SUBJECT, GROUP, DATE, START_TIME, END_TIME, TYPE, CLASSROOM}

    private AutoCompleteTextView actvSubject, actvSessionType, actvRoom;

    private TextInputEditText etGroup, etSessionDateTime, etSessionStartTime, etSessionEndTime;

    private TextInputLayout tilSubject, tilGroup, tilDate, tilStartTime, tilEndTime, tilType, tilRoom;

    private MaterialButton btnCreate;

    private final Calendar selectedDate = Calendar.getInstance();

    private List<SubjectResponse> subjectsList = new ArrayList<>();

    private boolean isEditMode = false;

    private String sessionId = null;


    public SessionFragment() {
        super(R.layout.fragment_session);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (!isAdded()) return;

        initializeControls(view);
        setupDropdowns();
        fetchSubjects();
        checkEditMode();

        btnCreate.setOnClickListener(v -> saveSession());
    }

    private void initializeControls(View view) {
        actvSubject = view.findViewById(R.id.actvSubject);
        actvSessionType = view.findViewById(R.id.actvSessionType);
        actvRoom = view.findViewById(R.id.actvClassroom);

        etGroup = view.findViewById(R.id.etGroup);
        etSessionDateTime = view.findViewById(R.id.etSessionDate);
        etSessionStartTime = view.findViewById(R.id.etSessionStartTime);
        etSessionEndTime = view.findViewById(R.id.etSessionEndTime);

        btnCreate = view.findViewById(R.id.btnCreateSession);

        tilSubject = FormUtils.findParentLayout(actvSubject);
        tilGroup = FormUtils.findParentLayout(etGroup);
        tilDate = FormUtils.findParentLayout(etSessionDateTime);
        tilStartTime = FormUtils.findParentLayout(etSessionStartTime);
        tilEndTime = FormUtils.findParentLayout(etSessionEndTime);
        tilType = FormUtils.findParentLayout(actvSessionType);
        tilRoom = FormUtils.findParentLayout(actvRoom);

        etSessionDateTime.setOnClickListener(v -> showDatePicker());
        etSessionStartTime.setOnClickListener(v -> showTimePicker(etSessionStartTime));
        etSessionEndTime.setOnClickListener(v -> showTimePicker(etSessionEndTime));

        FormUtils.clearErrorOnTyping(etGroup, tilGroup);
        FormUtils.clearErrorOnTyping(etSessionDateTime, tilDate);
        FormUtils.clearErrorOnTyping(etSessionStartTime, tilStartTime);
        FormUtils.clearErrorOnTyping(etSessionEndTime, tilEndTime);
    }

    private void setupDropdowns() {
        Context context = getContext();
        if (context == null) return;

        actvSessionType.setAdapter(new ArrayAdapter<>(
                context,
                android.R.layout.simple_dropdown_item_1line,
                getResources().getStringArray(R.array.session_types)
        ));

        actvRoom.setAdapter(new ArrayAdapter<>(
                context,
                android.R.layout.simple_dropdown_item_1line,
                getResources().getStringArray(R.array.session_rooms)
        ));
    }

    private void fetchSubjects() {
        Context context = getContext();
        if (context == null) return;

        SubjectService service = SupabaseClient.getInstance(context).create(SubjectService.class);

        service.getSubjects().enqueue(new Callback<List<SubjectResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<SubjectResponse>> call,
                                   @NonNull Response<List<SubjectResponse>> response) {

                if (!isAdded()) return;
                Context context = getContext();
                if (context == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    subjectsList = response.body();

                    actvSubject.setAdapter(new ArrayAdapter<>(
                            context,
                            android.R.layout.simple_dropdown_item_1line,
                            subjectsList
                    ));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<SubjectResponse>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Context context = getContext();
                if (context == null) return;

                Log.e(TAG, "Subjects error", t);
                Toast.makeText(context, "Failed to load subjects", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkEditMode() {

        if (getArguments() != null &&
                getArguments().containsKey("session")) {

            isEditMode = true;

            SessionResponse session =
                    (SessionResponse) getArguments()
                            .getSerializable("session");

            if (session != null) {
                sessionId = session.getId();
                btnCreate.setText("Update Session");
                prefillForm(session);
            }

        } else {
            btnCreate.setText("Create Session");
        }
    }

    private void showDatePicker() {
        Context context = getContext();
        if (context == null) return;

        new DatePickerDialog(context, (v, y, m, d) -> {
            selectedDate.set(y, m, d);
            etSessionDateTime.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(selectedDate.getTime()));
        }, selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(TextInputEditText target) {
        Context context = getContext();
        if (context == null) return;

        Calendar now = Calendar.getInstance();

        new TimePickerDialog(context, (v, h, min) ->
                target.setText(String.format(Locale.getDefault(), "%02d:%02d", h, min)),
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true
        ).show();
    }

    private void prefillForm(SessionResponse session) {
        actvSubject.setText(session.getSubjectName(), false);
        etGroup.setText(session.getGroupCode());
        actvSessionType.setText(session.getSessionType(), false);
        actvRoom.setText(session.getClassroom(), false);

        etSessionStartTime.setText(DateUtils.formatIsoToTime(session.getStartTime()));
        etSessionEndTime.setText(DateUtils.formatIsoToTime(session.getEndTime()));

        try {
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(session.getStartTime());
            selectedDate.setTime(Date.from(offsetDateTime.toInstant()));
            etSessionDateTime.setText(DateUtils.formatIsoToDateTime(session.getStartTime()));
        } catch (Exception e) {
            Log.e(TAG, "Date prefill error", e);
        }
    }

    private void clearForm() {
        actvSubject.setText("", false);
        etGroup.setText("");
        etSessionDateTime.setText("");
        etSessionStartTime.setText("");
        etSessionEndTime.setText("");
        actvRoom.setText("", false);
        actvSessionType.setText("", false);

        selectedDate.setTimeInMillis(System.currentTimeMillis());
        FormUtils.clearErrors(tilSubject, tilGroup, tilDate, tilStartTime, tilEndTime, tilType, tilRoom);
    }

    private FormValidation<SessionField> validateForm() {
        if (TextUtils.isEmpty(actvSubject.getText())) {
            return FormValidation.error(SessionField.SUBJECT, "Subject is required!");
        }

        if (TextUtils.isEmpty(etGroup.getText())) {
            return FormValidation.error(SessionField.GROUP, "Group is required!");
        }

        if (TextUtils.isEmpty(etSessionDateTime.getText())) {
            return FormValidation.error(SessionField.DATE, "Date is required!");
        }

        if (TextUtils.isEmpty(etSessionStartTime.getText())) {
            return FormValidation.error(SessionField.START_TIME, "Start time is required!");
        }

        if (TextUtils.isEmpty(etSessionEndTime.getText())) {
            return FormValidation.error(SessionField.END_TIME, "End time is required!");
        }

        if (TextUtils.isEmpty(actvRoom.getText())) {
            return FormValidation.error(SessionField.CLASSROOM, "Class is required!");
        }

        if (TextUtils.isEmpty(actvSessionType.getText())) {
            return FormValidation.error(SessionField.TYPE, "Type is required!");
        }

        try {
            String[] start = etSessionStartTime.getText().toString().split(":");
            String[] end = etSessionEndTime.getText().toString().split(":");

            int startMin = Integer.parseInt(start[0]) * 60 + Integer.parseInt(start[1]);
            int endMin = Integer.parseInt(end[0]) * 60 + Integer.parseInt(end[1]);

            if (endMin <= startMin) {
                return FormValidation.error(SessionField.END_TIME, "End time must be after start time!");
            }
        } catch (Exception e) {
            return FormValidation.error(SessionField.END_TIME, "Invalid time format");
        }

        return FormValidation.valid();
    }

    private void handleValidationError(FormValidation<SessionField> result) {
        switch (result.field) {
            case SUBJECT:
                FormUtils.setError(tilSubject, null, result.message);
                break;
            case GROUP:
                FormUtils.setError(tilGroup, etGroup, result.message);
                break;
            case DATE:
                FormUtils.setError(tilDate, etSessionDateTime, result.message);
                break;
            case START_TIME:
                FormUtils.setError(tilStartTime, etSessionStartTime, result.message);
                break;
            case END_TIME:
                FormUtils.setError(tilEndTime, etSessionEndTime, result.message);
                break;
            case TYPE:
                FormUtils.setError(tilType, null, result.message);
                break;
            case CLASSROOM:
                FormUtils.setError(tilRoom, null, result.message);
                break;
        }
    }

    private void saveSession() {
        FormUtils.clearErrors(tilSubject, tilGroup, tilDate, tilStartTime, tilEndTime, tilType, tilRoom);

        FormValidation<SessionField> result = validateForm();

        if (!result.isValid) {
            handleValidationError(result);
            return;
        }

        ViewUtils.setLoading(btnCreate, isEditMode ? "Updating..." : "Creating...");

        Context context = getContext();
        if (context == null) return;

        String teacherId = SessionManager.getUserId(context);

        SubjectResponse subject = getSelectedSubject();

        if (teacherId == null || teacherId.isEmpty() || subject == null) {
            ViewUtils.resetButton(btnCreate, "Create Session");
            return;
        }

        linkTeacherToSubject(teacherId, subject.getId());
    }

    private SubjectResponse getSelectedSubject() {
        String name = actvSubject.getText().toString().trim();

        for (SubjectResponse subject : subjectsList) {
            if (subject.toString().equals(name)) {
                return subject;
            }
        }
        return null;
    }

    private SessionRequest buildSessionRequest(String teacherSubjectId) {
        String startText = etSessionStartTime.getText().toString();
        String endText = etSessionEndTime.getText().toString();

        if (!startText.contains(":") || !endText.contains(":")) return null;

        String[] startParts = startText.split(":");
        String[] endParts = endText.split(":");

        Calendar start = (Calendar) selectedDate.clone();
        Calendar end = (Calendar) selectedDate.clone();

        start.set(Calendar.HOUR_OF_DAY, Integer.parseInt(startParts[0]));
        start.set(Calendar.MINUTE, Integer.parseInt(startParts[1]));
        start.set(Calendar.SECOND, 0);

        end.set(Calendar.HOUR_OF_DAY, Integer.parseInt(endParts[0]));
        end.set(Calendar.MINUTE, Integer.parseInt(endParts[1]));
        end.set(Calendar.SECOND, 0);

        return new SessionRequest(
                teacherSubjectId,
                etGroup.getText().toString().trim(),
                DateUtils.formatToIsoUTC(start.getTime()),
                DateUtils.formatToIsoUTC(end.getTime()),
                actvRoom.getText().toString().trim(),
                actvSessionType.getText().toString().trim()
        );
    }

    private void linkTeacherToSubject(String teacherId, String subjectId) {
        Context context = getContext();
        if (context == null) return;

        TeacherSubjectService service = SupabaseClient.getInstance(context).create(TeacherSubjectService.class);

        service.assignSubjectToTeacher(new TeacherSubjectRequest(teacherId, subjectId))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        if (!isAdded()) return;

                        fetchTeacherSubjectId(teacherId, subjectId);
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        if (!isAdded()) return;

                        ViewUtils.resetButton(btnCreate, isEditMode ? "Update Session" : "Create Session");
                    }
                });
    }

    private void fetchTeacherSubjectId(String teacherId, String subjectId) {
        Context context = getContext();
        if (context == null) return;

        TeacherSubjectService service = SupabaseClient.getInstance(context).create(TeacherSubjectService.class);

        service.getTeacherSubject("eq." + teacherId, "eq." + subjectId, "id")
                .enqueue(new Callback<List<TeacherSubjectResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<TeacherSubjectResponse>> call,
                                           @NonNull Response<List<TeacherSubjectResponse>> response) {

                        if (!isAdded()) return;
                        Context context = getContext();
                        if (context == null) return;

                        if (response.body() == null || response.body().isEmpty()) {
                            Toast.makeText(context, "Failed to link subject", Toast.LENGTH_SHORT).show();
                            ViewUtils.resetButton(btnCreate, "Retry");
                            return;
                        }
                        SessionRequest sessionRequest = buildSessionRequest(response.body().get(0).getId());

                        if (sessionRequest == null) {
                            Toast.makeText(context, "Invalid time format", Toast.LENGTH_SHORT).show();
                            ViewUtils.resetButton(btnCreate, "Retry");
                            return;
                        }

                        if (isEditMode) {
                            updateSession(sessionRequest);
                        } else {
                            createSession(sessionRequest);
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<List<TeacherSubjectResponse>> call, @NonNull Throwable t) {
                        ViewUtils.resetButton(btnCreate, "Retry");
                    }
                });
    }

    private void createSession(SessionRequest sessionRequest) {

        Context context = getContext();
        if (context == null) return;

        SessionService sessionService = SupabaseClient.getInstance(context).create(SessionService.class);

        sessionService.checkClassroomAvailability("eq." + sessionRequest.getClassroom()).enqueue(new Callback<List<SessionResponse>>() {

            @Override
            public void onResponse(@NonNull Call<List<SessionResponse>> call, @NonNull Response<List<SessionResponse>> response) {

                if (!isAdded()) return;

                Context context = getContext();
                if (context == null) return;

                if (!response.isSuccessful()
                        || response.body() == null) {

                    Toast.makeText(context, "Error checking classroom", Toast.LENGTH_LONG).show();

                    ViewUtils.resetButton(btnCreate, isEditMode ? "Update Session" : "Create Session");

                    return;
                }

                boolean conflict = false;

                OffsetDateTime newStart = OffsetDateTime.parse(sessionRequest.getStartTime());

                OffsetDateTime newEnd = OffsetDateTime.parse(sessionRequest.getEndTime());

                for (SessionResponse session : response.body()) {

                    OffsetDateTime existingStart = OffsetDateTime.parse(session.getStartTime());

                    OffsetDateTime existingEnd = OffsetDateTime.parse(session.getEndTime());

                    if (newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart)) {
                        conflict = true;
                        break;
                    }
                }

                if (conflict) {

                    Toast.makeText(context, "Classroom is not available", Toast.LENGTH_LONG).show();

                    ViewUtils.resetButton(btnCreate, isEditMode ? "Update Session" : "Create Session");

                    return;
                }

                sessionService.createSession(sessionRequest).enqueue(new Callback<Void>() {

                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {

                        if (!isAdded()) return;

                        Context context = getContext();
                        if (context == null) return;

                        if (response.isSuccessful()) {
                            Toast.makeText(context, "Session created!", Toast.LENGTH_LONG).show();
                            clearForm();
                        } else {
                            Toast.makeText(context, "Create failed: " + response.code(), Toast.LENGTH_LONG).show();
                        }
                            ViewUtils.resetButton(btnCreate, isEditMode ? "Update Session" : "Create Session");
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        if (!isAdded()) return;

                        Context context = getContext();
                        if (context == null) return;

                        Log.e(TAG, "createSession error", t);

                        Toast.makeText(context, "Create failed", Toast.LENGTH_LONG).show();

                        ViewUtils.resetButton(btnCreate, isEditMode ? "Update Session" : "Create Session");
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<List<SessionResponse>> call, @NonNull Throwable t) {
                if (!isAdded()) return;

                Context context = getContext();
                if (context == null) return;

                Log.e(TAG, "Classroom check error", t);

                Toast.makeText(context, "Error checking classroom", Toast.LENGTH_LONG).show();

                ViewUtils.resetButton(btnCreate, isEditMode ? "Update Session" : "Create Session");
            }
        });
    }


    private void updateSession(SessionRequest request) {
        Context context = getContext();
        if (context == null) return;

        ScheduledSessionService service = SupabaseClient.getInstance(context).create(ScheduledSessionService.class);

        service.updateSession("eq." + sessionId, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {

                if (!isAdded()) return;
                Context context = getContext();
                if (context == null) return;

                if (response.isSuccessful()) {
                    Toast.makeText(context, "Session updated!", Toast.LENGTH_SHORT).show();
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                } else {
                    Toast.makeText(context, "Update failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }

                ViewUtils.resetButton(btnCreate, isEditMode ? "Update Session" : "Create Session");
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {

                if (!isAdded()) return;
                Context context = getContext();
                if (context == null) return;

                Log.e(TAG, "updateSession error", t);
                Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show();
                ViewUtils.resetButton(btnCreate, isEditMode ? "Update Session" : "Create Session");
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_sessions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (!isAdded()) return false;

        if (item.getItemId() == R.id.scheduled_sessions) {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new ScheduledSessionFragment())
                    .addToBackStack(null)
                    .commit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}