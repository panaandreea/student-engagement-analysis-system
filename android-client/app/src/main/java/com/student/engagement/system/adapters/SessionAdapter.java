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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class SessionAdapter extends RecyclerView.Adapter<SessionHolder> {
    private static final String TAG = "SESSION_ADAPTER";
    private final ArrayList<HistoryResponse> statsList;
    private final Fragment parentFragment;

    public SessionAdapter(ArrayList<HistoryResponse> statsList, Fragment parentFragment) {
        this.statsList = statsList;
        this.parentFragment = parentFragment;
    }

    @NonNull
    @Override
    public SessionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.session_item, parent, false);
        return new SessionHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionHolder holder, int position) {
        HistoryResponse item = statsList.get(position);

        holder.sessionTitle.setText(item.getSubjectName() + " - " + item.getGroupCode());
        holder.sessionTime.setText(formatTime(item.getStartTime()));
        holder.sessionLow.setText(String.valueOf(item.getLow()));
        holder.sessionMedium.setText(String.valueOf(item.getMedium()));
        holder.sessionHigh.setText(String.valueOf(item.getHigh()));
        holder.sessionStudents.setText(item.getTotalStudents() + " Students");

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

        holder.sessionArrow.setOnClickListener(v -> {
            Log.d(TAG, "Arrow clicked for session: " + item.getSessionId());

            Bundle bundle = new Bundle();
            bundle.putString("session_id", item.getSessionId());
            bundle.putString("subject", item.getSubjectName());
            bundle.putString("group", item.getGroupCode());
            bundle.putString("start_time", item.getStartTime());

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

    private void updateBadge(SessionHolder holder, String text, String bgColor, String textColor) {
        holder.sessionHighBadge.setText(text);
        holder.sessionCardBadge.setCardBackgroundColor(Color.parseColor(bgColor));
        holder.sessionHighBadge.setTextColor(Color.parseColor(textColor));
    }

    private String formatTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "";

        try {
            String cleanDate = dateStr
                    .replace("T", " ")
                    .split("\\.")[0]
                    .split("\\+")[0]
                    .trim();

            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));

            Date date = parser.parse(cleanDate);
            if (date == null) return dateStr;

            SimpleDateFormat formatter = new SimpleDateFormat("dd MMM • HH:mm", Locale.getDefault());
            formatter.setTimeZone(TimeZone.getDefault());

            return formatter.format(date);

        } catch (Exception e) {
            Log.e(TAG, "Date parse error: " + dateStr, e);
            return dateStr;
        }
    }

    @Override
    public int getItemCount() {
        return statsList != null ? statsList.size() : 0;
    }
}