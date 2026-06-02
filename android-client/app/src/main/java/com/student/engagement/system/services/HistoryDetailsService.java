package com.student.engagement.system.services;

import com.student.engagement.system.models.response.MonitorResponse;
import com.student.engagement.system.models.response.StudentResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface HistoryDetailsService {

    @GET("rest/v1/student_evolution")
    Call<List<StudentResponse>> getStudentEvolution(
            @Query("select") String select,
            @Query("session_id") String sessionId,
            @Query("order") String order
    );

    @GET("rest/v1/session_snapshots_analysis")
    Call<List<MonitorResponse>> getMonitor(
            @Query("session_id") String sessionId,
            @Query("teacher_id") String teacherId,
            @Query("order") String order
    );

}
