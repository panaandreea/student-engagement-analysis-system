package com.student.engagement.system.models.response;

import com.google.gson.annotations.SerializedName;

public class MonitorResponse {

    @SerializedName("captured_at")
    private String capturedAt;

    @SerializedName("subject_name")
    private String subjectName;

    @SerializedName("group_code")
    private String groupCode;

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("end_time")
    private String endTime;

    @SerializedName("low")
    private Integer low;

    @SerializedName("medium")
    private Integer medium;

    @SerializedName("high")
    private Integer high;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("snapshot_index")
    private Integer snapshotIndex;

    public int getLow() {
        return low != null ? low : 0;
    }

    public int getMedium() {
        return medium != null ? medium : 0;
    }

    public int getHigh() {
        return high != null ? high : 0;
    }

    public int getSnapshotIndex() {
        return snapshotIndex != null ? snapshotIndex : 0;
    }

    public String getCapturedAt() {
        return capturedAt;
    }

    public String getSubjectName() {
        return subjectName;
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

    public String getImageUrl() {
        return imageUrl;
    }
}