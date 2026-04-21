package com.student.engagement.system.models.response;

import com.google.gson.annotations.SerializedName;

public class HistoryResponse {
    @SerializedName("session_id")
    private String sessionId;
    @SerializedName("subject_name")
    private String subjectName;
    @SerializedName("teacher_id")
    private String teacherId;
    @SerializedName("group_code")
    private String groupCode;
    @SerializedName("start_time")
    private String startTime;
    @SerializedName("low")
    private int low;
    @SerializedName("medium")
    private int medium;
    @SerializedName("high")
    private int high;
    @SerializedName("total_students")
    private int totalStudents;

    public String getSessionId() {
        return sessionId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public String getStartTime() {
        return startTime;
    }

    public int getLow() {
        return low;
    }

    public int getMedium() {
        return medium;
    }

    public int getHigh() {
        return high;
    }

    public int getTotalStudents() {
        return totalStudents;
    }
}