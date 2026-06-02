package com.student.engagement.system.services;

import com.student.engagement.system.models.response.SubjectResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface SubjectService {

    @GET("rest/v1/subjects?select=*&order=name.asc")
    Call<List<SubjectResponse>> getSubjects();
}