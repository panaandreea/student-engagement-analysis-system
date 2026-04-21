package com.student.engagement.system.models.response;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class SubjectResponse {
    @SerializedName("id")
    private String id;
    @SerializedName("name")
    private String name;

    public SubjectResponse() {
    }

    public SubjectResponse(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @NonNull
    @Override
    public String toString() {
       return name;
    }
}