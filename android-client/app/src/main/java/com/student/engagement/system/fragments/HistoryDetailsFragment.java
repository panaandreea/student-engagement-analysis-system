package com.student.engagement.system.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import com.google.android.material.button.MaterialButton;
import com.student.engagement.system.R;
import com.student.engagement.system.models.response.MonitorResponse;
import com.student.engagement.system.models.response.StudentResponse;
import com.student.engagement.system.networking.SupabaseClient;
import com.student.engagement.system.services.HistoryDetailsService;
import com.student.engagement.system.session.SessionManager;
import com.student.engagement.system.utils.DateUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryDetailsFragment extends Fragment {

    private static final String TAG = "HISTORY_DETAILS";

    private LinearLayout layoutChart, layoutHeatmap;

    private ImageView ivHeatmap;
    private TextView tvSnapshotTitle;

    private ImageButton btnPrevSnapshot;

    private ImageButton btnNextSnapshot;

    private int currentSnapshotIndex = 0;

    private MaterialButton btnViewEvolution;

    private MaterialButton btnViewHeatmap;

    private String sessionId, subject, group, startTime, endTime;

    private BarChart barChart;

    private TextView tvSubjectGroup, tvSessionTime, tvTotalStudents, tvDominant;

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
            endTime = getArguments().getString("end_time");
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        barChart = view.findViewById(R.id.barChart);

        layoutChart = view.findViewById(R.id.layoutChart);
        layoutHeatmap = view.findViewById(R.id.layoutHeatmap);

        ivHeatmap = view.findViewById(R.id.ivHeatmap);
        tvSnapshotTitle = view.findViewById(R.id.tvSnapshotTitle);

        btnViewEvolution = view.findViewById(R.id.btnViewEvolution);
        btnViewHeatmap = view.findViewById(R.id.btnViewHeatmap);
        
        btnPrevSnapshot = view.findViewById(R.id.btnPrevSnapshot);
        btnNextSnapshot = view.findViewById(R.id.btnNextSnapshot);

        containerStudents = view.findViewById(R.id.containerStudents);
        tvSubjectGroup = view.findViewById(R.id.tvCourseTitle);
        tvSessionTime = view.findViewById(R.id.tvSessionDateTime);
        tvTotalStudents = view.findViewById(R.id.tvStudentCount);
        tvDominant = view.findViewById(R.id.tvDominantAttention);
        tvSubjectGroup.setText((subject != null ? subject : "-") + " - " + (group != null ? group : "-"));

        if (startTime != null && endTime != null) {

            String date = DateUtils.formatIsoToDate(startTime);

            String start = DateUtils.formatIsoToTime(startTime);

            String end = DateUtils.formatIsoToTime(endTime);

            tvSessionTime.setText(date + " • " + start + " - " + end);

        } else {
            tvSessionTime.setText("-");
        }

        setupChart();
        loadSnapshots();
        loadStudentEvolution();

        btnViewEvolution.setOnClickListener(v -> {
            layoutChart.setVisibility(View.VISIBLE);
            layoutHeatmap.setVisibility(View.GONE);
        });

        btnViewHeatmap.setOnClickListener(v -> {
            layoutChart.setVisibility(View.GONE);
            layoutHeatmap.setVisibility(View.VISIBLE);

            showHeatmap(currentSnapshotIndex);
        });

        btnPrevSnapshot.setOnClickListener(v -> {
            if(snapshotList.isEmpty()) return;

            currentSnapshotIndex--;

            if(currentSnapshotIndex < 0){
                currentSnapshotIndex = snapshotList.size() - 1;
            }

            showHeatmap(currentSnapshotIndex);
        });

        btnNextSnapshot.setOnClickListener(v->{
            if(snapshotList.isEmpty()) return;

            currentSnapshotIndex++;

            if(currentSnapshotIndex >= snapshotList.size()){
                currentSnapshotIndex = 0;
            }
            showHeatmap(currentSnapshotIndex);
        });

    }

    private void showHeatmap(int position) {
        if (snapshotList.isEmpty()){
            return;
        }

        if (position < 0 || position >= snapshotList.size()) {
            return;
        }

        MonitorResponse snapshot = snapshotList.get(position);

        tvSnapshotTitle.setText("Snapshot " + snapshot.getSnapshotIndex());

        Glide.with(this)
                .load(snapshot.getImageUrl())
                .into(ivHeatmap);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        barChart = null;
        tvSubjectGroup = null;
        tvSessionTime = null;
        tvTotalStudents = null;
        tvDominant = null;
        containerStudents = null;
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

    private void loadSnapshots() {
        Context context = getContext();
        if (context == null) return;

        HistoryDetailsService service = SupabaseClient.getInstance(context).create(HistoryDetailsService.class);

        service.getMonitor("eq." + sessionId, "eq." + SessionManager.getUserId(context), "snapshot_index.asc").enqueue(new Callback<List<MonitorResponse>>() {

            @Override
            public void onResponse(@NonNull Call<List<MonitorResponse>> call, @NonNull Response<List<MonitorResponse>> response) {

                if (!isAdded() || getView() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    snapshotList.clear();
                    snapshotList.addAll(response.body());
                    drawChart();
                    updateCards();

                    currentSnapshotIndex = 0;

                    if(!snapshotList.isEmpty()){
                        showHeatmap(0);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<MonitorResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "Snapshot error", t);
            }
        });
    }

    private void drawChart() {
        if (!isAdded()) return;

        if (barChart == null) return;

        if (snapshotList.isEmpty()) {
            barChart.clear();
            return;
        }

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

        if (maxTotal == 0) {
            barChart.clear();
            return;
        }

        if (maxTotal < 6) maxTotal = 6;

        BarDataSet ds = new BarDataSet(entries, "");
        ds.setColors(
                Color.parseColor("#DC2626"),
                Color.parseColor("#D97706"),
                Color.parseColor("#059669")
        );
        ds.setStackLabels(new String[]{"Low", "Med", "High"});

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
        if (!isAdded()) return;

        if (snapshotList.isEmpty()) {
            tvTotalStudents.setText("-");
            tvDominant.setText("-");
            tvDominant.setTextColor(Color.GRAY);
            return;
        }

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
    }

    private void loadStudentEvolution() {
        Context context = getContext();
        if (context == null) return;

        HistoryDetailsService service = SupabaseClient.getInstance(context)
                .create(HistoryDetailsService.class);

        service.getStudentEvolution("*", "eq." + sessionId, "student_id.asc,snapshot_index.asc").enqueue(new Callback<List<StudentResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<StudentResponse>> call, @NonNull Response<List<StudentResponse>> response) {
                if (!isAdded() || getView() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    renderStudents(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<StudentResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "Student error", t);
            }
        });
    }

    private void renderStudents(List<StudentResponse> list) {
        Context context = getContext();
        if (context == null) return;

        containerStudents.removeAllViews();

        Map<String, List<StudentResponse>> map = new LinkedHashMap<>();

        for (StudentResponse s : list) {
            map.computeIfAbsent(s.getStudentId(), k -> new ArrayList<>()).add(s);
        }

        for (List<StudentResponse> snapshots : map.values()) {

            if (snapshots.isEmpty()) continue;

            View item = LayoutInflater.from(context).inflate(R.layout.item_history_details, containerStudents, false);

            TextView tvId = item.findViewById(R.id.tvStudentId);
            LinearLayout flow = item.findViewById(R.id.containerAttention);

            tvId.setText("Student " + snapshots.get(0).getLocalId());

            for (int i = 0; i < snapshots.size(); i++) {

                TextView badge = new TextView(context);
                badge.setPadding(20, 10, 20, 10);

                String attention = snapshots.get(i).getAttention();
                badge.setText(attention != null ? attention.toUpperCase() : "-");
                badge.setTextColor(Color.WHITE);

                if ("low".equals(attention)) {
                    badge.setBackgroundColor(Color.parseColor("#DC2626"));
                } else if ("medium".equals(attention)) {
                    badge.setBackgroundColor(Color.parseColor("#D97706"));
                } else if ("high".equals(attention)) {
                    badge.setBackgroundColor(Color.parseColor("#059669"));
                }

                flow.addView(badge);

                if (i < snapshots.size() - 1) {
                    TextView arrow = new TextView(context);
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
}