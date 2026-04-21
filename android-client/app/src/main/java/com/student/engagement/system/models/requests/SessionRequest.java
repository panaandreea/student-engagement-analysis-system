package com.student.engagement.system.models.requests;

import com.google.gson.annotations.SerializedName;

public class SessionRequest {
    @SerializedName("teacher_subject_id")
    private final String teacherSubjectId;
    @SerializedName("group_code")
    private final String groupCode;
    @SerializedName("session_type")
    private final String sessionType;
    @SerializedName("start_time")
    private final String startTime;
    @SerializedName("end_time")
    private final String endTime;
    @SerializedName("session_status")
    private final String status;
    @SerializedName("planned_observations")
    private final int totalObservationsPlanned;

    public SessionRequest(String teacherSubjectId, String groupCode, String sessionType, String startTime, String endTime, int totalObservationsPlanned) {
        this.teacherSubjectId = teacherSubjectId;
        this.groupCode = groupCode;
        this.sessionType = sessionType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = "scheduled";
        this.totalObservationsPlanned = totalObservationsPlanned;
    }
}