package com.student.engagement.system.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.student.engagement.system.R;
import com.student.engagement.system.models.response.MonitorResponse;
import com.student.engagement.system.networking.SupabaseClient;
import com.student.engagement.system.services.MonitorService;
import com.student.engagement.system.session.SessionManager;
import com.student.engagement.system.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MonitorFragment extends Fragment {

    private static final String TAG = "MONITOR";

    private static final String COLOR_LOW = "#DC2626";

    private static final String COLOR_MED = "#D97706";

    private static final String COLOR_HIGH = "#059669";

    private BarChart barChart;

    private TableLayout tableComparison;

    private LinearLayout layoutHeatmap;

    private TextView tvSnapshotTitle;

    private ImageView ivHeatmap;

    private MaterialButtonToggleGroup toggleGroupView;

    private TextView tvSubjectGroup, tvSessionTime;

    private TextView tvTotalStudents, tvDominant;

    private TextView tvPrevLow, tvPrevMedium, tvPrevHigh;

    private TextView tvCurrLow, tvCurrMedium, tvCurrHigh;

    private TextView tvDeltaLow, tvDeltaMed, tvDeltaHigh;

    private TextView tvLabelPrev, tvLabelCurr;

    private ImageButton btnPrev, btnNext;

    private List<MonitorResponse> snapshotList = new ArrayList<>();

    private int currentIndex = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final int INTERVAL = 5000;


    public MonitorFragment() {
        super(R.layout.fragment_monitor);
    }

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadSnapshots();
            handler.postDelayed(this, INTERVAL);
        }
    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view);
        setupChart();
        setupButtons();
        loadSnapshots();
        setupToggle();
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.post(refreshRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }

    private void init(View v) {
        barChart = v.findViewById(R.id.barChart);

        tableComparison = v.findViewById(R.id.tableComparison);
        layoutHeatmap = v.findViewById(R.id.layoutHeatmap);

        tvSnapshotTitle = v.findViewById(R.id.tvSnapshotTitle);
        ivHeatmap = v.findViewById(R.id.ivHeatmap);

        toggleGroupView = v.findViewById(R.id.toggleGroupView);

        tvSubjectGroup = v.findViewById(R.id.tvSubjectGroup);
        tvSessionTime = v.findViewById(R.id.tvSessionTime);
        tvTotalStudents = v.findViewById(R.id.tvTotalStudents);
        tvDominant = v.findViewById(R.id.tvDominant);

        tvPrevLow = v.findViewById(R.id.tvPrevLow);
        tvPrevMedium = v.findViewById(R.id.tvPrevMedium);
        tvPrevHigh = v.findViewById(R.id.tvPrevHigh);

        tvCurrLow = v.findViewById(R.id.tvCurrLow);
        tvCurrMedium = v.findViewById(R.id.tvCurrMedium);
        tvCurrHigh = v.findViewById(R.id.tvCurrHigh);

        tvDeltaLow = v.findViewById(R.id.tvDeltaLow);
        tvDeltaMed = v.findViewById(R.id.tvDeltaMed);
        tvDeltaHigh = v.findViewById(R.id.tvDeltaHigh);

        tvLabelPrev = v.findViewById(R.id.tvLabelPrev);
        tvLabelCurr = v.findViewById(R.id.tvLabelCurr);

        btnPrev = v.findViewById(R.id.btnPrevComparison);
        btnNext = v.findViewById(R.id.btnNextComparison);

    }

    private void setupButtons() {
        btnPrev.setOnClickListener(v -> {
            if (currentIndex + 1 < snapshotList.size()) {
                currentIndex++;
                updateUI();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                updateUI();
            }
        });
    }

    private void setupChart() {
        if (barChart == null) return;

        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"Low", "Med", "High"}));
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setAxisMaximum(100f);
    }

    private void loadSnapshots() {
        Context context = getContext();
        if (context == null) return;

        String teacherId = SessionManager.getUserId(context);

        if (teacherId == null || teacherId.isEmpty()) return;

        MonitorService service = SupabaseClient.getInstance(context)
                .create(MonitorService.class);

        service.getActiveMonitor("eq." + teacherId, "snapshot_index.desc")
                .enqueue(new Callback<List<MonitorResponse>>() {

                    @Override
                    public void onResponse(@NonNull Call<List<MonitorResponse>> call,
                                           @NonNull Response<List<MonitorResponse>> response) {

                        if (!isAdded() || getView() == null) return;

                        if (!response.isSuccessful() || response.body() == null) return;

                        List<MonitorResponse> newData = response.body();

                        if (newData.isEmpty()) {
                            showNoDataState();
                            return;
                        }

                        snapshotList = newData;

                        if (currentIndex >= snapshotList.size()) {
                            currentIndex = 0;
                        }

                        updateUI();
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<MonitorResponse>> call,
                                          @NonNull Throwable t) {
                        if (!isAdded() || getView() == null) return;
                        Log.e(TAG, "API error", t);
                    }
                });
    }


    private void setupToggle(){
        toggleGroupView.addOnButtonCheckedListener(
                (group, checkedId, isChecked) ->{
                    if(!isChecked) return;

                    if(checkedId == R.id.btnViewComparison) {
                        tableComparison.setVisibility(View.VISIBLE);
                        layoutHeatmap.setVisibility(View.GONE);
                    } else if (checkedId == R.id.btnViewHeatmap){
                        tableComparison.setVisibility(View.GONE);
                        layoutHeatmap.setVisibility(View.VISIBLE);

                        updateHeatmap();
                    }
                }
        );
    }

    private void updateHeatmap() {
        if(snapshotList.isEmpty()) return;

        MonitorResponse snapshot = snapshotList.get(currentIndex);

        tvSnapshotTitle.setText("Snapshot " + snapshot.getSnapshotIndex());

        Glide.with(requireContext())
                .load(snapshot.getImageUrl())
                .into(ivHeatmap);
    }

    private void updateUI() {
        if (!isAdded() || getView() == null) return;

        if (snapshotList == null || snapshotList.isEmpty()) return;

        MonitorResponse curr = snapshotList.get(currentIndex);

        MonitorResponse prev = (currentIndex + 1 < snapshotList.size())
                ? snapshotList.get(currentIndex + 1)
                : null;

        tvLabelCurr.setText("S" + curr.getSnapshotIndex());
        tvLabelPrev.setText(prev != null ? "S" + prev.getSnapshotIndex() : "-");

        tvSubjectGroup.setText(
                (curr.getSubjectName() != null ? curr.getSubjectName() : "-") +
                        " - " +
                        (curr.getGroupCode() != null ? curr.getGroupCode() : "-")
        );

        tvSessionTime.setText(
                curr.getStartTime() != null && curr.getEndTime() != null
                        ? DateUtils.formatIsoToDateTime(curr.getStartTime()) +
                        " - " +
                        DateUtils.formatIsoToTime(curr.getEndTime())
                        : "-"
        );

        int total = curr.getLow() + curr.getMedium() + curr.getHigh();

        tvTotalStudents.setText(String.valueOf(total));
        updateDominant(curr);

        tvCurrLow.setText(String.valueOf(curr.getLow()));
        tvCurrMedium.setText(String.valueOf(curr.getMedium()));
        tvCurrHigh.setText(String.valueOf(curr.getHigh()));

        if (prev != null) {
            tvPrevLow.setText(String.valueOf(prev.getLow()));
            tvPrevMedium.setText(String.valueOf(prev.getMedium()));
            tvPrevHigh.setText(String.valueOf(prev.getHigh()));

            setDelta(tvDeltaLow, prev.getLow(), curr.getLow());
            setDelta(tvDeltaMed, prev.getMedium(), curr.getMedium());
            setDelta(tvDeltaHigh, prev.getHigh(), curr.getHigh());
        } else {
            tvPrevLow.setText("-");
            tvPrevMedium.setText("-");
            tvPrevHigh.setText("-");
            tvDeltaLow.setText("-");
            tvDeltaMed.setText("-");
            tvDeltaHigh.setText("-");
        }

        updateChart(curr, total);

        if(layoutHeatmap.getVisibility() == View.VISIBLE){
            updateHeatmap();
        }
    }

    private void updateChart(MonitorResponse m, int total) {
        if (barChart == null) return;

        barChart.clear();

        if (total == 0) {
            barChart.clear();
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, percent(m.getLow(), total)));
        entries.add(new BarEntry(1, percent(m.getMedium(), total)));
        entries.add(new BarEntry(2, percent(m.getHigh(), total)));

        BarDataSet set = new BarDataSet(entries, "");
        set.setColors(
                Color.parseColor(COLOR_LOW),
                Color.parseColor(COLOR_MED),
                Color.parseColor(COLOR_HIGH)
        );

        BarData data = new BarData(set);
        barChart.setData(data);
        barChart.invalidate();
    }

    private void updateDominant(MonitorResponse m) {
        int total = m.getLow() + m.getMedium() + m.getHigh();

        if (total == 0) {
            tvDominant.setText("-");
            tvDominant.setTextColor(Color.GRAY);
            return;
        }

        if (m.getHigh() >= m.getMedium() && m.getHigh() >= m.getLow()) {
            tvDominant.setText("HIGH");
            tvDominant.setTextColor(Color.parseColor(COLOR_HIGH));
        } else if (m.getMedium() >= m.getLow()) {
            tvDominant.setText("MED");
            tvDominant.setTextColor(Color.parseColor(COLOR_MED));
        } else {
            tvDominant.setText("LOW");
            tvDominant.setTextColor(Color.parseColor(COLOR_LOW));
        }
    }

    private void showNoDataState() {
        snapshotList.clear();
        currentIndex = 0;

        tvLabelPrev.setText("-");
        tvLabelCurr.setText("CURR");

        tvPrevLow.setText("-");
        tvPrevMedium.setText("-");
        tvPrevHigh.setText("-");

        tvCurrLow.setText("0");
        tvCurrMedium.setText("0");
        tvCurrHigh.setText("0");

        tvDeltaLow.setText("-");
        tvDeltaMed.setText("-");
        tvDeltaHigh.setText("-");

        if (barChart != null) {
            barChart.clear();
        }
    }

    private void setDelta(TextView tv, int prev, int curr) {
        int diff = curr - prev;

        if (diff == 0) {
            tv.setText("0");
            return;
        }

        tv.setText((diff > 0 ? "+" : "") + diff);
    }

    private float percent(int v, int total) {
        return total == 0 ? 0 : (v * 100f / total);
    }
}