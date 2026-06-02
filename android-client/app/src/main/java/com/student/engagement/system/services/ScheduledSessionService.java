package com.student.engagement.system.services;

import com.student.engagement.system.models.requests.SessionRequest;
import com.student.engagement.system.models.response.SessionResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Query;

public interface ScheduledSessionService {

    @PATCH("rest/v1/sessions")
    Call<Void> updateSession(
            @Query("id") String sessionId,
            @Body SessionRequest request
    );

    @DELETE("rest/v1/sessions")
    Call<Void> deleteSession(
            @Query("id") String sessionId
    );

    @GET("rest/v1/sessions_with_subject?select=*")
    Call<List<SessionResponse>> getScheduledSessions(
            @Query("start_time") String startTime,
            @Query("teacher_id") String teacherId
    );
}
