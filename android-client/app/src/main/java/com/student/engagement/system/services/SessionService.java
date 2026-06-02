package com.student.engagement.system.services;

import com.student.engagement.system.models.requests.SessionRequest;
import com.student.engagement.system.models.response.SessionResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SessionService {

    @POST("rest/v1/sessions")
    Call<Void> createSession(
            @Body SessionRequest sessionRequest
    );

    @GET("rest/v1/active_classroom_sessions")
    Call<List<SessionResponse>> checkClassroomAvailability(
            @Query("classroom") String classroom
    );

}