package com.student.engagement.system.adapters;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.student.engagement.system.R;
import com.student.engagement.system.fragments.HistoryDetailsFragment;
import com.student.engagement.system.models.response.HistoryResponse;
import com.student.engagement.system.utils.DateUtils;

import java.util.ArrayList;

public class HistorySessionAdapter extends RecyclerView.Adapter<HistorySessionHolder> {

    private static final String TAG = "SESSION_ADAPTER";

    private final ArrayList<HistoryResponse> statsList;

    private final Fragment parentFragment;

    public HistorySessionAdapter(ArrayList<HistoryResponse> statsList, Fragment parentFragment) {
        this.statsList = statsList;
        this.parentFragment = parentFragment;
    }

    @NonNull
    @Override
    public HistorySessionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistorySessionHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistorySessionHolder holder, int position) {
        HistoryResponse item = statsList.get(position);

        holder.tvCourseTitle.setText(item.getSubjectName() + " - " + item.getGroupCode());

        String date = DateUtils.formatIsoToDate(item.getStartTime());

        String start = DateUtils.formatIsoToTime(item.getStartTime());
        String end = DateUtils.formatIsoToTime(item.getEndTime());

        holder.tvSessionDateTime.setText(
                date + " • " + start + " - " + end
        );

        holder.tvLowAttention.setText(String.valueOf(item.getLow()));
        holder.tvMediumAttention.setText(String.valueOf(item.getMedium()));
        holder.tvHighAttention.setText(String.valueOf(item.getHigh()));

        holder.tvStudentCount.setText(item.getTotalStudents() + " Students");

        int low = item.getLow();
        int med = item.getMedium();
        int high = item.getHigh();

        Log.d(TAG, "Bind item pos=" + position + " low=" + low + " med=" + med + " high=" + high);

        if (item.getTotalStudents() == 0) {
            updateBadge(holder, "NO DATA", "#F3F4F6", "#6B7280");
        } else if (high >= med && high >= low) {
            updateBadge(holder, "HIGH", "#ECFDF5", "#059669");
        } else if (med >= low) {
            updateBadge(holder, "MEDIUM", "#FFFBEB", "#D97706");
        } else {
            updateBadge(holder, "LOW", "#FEF2F2", "#DC2626");
        }

        holder.ivArrow.setOnClickListener(v -> {
            Log.d(TAG, "Arrow clicked for session: " + item.getSessionId());

            Bundle bundle = new Bundle();
            bundle.putString("session_id", item.getSessionId());
            bundle.putString("subject", item.getSubjectName());
            bundle.putString("group", item.getGroupCode());
            bundle.putString("start_time", item.getStartTime());
            bundle.putString("end_time", item.getEndTime());

            HistoryDetailsFragment fragment = new HistoryDetailsFragment();
            fragment.setArguments(bundle);

            parentFragment.requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void updateBadge(HistorySessionHolder holder, String text, String bgColor, String textColor) {
        holder.tvHighBadge.setText(text);
        holder.cardBadge.setCardBackgroundColor(Color.parseColor(bgColor));
        holder.tvHighBadge.setTextColor(Color.parseColor(textColor));
    }


    @Override
    public int getItemCount() {
        return statsList != null ? statsList.size() : 0;
    }

    public void updateData(ArrayList<HistoryResponse> newData) {
        statsList.clear();
        statsList.addAll(newData);
        notifyDataSetChanged();
    }
}