package com.student.engagement.system.models.response;

import com.google.gson.annotations.SerializedName;

public class SessionResponse {
    @SerializedName("id")
    private String id;
    @SerializedName("group_code")
    private String groupCode;
    @SerializedName("start_time")
    private String startTime;
    @SerializedName("end_time")
    private String endTime;


    public String getId() {
        return id;
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
}
