package com.student.engagement.system.services;

import com.student.engagement.system.models.response.HistoryResponse;
import com.student.engagement.system.models.response.MonitorResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface HistoryService {
    @GET("rest/v1/history_statistics")
    Call<List<HistoryResponse>> getStats(
            @Query("teacher_id") String teacherId,
            @Query("select") String select,
            @Query("order") String order
    );
}