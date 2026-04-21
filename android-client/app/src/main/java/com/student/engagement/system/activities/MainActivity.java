package com.student.engagement.system.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.student.engagement.system.R;
import com.student.engagement.system.fragments.HistoryFragment;
import com.student.engagement.system.fragments.MonitorFragment;
import com.student.engagement.system.fragments.ProfileFragment;
import com.student.engagement.system.fragments.SessionFragment;
import com.student.engagement.system.models.response.MonitorResponse;
import com.student.engagement.system.models.response.SessionResponse;
import com.student.engagement.system.models.response.TeacherSubjectResponse;
import com.student.engagement.system.networking.SupabaseClient;
import com.student.engagement.system.services.MonitorService;
import com.student.engagement.system.services.SessionService;
import com.student.engagement.system.session.SessionManager;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MAIN_ACTIVITY";
    private BottomNavigationView bottomNavigationView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int INTERVAL = 10000;
    private boolean alreadyInMonitor = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigation);

        Log.d(TAG, "=== APP START ===");

        if (savedInstanceState == null) {
            loadFragment(new SessionFragment());
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_session) {
                Log.d(TAG, "NAV -> SESSION");
                loadFragment(new SessionFragment());
                return true;

            } else if (id == R.id.nav_monitor) {
                Log.d(TAG, "NAV -> MONITOR CLICK");
                checkActiveSessionAndNavigate(true);
                return true;

            } else if (id == R.id.nav_history) {
                Log.d(TAG, "NAV -> HISTORY");
                loadFragment(new HistoryFragment());
                return true;

            } else if (id == R.id.nav_profile) {
                Log.d(TAG, "NAV -> PROFILE");
                loadFragment(new ProfileFragment());
                return true;
            }

            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "ON RESUME -> start auto check");
        handler.post(sessionChecker);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "ON PAUSE -> stop auto check");
        handler.removeCallbacks(sessionChecker);
    }

    private final Runnable sessionChecker = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "AUTO CHECK (background)");
            checkActiveSessionAndNavigate(false);
            handler.postDelayed(this, INTERVAL);
        }
    };

    private void loadFragment(@NonNull Fragment fragment) {

        if (!(fragment instanceof MonitorFragment)) {
            alreadyInMonitor = false;
        }

        Log.d(TAG, "Loading fragment: " + fragment.getClass().getSimpleName());

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void checkActiveSessionAndNavigate(boolean showToast) {

        String teacherId = SessionManager.getUserId(this);

        if (teacherId == null) {
            Log.e(TAG, "Teacher ID null");
            return;
        }

        Log.d(TAG, "Teacher ID: " + teacherId);

        SessionService sessionService = SupabaseClient.getInstance(this).create(SessionService.class);
        MonitorService monitorService = SupabaseClient.getInstance(this).create(MonitorService.class);

        sessionService.getTeacherSubject("eq." + teacherId, null, "id")
                .enqueue(new Callback<List<TeacherSubjectResponse>>() {

                    @Override
                    public void onResponse(@NonNull Call<List<TeacherSubjectResponse>> call,
                                           @NonNull Response<List<TeacherSubjectResponse>> response) {

                        if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                            Log.e(TAG, "No teacher subjects");
                            return;
                        }

                        List<TeacherSubjectResponse> list = response.body();

                        Log.d(TAG, "Subjects found: " + list.size());

                        StringBuilder ids = new StringBuilder("in.(");
                        for (int i = 0; i < list.size(); i++) {
                            ids.append(list.get(i).getId());
                            if (i < list.size() - 1) ids.append(",");
                        }
                        ids.append(")");

                        String now = OffsetDateTime.now(ZoneOffset.UTC)
                                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

                        Log.d(TAG, "NOW UTC = " + now);

                        sessionService.getActiveSessionsMulti(
                                ids.toString(),
                                "lte." + now,
                                "gte." + now,
                                1
                        ).enqueue(new Callback<List<SessionResponse>>() {

                            @Override
                            public void onResponse(@NonNull Call<List<SessionResponse>> call,
                                                   @NonNull Response<List<SessionResponse>> response) {

                                if (!response.isSuccessful() ||
                                        response.body() == null ||
                                        response.body().isEmpty()) {

                                    Log.d(TAG, "NO ACTIVE SESSION");

                                    if (showToast) {
                                        Toast.makeText(MainActivity.this,
                                                "No active session",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                    return;
                                }

                                SessionResponse session = response.body().get(0);
                                String sessionId = session.getId();

                                Log.d(TAG, "ACTIVE SESSION: " + sessionId);

                                monitorService.getMonitor(
                                        "eq." + sessionId,
                                        "eq." + teacherId,
                                        "snapshot_index.desc"
                                ).enqueue(new Callback<List<MonitorResponse>>() {

                                    @Override
                                    public void onResponse(@NonNull Call<List<MonitorResponse>> call,
                                                           @NonNull Response<List<MonitorResponse>> response) {

                                        if (!response.isSuccessful() || response.body() == null) {
                                            Log.e(TAG, "Monitor API error");
                                            return;
                                        }

                                        List<MonitorResponse> snapshots = response.body();

                                        Log.d(TAG, "Snapshots found: " + snapshots.size());

                                        if (snapshots.isEmpty()) {

                                            Log.d(TAG, "SESSION ACTIVE BUT NO DATA");

                                            if (showToast) {
                                                Toast.makeText(MainActivity.this,
                                                        "Session started, waiting for data...",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                            return;
                                        }

                                        if (showToast) {
                                            Log.d(TAG, "ENTERING MONITOR");
                                            alreadyInMonitor = true;
                                            loadFragment(new MonitorFragment());
                                        }
                                    }

                                    @Override
                                    public void onFailure(@NonNull Call<List<MonitorResponse>> call,
                                                          @NonNull Throwable t) {

                                        Log.e(TAG, "Monitor API error", t);
                                    }
                                });
                            }

                            @Override
                            public void onFailure(@NonNull Call<List<SessionResponse>> call,
                                                  @NonNull Throwable t) {

                                Log.e(TAG, "Session API error", t);
                            }
                        });
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<TeacherSubjectResponse>> call,
                                          @NonNull Throwable t) {

                        Log.e(TAG, "TeacherSubject API error", t);
                    }
                });
    }
}