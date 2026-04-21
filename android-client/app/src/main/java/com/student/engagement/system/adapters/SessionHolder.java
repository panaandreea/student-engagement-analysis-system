package com.student.engagement.system.adapters;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.student.engagement.system.R;

public class SessionHolder extends RecyclerView.ViewHolder {
    protected TextView sessionTitle;
    protected TextView sessionHighBadge;
    protected MaterialCardView sessionCardBadge;
    protected TextView sessionTime;
    protected TextView sessionLow;
    protected TextView sessionMedium;
    protected TextView sessionHigh;
    protected TextView sessionStudents;
    protected ImageView sessionArrow;

    public SessionHolder(@NonNull View itemView) {
        super(itemView);
        sessionTitle = itemView.findViewById(R.id.tvTitle);
        sessionHighBadge = itemView.findViewById(R.id.tvHighBadge);
        sessionCardBadge = itemView.findViewById(R.id.cardBadge);
        sessionTime = itemView.findViewById(R.id.tvTime);
        sessionLow = itemView.findViewById(R.id.tvLow);
        sessionMedium = itemView.findViewById(R.id.tvMedium);
        sessionHigh = itemView.findViewById(R.id.tvHigh);
        sessionStudents = itemView.findViewById(R.id.tvStudents);
        sessionArrow = itemView.findViewById(R.id.ivArrow);
    }
}
