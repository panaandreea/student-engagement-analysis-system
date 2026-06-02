package com.student.engagement.system.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.student.engagement.system.R;
import com.student.engagement.system.adapters.HistorySessionAdapter;
import com.student.engagement.system.models.response.HistoryResponse;
import com.student.engagement.system.networking.SupabaseClient;
import com.student.engagement.system.services.HistoryService;
import com.student.engagement.system.session.SessionManager;
import com.student.engagement.system.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryFragment extends Fragment {

    private static final String TAG = "HISTORY";

    private RecyclerView rvSessions;

    private HistorySessionAdapter adapter;

    private final ArrayList<HistoryResponse> statsList = new ArrayList<>();

    private TextView tvTotalSessionsValue;

    private TextView tvBestSessionValue;

    private TextView tvWorstSessionValue;

    private TextView tvBestSessionDate;

    private TextView tvWorstSessionDate;

    public HistoryFragment() {
        super(R.layout.fragment_history);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeUI(view);
        setupRecyclerView();
        loadHistory();
    }

    private void initializeUI(View view) {
        rvSessions = view.findViewById(R.id.rvHistorySessions);
        tvTotalSessionsValue = view.findViewById(R.id.tvTotalSessionsValue);
        tvBestSessionValue = view.findViewById(R.id.tvBestSessionValue);
        tvWorstSessionValue = view.findViewById(R.id.tvWorstSessionValue);
        tvBestSessionDate = view.findViewById(R.id.tvBestSessionDate);
        tvWorstSessionDate = view.findViewById(R.id.tvWorstSessionDate);
    }

    private void setupRecyclerView() {
        adapter = new HistorySessionAdapter(statsList, this);
        rvSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSessions.setAdapter(adapter);
    }

    private void loadHistory() {
        String teacherId = SessionManager.getUserId(requireContext());

        if (teacherId == null || teacherId.isEmpty()) return;

        HistoryService service = SupabaseClient.getInstance(requireContext())
                .create(HistoryService.class);

        service.getStats("eq." + teacherId, "*", "start_time.desc").enqueue(new Callback<List<HistoryResponse>>() {

            @Override
            public void onResponse(@NonNull Call<List<HistoryResponse>> call, @NonNull Response<List<HistoryResponse>> response) {

                if (!isAdded() || getView() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    updateUIWithData(response.body());
                } else {
                    Toast.makeText(getContext(), "Failed to load history", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<HistoryResponse>> call, @NonNull Throwable t) {

                if (!isAdded() || getView() == null) return;

                Log.e(TAG, "Network error", t);
                Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIWithData(List<HistoryResponse> data) {
        if (!isAdded() || getView() == null) return;

        if (data == null) data = new ArrayList<>();

        adapter.updateData(new ArrayList<>(data));

        tvTotalSessionsValue.setText(String.valueOf(data.size()));

        if (!data.isEmpty()) {
            calculateAndPopulateSummary(data);
        } else {
            tvBestSessionValue.setText("-");
            tvWorstSessionValue.setText("-");
            tvBestSessionDate.setText("-");
            tvWorstSessionDate.setText("-");
        }
    }

    private void calculateAndPopulateSummary(List<HistoryResponse> stats) {
        if (!isAdded() || getView() == null) return;

        HistoryResponse best = null;
        HistoryResponse worst = null;

        float maxHighRatio = -1f;
        float maxLowRatio = -1f;

        boolean hasHigh = false;
        boolean hasLow = false;

        for (HistoryResponse s : stats) {

            if (s == null) continue;

            int low = s.getLow();
            int medium = s.getMedium();
            int high = s.getHigh();

            int total = s.getTotalStudents();

            if (total <= 0) continue;

            if (high > 0) hasHigh = true;
            if (low > 0) hasLow = true;

            float highRatio = (float) high / total;
            float lowRatio = (float) low / total;

            if (highRatio > maxHighRatio) {
                maxHighRatio = highRatio;
                best = s;
            }

            if (lowRatio > maxLowRatio) {
                maxLowRatio = lowRatio;
                worst = s;
            }
        }

        if (!hasHigh || best == null || best.getHigh() == 0) {
            tvBestSessionValue.setText("-");
            tvBestSessionDate.setText("-");
        } else {
            tvBestSessionValue.setText(
                    best.getGroupCode() != null ? best.getGroupCode() : "-"
            );

            if (best.getStartTime() != null && best.getEndTime() != null) {

                String date = DateUtils.formatIsoToDate(best.getStartTime());

                String start =
                        DateUtils.formatIsoToTime(best.getStartTime());

                String end =
                        DateUtils.formatIsoToTime(best.getEndTime());

                tvBestSessionDate.setText(
                      date + "\n" + start + " - " + end
                );

            } else {
                tvBestSessionDate.setText("-");
            }

        }

        if (!hasLow || worst == null || worst.getLow() == 0) {
            tvWorstSessionValue.setText("-");
            tvWorstSessionDate.setText("-");
        } else {
            tvWorstSessionValue.setText(worst.getGroupCode() != null ? worst.getGroupCode() : "-");

            if (worst.getStartTime() != null && worst.getEndTime() != null) {

                String date = DateUtils.formatIsoToDate(worst.getStartTime());

                String start = DateUtils.formatIsoToTime(worst.getStartTime());

                String end = DateUtils.formatIsoToTime(worst.getEndTime());

                tvWorstSessionDate.setText(date + "\n" +start + " - " + end);

            } else {
                tvWorstSessionDate.setText("-");
            }

        }
    }
}