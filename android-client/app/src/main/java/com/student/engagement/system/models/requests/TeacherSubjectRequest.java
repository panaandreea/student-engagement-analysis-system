package com.student.engagement.system.models.requests;

import com.google.gson.annotations.SerializedName;

public class TeacherSubjectRequest {
    @SerializedName("teacher_id")
    private final String teacherId;
    @SerializedName("subject_id")
    private final String subjectId;

    public TeacherSubjectRequest(String teacherId, String subjectId) {
        this.teacherId = teacherId;
        this.subjectId = subjectId;
    }
}