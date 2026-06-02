package com.student.engagement.system.adapters;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.student.engagement.system.R;

public class ScheduledSessionHolder extends RecyclerView.ViewHolder {

    protected TextView tvCourseTitle, tvSessionDateTime, tvSessionClassroom, tvSessionType;

    protected ImageButton btnEditSession, btnDeleteSession;

    public ScheduledSessionHolder(@NonNull View itemView) {
        super(itemView);

        tvCourseTitle = itemView.findViewById(R.id.tvCourseTitle);
        tvSessionDateTime = itemView.findViewById(R.id.tvSessionDateTime);
        tvSessionClassroom = itemView.findViewById(R.id.tvSessionClassroom);
        tvSessionType = itemView.findViewById(R.id.tvSessionType);
        btnEditSession = itemView.findViewById(R.id.btnEditSession);
        btnDeleteSession = itemView.findViewById(R.id.btnDeleteSession);
    }
}
