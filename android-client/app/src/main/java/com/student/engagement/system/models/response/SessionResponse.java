package com.student.engagement.system.models.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class SessionResponse implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName("teacher_subject_id")
    private String teacherSubjectId;

    @SerializedName("group_code")
    private String groupCode;

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("end_time")
    private String endTime;

    @SerializedName("session_type")
    private String sessionType;

    @SerializedName("classroom")
    private String classroom;

    @SerializedName("subject_name")
    private String subjectName;

    public String getId() {
        return id;
    }

    public String getTeacherSubjectId() {
        return teacherSubjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public String getClassroom() {
        return classroom;
    }

    public String getSessionType() {
        return sessionType;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getGroupCode() {
        return groupCode;
    }
}

