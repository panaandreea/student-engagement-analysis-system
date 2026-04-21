package com.student.engagement.system.services;

import com.student.engagement.system.models.response.MonitorResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MonitorService {
    @GET("rest/v1/session_snapshots_analysis")
    Call<List<MonitorResponse>> getMonitor(
            @Query("session_id") String sessionId,
            @Query("teacher_id") String teacherId,
            @Query("order") String order
    );
}
