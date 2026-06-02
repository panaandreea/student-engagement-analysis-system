package com.student.engagement.system.models.requests;

import com.google.gson.annotations.SerializedName;

public class SessionRequest {

    @SerializedName("teacher_subject_id")
    private final String teacherSubjectId;

    @SerializedName("group_code")
    private final String groupCode;

    @SerializedName("start_time")
    private final String startTime;

    @SerializedName("end_time")
    private final String endTime;

    @SerializedName("classroom")
    private final String classroom;

    @SerializedName("session_type")
    private final String sessionType;

    public SessionRequest(String teacherSubjectId, String groupCode, String startTime, String endTime, String classroom, String sessionType) {
        this.teacherSubjectId = teacherSubjectId;
        this.groupCode = groupCode;
        this.startTime = startTime;
        this.endTime = endTime;
        this.classroom = classroom;
        this.sessionType = sessionType;
    }

    public String getTeacherSubjectId() {
        return teacherSubjectId;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getClassroom() {
        return classroom;
    }

    public String getSessionType() {
        return sessionType;
    }
}