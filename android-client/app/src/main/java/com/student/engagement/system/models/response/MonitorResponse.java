package com.student.engagement.system.models.response;

import com.google.gson.annotations.SerializedName;

public class MonitorResponse {
    @SerializedName("low")
    private int low;
    @SerializedName("medium")
    private int medium;
    @SerializedName("high")
    private int high;
    @SerializedName("snapshot_index")
    private int snapshotIndex;
    @SerializedName("captured_at")
    private String capturedAt;

    public int getLow() {
        return low;
    }

    public int getMedium() {
        return medium;
    }

    public int getHigh() {
        return high;
    }

    public int getSnapshotIndex() {
        return snapshotIndex;
    }

    public String getCapturedAt() {
        return capturedAt;
    }
}
