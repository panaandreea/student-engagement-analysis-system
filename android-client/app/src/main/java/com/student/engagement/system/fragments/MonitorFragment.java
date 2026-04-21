package com.student.engagement.system.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.student.engagement.system.R;
import com.student.engagement.system.models.response.MonitorResponse;
import com.student.engagement.system.models.response.SessionResponse;
import com.student.engagement.system.models.response.TeacherSubjectResponse;
import com.student.engagement.system.networking.SupabaseClient;
import com.student.engagement.system.services.MonitorService;
import com.student.engagement.system.services.SessionService;
import com.student.engagement.system.session.SessionManager;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MonitorFragment extends Fragment {
    private static final String TAG = "MONITOR";
    private static final String COLOR_LOW = "#DC2626";
    private static final String COLOR_MEDIUM = "#D97706";
    private static final String COLOR_HIGH = "#059669";
    private BarChart barChart;
    private TextView tvSubjectGroup;
    private TextView tvSessionTime;
    private TextView tvTotalStudents;
    private TextView tvDominant;
    private TextView tvCurrLow;
    private TextView tvCurrMedium;
    private TextView tvCurrHigh;
    private ImageButton btnPrev;
    private ImageButton btnNext;
    private List<MonitorResponse> snapshotList = new ArrayList<>();
    private int currentIndex = 0;
    private String activeSessionId;
    private String teacherId;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int INTERVAL = 10000;

    public MonitorFragment() {
        super(R.layout.fragment_monitor);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "=== MONITOR START ===");

        mapUI(view);
        setupChart();
        setupButtons();
        loadActiveSession();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume -> start polling");
        handler.post(refreshRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause -> stop polling");
        handler.removeCallbacks(refreshRunnable);
    }

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Polling...");
            loadActiveSession();
            handler.postDelayed(this, INTERVAL);
        }
    };

    private void mapUI(View view) {
        barChart = view.findViewById(R.id.barChart);
        tvSubjectGroup = view.findViewById(R.id.tvSubjectGroup);
        tvSessionTime = view.findViewById(R.id.tvSessionTime);
        tvTotalStudents = view.findViewById(R.id.tvTotalStudents);
        tvDominant = view.findViewById(R.id.tvDominant);

        tvCurrLow = view.findViewById(R.id.tvCurrLow);
        tvCurrMedium = view.findViewById(R.id.tvCurrMedium);
        tvCurrHigh = view.findViewById(R.id.tvCurrHigh);

        btnPrev = view.findViewById(R.id.btnPrevComparison);
        btnNext = view.findViewById(R.id.btnNextComparison);
    }

    private void setupButtons() {
        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> navigateSnapshots(1));
        }
        if (btnNext != null) {
            btnNext.setOnClickListener(v -> navigateSnapshots(-1));
        }
    }

    private void setupChart() {
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setScaleEnabled(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"Low", "Med", "High"}));

        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setAxisMaximum(100f);
    }

    private void loadActiveSession() {
        teacherId = SessionManager.getUserId(requireContext());
        if (teacherId == null) {
            Log.e(TAG, "Teacher ID is null");
            return;
        }

        SessionService sessionService = SupabaseClient.getInstance(requireContext())
                .create(SessionService.class);

        sessionService.getTeacherSubject("eq." + teacherId, null, "id")
                .enqueue(new Callback<List<TeacherSubjectResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<TeacherSubjectResponse>> call,
                                           @NonNull Response<List<TeacherSubjectResponse>> response) {

                        if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                            Log.e(TAG, "No teacher subjects found");
                            return;
                        }

                        List<TeacherSubjectResponse> list = response.body();

                        StringBuilder ids = new StringBuilder("in.(");
                        for (int i = 0; i < list.size(); i++) {
                            ids.append(list.get(i).getId());
                            if (i < list.size() - 1) {
                                ids.append(",");
                            }
                        }
                        ids.append(")");

                        String now = OffsetDateTime.now(ZoneOffset.UTC).toString();

                        Log.d(TAG, "NOW UTC = " + now);
                        Log.d(TAG, "Teacher subject filter = " + ids);

                        sessionService.getActiveSessionsMulti(
                                ids.toString(),
                                "lte." + now,
                                "gte." + now,
                                1
                        ).enqueue(new Callback<List<SessionResponse>>() {
                            @Override
                            public void onResponse(@NonNull Call<List<SessionResponse>> call,
                                                   @NonNull Response<List<SessionResponse>> response) {

                                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                                    Log.e(TAG, "NO ACTIVE SESSION");
                                    return;
                                }

                                SessionResponse session = response.body().get(0);
                                activeSessionId = session.getId();

                                Log.d(TAG, "ACTIVE SESSION = " + activeSessionId);
                                Log.d(TAG, "GROUP = " + session.getGroupCode());
                                Log.d(TAG, "START = " + session.getStartTime());
                                Log.d(TAG, "END = " + session.getEndTime());

                                tvSubjectGroup.setText(session.getGroupCode());
                                tvSessionTime.setText(formatRomanianTime(session.getStartTime(), session.getEndTime()));

                                loadSnapshots();
                            }

                            @Override
                            public void onFailure(@NonNull Call<List<SessionResponse>> call,
                                                  @NonNull Throwable t) {
                                Log.e(TAG, "Session error", t);
                            }
                        });
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<TeacherSubjectResponse>> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "TeacherSubject error", t);
                    }
                });
    }

    private void loadSnapshots() {
        if (activeSessionId == null || teacherId == null) {
            Log.e(TAG, "Cannot load snapshots: activeSessionId or teacherId is null");
            return;
        }

        MonitorService service = SupabaseClient.getInstance(requireContext())
                .create(MonitorService.class);

        service.getMonitor("eq." + activeSessionId, "eq." + teacherId, "snapshot_index.desc")
                .enqueue(new Callback<List<MonitorResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<MonitorResponse>> call,
                                           @NonNull Response<List<MonitorResponse>> response) {

                        if (!response.isSuccessful() || response.body() == null) {
                            Log.e(TAG, "Snapshot response invalid: " + response.code());
                            return;
                        }

                        snapshotList = response.body();
                        Log.d(TAG, "Snapshots size = " + snapshotList.size());

                        if (snapshotList.isEmpty()) {
                            Log.d(TAG, "Session active but no data yet");
                            return;
                        }

                        currentIndex = 0;
                        updateUI();
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<MonitorResponse>> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "Snapshot error", t);
                    }
                });
    }

    private void navigateSnapshots(int step) {
        if (snapshotList.isEmpty()) {
            Log.d(TAG, "Cannot navigate, snapshot list is empty");
            return;
        }

        int nextIndex = currentIndex + step;

        if (nextIndex >= 0 && nextIndex < snapshotList.size()) {
            currentIndex = nextIndex;
            Log.d(TAG, "Navigated to snapshot list index = " + currentIndex);
            updateUI();
        }
    }

    private void updateUI() {
        if (snapshotList.isEmpty()) {
            Log.d(TAG, "updateUI skipped, no snapshots");
            return;
        }

        MonitorResponse curr = snapshotList.get(currentIndex);
        int total = curr.getLow() + curr.getMedium() + curr.getHigh();

        Log.d(TAG,
                "Update UI -> snapIndex="
                        + curr.getSnapshotIndex()
                        + ", low=" + curr.getLow()
                        + ", medium=" + curr.getMedium()
                        + ", high=" + curr.getHigh()
                        + ", total=" + total
        );

        tvTotalStudents.setText(String.valueOf(total));
        tvCurrLow.setText(String.valueOf(curr.getLow()));
        tvCurrMedium.setText(String.valueOf(curr.getMedium()));
        tvCurrHigh.setText(String.valueOf(curr.getHigh()));

        updateDominant(curr);
        updateChart(curr, total);
    }

    private void updateDominant(@NonNull MonitorResponse m) {
        if (m.getHigh() >= m.getMedium() && m.getHigh() >= m.getLow()) {
            tvDominant.setText("HIGH");
            tvDominant.setTextColor(Color.parseColor(COLOR_HIGH));
            Log.d(TAG, "Dominant = HIGH");
        } else if (m.getMedium() >= m.getLow()) {
            tvDominant.setText("MEDIUM");
            tvDominant.setTextColor(Color.parseColor(COLOR_MEDIUM));
            Log.d(TAG, "Dominant = MEDIUM");
        } else {
            tvDominant.setText("LOW");
            tvDominant.setTextColor(Color.parseColor(COLOR_LOW));
            Log.d(TAG, "Dominant = LOW");
        }
    }

    private void updateChart(@NonNull MonitorResponse m, int total) {
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, percent(m.getLow(), total)));
        entries.add(new BarEntry(1, percent(m.getMedium(), total)));
        entries.add(new BarEntry(2, percent(m.getHigh(), total)));

        BarDataSet set = new BarDataSet(entries, "");
        set.setColors(
                Color.parseColor(COLOR_LOW),
                Color.parseColor(COLOR_MEDIUM),
                Color.parseColor(COLOR_HIGH)
        );

        BarData data = new BarData(set);
        data.setBarWidth(0.5f);

        barChart.setData(data);
        barChart.getAxisLeft().setAxisMaximum(100f);
        barChart.invalidate();
    }

    private float percent(int value, int total) {
        return total == 0 ? 0f : (value * 100f / total);
    }

    private String formatRomanianTime(String start, String end) {
        try {
            OffsetDateTime s = OffsetDateTime.parse(start);
            OffsetDateTime e = OffsetDateTime.parse(end);

            ZoneId ro = ZoneId.of("Europe/Bucharest");

            DateTimeFormatter leftFormatter = DateTimeFormatter.ofPattern("dd MMM • HH:mm");
            DateTimeFormatter rightFormatter = DateTimeFormatter.ofPattern("HH:mm");

            String startFormatted = s.atZoneSameInstant(ro).format(leftFormatter);
            String endFormatted = e.atZoneSameInstant(ro).format(rightFormatter);

            return startFormatted + " - " + endFormatted;

        } catch (Exception ex) {
            Log.e(TAG, "Time parse error", ex);
            return "-";
        }
    }
}