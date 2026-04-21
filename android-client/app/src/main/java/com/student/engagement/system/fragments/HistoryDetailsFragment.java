package com.student.engagement.system.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import com.student.engagement.system.R;
import com.student.engagement.system.models.response.MonitorResponse;
import com.student.engagement.system.models.response.StudentResponse;
import com.student.engagement.system.networking.SupabaseClient;
import com.student.engagement.system.services.MonitorService;
import com.student.engagement.system.services.StudentService;
import com.student.engagement.system.session.SessionManager;

import java.text.SimpleDateFormat;
import java.util.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryDetailsFragment extends Fragment {
    private static final String TAG = "HISTORY_DETAILS";
    private String sessionId, subject, group, startTime;
    private BarChart barChart;
    private TextView tvSubjectGroup, tvSessionTime, tvTotalStudents, tvDominant;
    private ImageButton btnBack;
    private LinearLayout containerStudents;
    private final List<MonitorResponse> snapshotList = new ArrayList<>();

    public HistoryDetailsFragment() {
        super(R.layout.fragment_history_details);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            sessionId = getArguments().getString("session_id");
            subject = getArguments().getString("subject");
            group = getArguments().getString("group");
            startTime = getArguments().getString("start_time");
        }

        Log.d(TAG, "Session ID: " + sessionId);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        barChart = view.findViewById(R.id.barChart);
        tvSubjectGroup = view.findViewById(R.id.tvSubjectGroup);
        tvSessionTime = view.findViewById(R.id.tvSessionTime);
        tvTotalStudents = view.findViewById(R.id.tvTotalStudents);
        tvDominant = view.findViewById(R.id.tvDominant);
        btnBack = view.findViewById(R.id.btnBack);
        containerStudents = view.findViewById(R.id.containerStudents);

        tvSubjectGroup.setText(subject + " - " + group);
        tvSessionTime.setText(formatTime(startTime));

        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        setupChart();

        loadSnapshots();
        loadStudentEvolution();
    }

    private void loadSnapshots() {

        MonitorService service = SupabaseClient.getInstance(requireContext())
                .create(MonitorService.class);

        service.getMonitor(
                "eq." + sessionId,
                "eq." + SessionManager.getUserId(requireContext()),
                "snapshot_index.asc"
        ).enqueue(new Callback<List<MonitorResponse>>() {

            @Override
            public void onResponse(Call<List<MonitorResponse>> call,
                                   Response<List<MonitorResponse>> response) {

                if (response.isSuccessful() && response.body() != null) {

                    snapshotList.clear();
                    snapshotList.addAll(response.body());

                    Log.d(TAG, "Snapshots loaded: " + snapshotList.size());

                    drawChart();
                    updateCards();
                }
            }

            @Override
            public void onFailure(Call<List<MonitorResponse>> call, Throwable t) {
                Log.e(TAG, "Snapshot error", t);
            }
        });
    }

    private void setupChart() {

        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        barChart.getAxisRight().setEnabled(false);
    }

    private void drawChart() {

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int maxTotal = 0;

        for (int i = 0; i < snapshotList.size(); i++) {

            MonitorResponse s = snapshotList.get(i);

            entries.add(new BarEntry(i, new float[]{
                    s.getLow(),
                    s.getMedium(),
                    s.getHigh()
            }));

            labels.add("S" + s.getSnapshotIndex());

            int total = s.getLow() + s.getMedium() + s.getHigh();
            if (total > maxTotal) maxTotal = total;
        }

        if (maxTotal < 6) maxTotal = 6;

        BarDataSet ds = new BarDataSet(entries, "");
        ds.setColors(
                Color.parseColor("#DC2626"),
                Color.parseColor("#D97706"),
                Color.parseColor("#059669")
        );

        BarData data = new BarData(ds);
        data.setBarWidth(0.5f);

        barChart.setData(data);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(labels.size());

        int step = getStep(maxTotal);

        YAxis left = barChart.getAxisLeft();
        left.setAxisMinimum(0f);
        left.setAxisMaximum(roundUp(maxTotal, step));
        left.setGranularity(step);

        left.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        barChart.invalidate();
    }

    private void updateCards() {

        if (snapshotList.isEmpty()) return;

        int totalStudents = 0;
        float totalScore = 0;

        for (MonitorResponse s : snapshotList) {

            int low = s.getLow();
            int med = s.getMedium();
            int high = s.getHigh();

            int total = low + med + high;
            totalStudents += total;

            float score = total == 0 ? 0 :
                    (med * 50f + high * 100f) / total;

            totalScore += score;
        }

        int avgStudents = totalStudents / snapshotList.size();
        float avgScore = totalScore / snapshotList.size();

        tvTotalStudents.setText(String.valueOf(avgStudents));

        if (avgScore >= 66) {
            tvDominant.setText("HIGH");
            tvDominant.setTextColor(Color.parseColor("#059669"));
        } else if (avgScore >= 33) {
            tvDominant.setText("MEDIUM");
            tvDominant.setTextColor(Color.parseColor("#D97706"));
        } else {
            tvDominant.setText("LOW");
            tvDominant.setTextColor(Color.parseColor("#DC2626"));
        }

        Log.d(TAG, "Avg Students: " + avgStudents);
        Log.d(TAG, "Avg Score: " + avgScore);
    }

    private void loadStudentEvolution() {

        StudentService service = SupabaseClient.getInstance(requireContext())
                .create(StudentService.class);

        service.getStudentEvolution(
                "*",
                "eq." + sessionId,
                "student_id.asc,snapshot_index.asc"
        ).enqueue(new Callback<List<StudentResponse>>() {

            @Override
            public void onResponse(Call<List<StudentResponse>> call,
                                   Response<List<StudentResponse>> response) {

                if (response.isSuccessful() && response.body() != null) {
                    renderStudents(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<StudentResponse>> call, Throwable t) {
                Log.e(TAG, "Student error", t);
            }
        });
    }

    private void renderStudents(List<StudentResponse> list) {

        containerStudents.removeAllViews();

        Map<String, List<StudentResponse>> map = new LinkedHashMap<>();

        for (StudentResponse s : list) {
            map.computeIfAbsent(s.getStudentId(), k -> new ArrayList<>()).add(s);
        }

        for (List<StudentResponse> snapshots : map.values()) {

            View item = LayoutInflater.from(getContext())
                    .inflate(R.layout.student_item, containerStudents, false);

            TextView tvId = item.findViewById(R.id.tvId);
            LinearLayout flow = item.findViewById(R.id.containerFlow);

            tvId.setText("Student " + snapshots.get(0).getLocalId());

            for (int i = 0; i < snapshots.size(); i++) {

                TextView badge = new TextView(getContext());
                badge.setPadding(20, 10, 20, 10);
                badge.setText(snapshots.get(i).getAttention().toUpperCase());
                badge.setTextColor(Color.WHITE);

                switch (snapshots.get(i).getAttention()) {
                    case "low":
                        badge.setBackgroundColor(Color.parseColor("#DC2626"));
                        break;
                    case "medium":
                        badge.setBackgroundColor(Color.parseColor("#D97706"));
                        break;
                    case "high":
                        badge.setBackgroundColor(Color.parseColor("#059669"));
                        break;
                }

                flow.addView(badge);

                if (i < snapshots.size() - 1) {
                    TextView arrow = new TextView(getContext());
                    arrow.setText(" → ");
                    flow.addView(arrow);
                }
            }

            containerStudents.addView(item);
        }
    }

    private int getStep(int max) {
        if (max <= 15) return 3;
        if (max <= 30) return 5;
        if (max <= 60) return 10;
        return 20;
    }

    private float roundUp(int value, int step) {
        return ((value + step - 1) / step) * step;
    }

    private String formatTime(String dateStr) {
        try {
            SimpleDateFormat inputFormat =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            Date date = inputFormat.parse(dateStr);

            SimpleDateFormat outputFormat =
                    new SimpleDateFormat("dd MMM • HH:mm", Locale.getDefault());
            outputFormat.setTimeZone(TimeZone.getTimeZone("Europe/Bucharest"));

            return outputFormat.format(date);

        } catch (Exception e) {
            Log.e(TAG, "Date parse error", e);
            return dateStr;
        }
    }
}