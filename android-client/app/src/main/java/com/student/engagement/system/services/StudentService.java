package com.student.engagement.system.services;

import com.student.engagement.system.models.response.StudentResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface StudentService {
    @GET("rest/v1/student_evolution")
    Call<List<StudentResponse>> getStudentEvolution(
            @Query("select") String select,
            @Query("session_id") String sessionId,
            @Query("order") String order
    );
}