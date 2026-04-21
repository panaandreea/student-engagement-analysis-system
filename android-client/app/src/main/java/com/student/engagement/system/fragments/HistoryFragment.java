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
import com.student.engagement.system.adapters.SessionAdapter;
import com.student.engagement.system.models.response.HistoryResponse;
import com.student.engagement.system.networking.SupabaseClient;
import com.student.engagement.system.services.HistoryService;
import com.student.engagement.system.session.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryFragment extends Fragment {
    private static final String TAG = "HISTORY";
    private RecyclerView rvSessions;
    private SessionAdapter adapter;
    private final ArrayList<HistoryResponse> statsList = new ArrayList<>();
    private TextView tvTotalSessionsValue, tvBestSessionValue, tvWorstSessionValue;
    private TextView tvBestSessionDate, tvWorstSessionDate;

    public HistoryFragment() {
        super(R.layout.fragment_history);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");

        initializeUI(view);
        setupRecyclerView();
        loadHistory();
    }

    private void initializeUI(View view) {
        rvSessions = view.findViewById(R.id.rvSessions);
        tvTotalSessionsValue = view.findViewById(R.id.tvTotalSessionsValue);
        tvBestSessionValue = view.findViewById(R.id.tvBestSessionValue);
        tvWorstSessionValue = view.findViewById(R.id.tvWorstSessionValue);
        tvBestSessionDate = view.findViewById(R.id.tvBestSessionDate);
        tvWorstSessionDate = view.findViewById(R.id.tvWorstSessionDate);
    }

    private void setupRecyclerView() {
        adapter = new SessionAdapter(statsList, this);
        rvSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSessions.setAdapter(adapter);
    }

    private void loadHistory() {
        String teacherId = SessionManager.getUserId(requireContext());
        Log.d(TAG, "Loading history for: " + teacherId);

        if (teacherId == null || teacherId.isEmpty()) {
            Log.e(TAG, "Teacher ID null!");
            return;
        }

        HistoryService service = SupabaseClient.getInstance(requireContext())
                .create(HistoryService.class);

        service.getStats("eq." + teacherId, "*", "start_time.desc").enqueue(new Callback<List<HistoryResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<HistoryResponse>> call,
                                   @NonNull Response<List<HistoryResponse>> response) {

                Log.d(TAG, "API response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    List<HistoryResponse> data = response.body();
                    Log.d(TAG, "Data size: " + data.size());

                    updateUIWithData(data);
                } else {
                    Log.e(TAG, "API error: " + response.code());
                    Toast.makeText(requireContext(), "Failed to load history", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<HistoryResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "Network error", t);
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIWithData(List<HistoryResponse> data) {
        statsList.clear();
        statsList.addAll(data);
        adapter.notifyDataSetChanged();

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
        HistoryResponse best = null;
        HistoryResponse worst = null;

        int maxHigh = -1;
        int maxLow = -1;

        for (HistoryResponse s : stats) {

            if (s.getHigh() > maxHigh) {
                maxHigh = s.getHigh();
                best = s;
            }

            if (s.getLow() > maxLow) {
                maxLow = s.getLow();
                worst = s;
            }
        }

        Log.d(TAG, "Best HIGH = " + maxHigh);
        Log.d(TAG, "Worst LOW = " + maxLow);

        if (best != null && maxHigh > 0) {
            tvBestSessionValue.setText(best.getGroupCode());
            tvBestSessionDate.setText(formatDateForUI(best.getStartTime()));
        } else {
            tvBestSessionValue.setText("-");
            tvBestSessionDate.setText("-");
        }

        if (worst != null && maxLow > 0) {
            tvWorstSessionValue.setText(worst.getGroupCode());
            tvWorstSessionDate.setText(formatDateForUI(worst.getStartTime()));
        } else {
            tvWorstSessionValue.setText("-");
            tvWorstSessionDate.setText("-");
        }
    }

    private String formatDateForUI(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "-";

        try {
            String cleanDate = dateStr.replace("T", " ")
                    .split("\\.")[0]
                    .split("\\+")[0]
                    .trim();

            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));

            Date date = parser.parse(cleanDate);
            if (date == null) return "-";

            SimpleDateFormat formatter = new SimpleDateFormat("dd MMM • HH:mm", Locale.getDefault());
            formatter.setTimeZone(TimeZone.getDefault());

            return formatter.format(date);

        } catch (Exception e) {
            Log.e(TAG, "Date parse error: " + dateStr, e);
            return "-";
        }
    }
}