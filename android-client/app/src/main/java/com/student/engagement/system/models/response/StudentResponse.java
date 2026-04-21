package com.student.engagement.system.models.response;

import com.google.gson.annotations.SerializedName;

public class StudentResponse {
    @SerializedName("student_id")
    private String studentId;
    @SerializedName("local_id")
    private String localId;
    @SerializedName("session_id")
    private String sessionId;
    @SerializedName("snapshot_index")
    private int snapshotIndex;
    @SerializedName("attention")
    private String attention;

    public String getStudentId() {
        return studentId;
    }

    public String getLocalId() {
        return localId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public int getSnapshotIndex() {
        return snapshotIndex;
    }

    public String getAttention() {
        return attention;
    }
}
