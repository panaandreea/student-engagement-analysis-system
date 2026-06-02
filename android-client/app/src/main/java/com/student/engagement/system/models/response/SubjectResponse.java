package com.student.engagement.system.models.response;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class SubjectResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    public String getId() {
        return id;
    }

    @NonNull
    @Override
    public String toString() {
       return name != null ? name : "";
    }
}