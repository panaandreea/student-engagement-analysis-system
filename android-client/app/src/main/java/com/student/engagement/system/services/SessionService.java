package com.student.engagement.system.services;

import com.student.engagement.system.models.requests.SessionRequest;
import com.student.engagement.system.models.requests.TeacherSubjectRequest;
import com.student.engagement.system.models.response.SessionResponse;
import com.student.engagement.system.models.response.TeacherSubjectResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SessionService {
    @Headers({"Prefer: resolution=merge-duplicates"})
    @POST("rest/v1/teacher_subjects?on_conflict=teacher_id,subject_id")
    Call<Void> assignSubjectToTeacher(
            @Body TeacherSubjectRequest request
    );
    @GET("rest/v1/teacher_subjects")
    Call<List<TeacherSubjectResponse>> getTeacherSubject(
            @Query("teacher_id") String teacherId,
            @Query("subject_id") String subjectId,
            @Query("select") String select
    );
    @POST("rest/v1/sessions")
    Call<Void> createSession(
            @Body SessionRequest sessionRequest
    );
    @GET("rest/v1/sessions")
    Call<List<SessionResponse>> getActiveSessionsMulti(
            @Query("teacher_subject_id") String teacherSubjectIds, // ex: in.(id1,id2)
            @Query("start_time") String start,
            @Query("end_time") String end,
            @Query("limit") int limit
    );
}