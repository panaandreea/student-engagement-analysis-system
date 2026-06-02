package com.student.engagement.system.adapters;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.student.engagement.system.R;

public class HistorySessionHolder extends RecyclerView.ViewHolder {

    protected TextView tvCourseTitle;

    protected TextView tvHighBadge;

    protected MaterialCardView cardBadge;

    protected TextView tvSessionDateTime;

    protected TextView tvLowAttention;

    protected TextView tvMediumAttention;

    protected TextView tvHighAttention;

    protected TextView tvStudentCount;

    protected ImageView ivArrow;

    public HistorySessionHolder(@NonNull View itemView) {
        super(itemView);

        tvCourseTitle = itemView.findViewById(R.id.tvCourseTitle);
        tvHighBadge = itemView.findViewById(R.id.tvHighBadge);
        cardBadge = itemView.findViewById(R.id.cardBadge);
        tvSessionDateTime = itemView.findViewById(R.id.tvSessionDateTime);
        tvLowAttention = itemView.findViewById(R.id.tvLowAttention);
        tvMediumAttention = itemView.findViewById(R.id.tvMediumAttention);
        tvHighAttention = itemView.findViewById(R.id.tvHighAttention);
        tvStudentCount = itemView.findViewById(R.id.tvStudentCount);
        ivArrow = itemView.findViewById(R.id.ivArrow);
    }
}
