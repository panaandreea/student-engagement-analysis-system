package com.student.engagement.system.services;

import com.student.engagement.system.models.response.MonitorResponse;
import com.student.engagement.system.models.response.SessionResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MonitorService {

    @GET("rest/v1/active_sessions")
    Call<List<SessionResponse>> getActiveSession(
            @Query("teacher_id") String teacherId
    );

    @GET("rest/v1/active_session_snapshots")
    Call<List<MonitorResponse>> getActiveMonitor(
            @Query("teacher_id") String teacherId,
            @Query("order") String order
    );
}
