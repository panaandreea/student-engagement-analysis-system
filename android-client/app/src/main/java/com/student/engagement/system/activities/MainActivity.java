package com.student.engagement.system.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.student.engagement.system.R;
import com.student.engagement.system.fragments.HistoryFragment;
import com.student.engagement.system.fragments.MonitorFragment;
import com.student.engagement.system.fragments.ProfileFragment;
import com.student.engagement.system.fragments.SessionFragment;
import com.student.engagement.system.models.response.SessionResponse;
import com.student.engagement.system.networking.SupabaseClient;
import com.student.engagement.system.services.MonitorService;
import com.student.engagement.system.session.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MAIN_ACTIVITY";

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigation);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null) {
            loadFragment(new SessionFragment());
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_session) {
                loadFragment(new SessionFragment());
                return true;

            } else if (id == R.id.nav_monitor) {
                checkActiveSessionAndNavigate();
                return true;

            } else if (id == R.id.nav_history) {
                loadFragment(new HistoryFragment());
                return true;

            } else if (id == R.id.nav_profile) {
                loadFragment(new ProfileFragment());
                return true;
            }

            return false;
        });
    }

    private void loadFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void checkActiveSessionAndNavigate() {

        String teacherId = SessionManager.getUserId(this);
        if (teacherId == null || teacherId.isEmpty()) return;

        MonitorService sessionService = SupabaseClient
                .getInstance(this)
                .create(MonitorService.class);

        sessionService.getActiveSession("eq." + teacherId)
                .enqueue(new Callback<List<SessionResponse>>() {

                    @Override
                    public void onResponse(@NonNull Call<List<SessionResponse>> call,
                                           @NonNull Response<List<SessionResponse>> response) {

                        if (isFinishing() || isDestroyed()) return;

                        if (!response.isSuccessful() || response.body() == null) return;

                        List<SessionResponse> sessions = response.body();

                        if (sessions.isEmpty()) {
                            Toast.makeText(MainActivity.this,
                                    "No active session",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        loadFragment(new MonitorFragment());
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<SessionResponse>> call,
                                          @NonNull Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        Log.e(TAG, "Session error", t);

                        Toast.makeText(MainActivity.this,
                                "Error loading session",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}