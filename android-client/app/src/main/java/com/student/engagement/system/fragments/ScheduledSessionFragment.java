package com.student.engagement.system.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.student.engagement.system.R;
import com.student.engagement.system.adapters.ScheduledSessionAdapter;
import com.student.engagement.system.models.response.SessionResponse;
import com.student.engagement.system.networking.SupabaseClient;
import com.student.engagement.system.services.ScheduledSessionService;
import com.student.engagement.system.session.SessionManager;
import com.student.engagement.system.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScheduledSessionFragment extends Fragment {

    private static final String TAG = "SESSIONS";

    private RecyclerView rvSessions;

    private ScheduledSessionAdapter adapter;

    private final List<SessionResponse> sessionsList = new ArrayList<>();


    public ScheduledSessionFragment() {
        super(R.layout.fragment_scheduled_sessions);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvSessions = view.findViewById(R.id.rvScheduledSessions);

        setupRecyclerView();
    }

    private void setupRecyclerView() {

        adapter = new ScheduledSessionAdapter(
                new ScheduledSessionAdapter.OnSessionActionListener() {

                    @Override
                    public void onEdit(SessionResponse session) {
                        Log.d(TAG, "Edit clicked: " + session.getId());
                        openEditFragment(session);
                    }

                    @Override
                    public void onDelete(SessionResponse session) {
                        showDeleteDialog(session);
                    }
                },
                sessionsList
        );

        rvSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSessions.setAdapter(adapter);
    }

    private void openEditFragment(SessionResponse session) {
        SessionFragment fragment = new SessionFragment();

        Bundle args = new Bundle();
        args.putSerializable("session", session);

        fragment.setArguments(args);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchSessions();
    }

    private void fetchSessions() {
        Context context = getContext();
        if (context == null) return;

        ScheduledSessionService service = SupabaseClient
                .getInstance(context)
                .create(ScheduledSessionService.class);

        service.getScheduledSessions(
                "gt." + DateUtils.getCurrentIsoUTC(),
                "eq." + SessionManager.getUserId(context)
        ).enqueue(new Callback<List<SessionResponse>>() {

            @Override
            public void onResponse(@NonNull Call<List<SessionResponse>> call,
                                   @NonNull Response<List<SessionResponse>> response) {

                if (!isAdded() || getView() == null) return;

                if (response.isSuccessful() && response.body() != null) {

                    adapter.updateData(response.body());

                    Log.d(TAG, "Loaded: " + response.body().size());

                } else {
                    Log.e(TAG, "Fetch failed: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<SessionResponse>> call,
                                  @NonNull Throwable t) {

                if (!isAdded() || getView() == null) return;

                Log.e(TAG, "API error: " + t.getMessage(), t);
            }
        });
    }

    private void showDeleteDialog(SessionResponse session) {
        Context context = getContext();
        if (context == null) return;

        new AlertDialog.Builder(context)
                .setTitle("Delete session")
                .setMessage("Are you sure you want to delete this session?")
                .setPositiveButton("Delete", (dialog, which) -> deleteSession(session))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSession(SessionResponse session) {
        Context context = getContext();
        if (context == null) return;

        ScheduledSessionService service = SupabaseClient.getInstance(context).create(ScheduledSessionService.class);

        service.deleteSession("eq." + session.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    if (!isAdded() || getView() == null) return;
                    Log.d(TAG, "Deleted");
                    fetchSessions();
                } else {
                    Log.e(TAG, "Delete failed: " + response.code());
                }
            }
            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "Delete error: " + t.getMessage(), t);
            }
        });
    }
}