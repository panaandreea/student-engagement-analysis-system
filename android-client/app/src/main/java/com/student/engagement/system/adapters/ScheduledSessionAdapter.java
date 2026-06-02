package com.student.engagement.system.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.student.engagement.system.R;
import com.student.engagement.system.models.response.SessionResponse;
import com.student.engagement.system.utils.DateUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScheduledSessionAdapter extends RecyclerView.Adapter<ScheduledSessionHolder> {

    private List<SessionResponse> sessions;

    public interface OnSessionActionListener {
        void onEdit(SessionResponse session);
        void onDelete(SessionResponse session);
    }

    private final OnSessionActionListener listener;

    public ScheduledSessionAdapter(OnSessionActionListener listener, List<SessionResponse> sessions) {
        this.listener = listener;
        this.sessions = sessions;
    }

    @NonNull
    @Override
    public ScheduledSessionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scheduled_session, parent,false);

        return new ScheduledSessionHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduledSessionHolder holder, int position) {
        SessionResponse sessionResponse = sessions.get(position);


        String title = (sessionResponse.getSubjectName() != null ? sessionResponse.getSubjectName() : "Unknown")
                + " - " + sessionResponse.getGroupCode();
        holder.tvCourseTitle.setText(title);

        String date = DateUtils.formatIsoToDateTime(sessionResponse.getStartTime());
        String endTime = DateUtils.formatIsoToTime(sessionResponse.getEndTime());

        holder.tvSessionDateTime.setText(date + " - " + endTime);

        holder.tvSessionClassroom.setText(sessionResponse.getClassroom());

        holder.tvSessionType.setText(sessionResponse.getSessionType());

        holder.btnEditSession.setOnClickListener(v->listener.onEdit(sessionResponse));
        holder.btnDeleteSession.setOnClickListener(v->listener.onDelete(sessionResponse));
    }


    @Override
    public int getItemCount() {
        return sessions != null ? sessions.size() : 0;
    }

    public void updateData(List<SessionResponse> newSessions) {
        if (newSessions == null) {
            this.sessions = new ArrayList<>();
        } else {

            newSessions.sort((s1, s2) -> {
                try {
                    return OffsetDateTime.parse(s2.getStartTime())
                            .compareTo(OffsetDateTime.parse(s1.getStartTime()));
                } catch (Exception e) {
                    return 0;
                }
            });

            this.sessions = newSessions;
        }
        notifyDataSetChanged();
    }
}
