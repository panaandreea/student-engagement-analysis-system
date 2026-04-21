package com.student.engagement.system.services;

import com.student.engagement.system.models.requests.LoginRequest;
import com.student.engagement.system.models.requests.RegisterRequest;
import com.student.engagement.system.models.response.AuthResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;

public interface AuthService {
    @POST("auth/v1/signup")
    Call<AuthResponse> register(@Body RegisterRequest registerRequest);
    @POST("auth/v1/token?grant_type=password")
    Call<AuthResponse> login(@Body LoginRequest loginRequest);
    @PUT("auth/v1/user")
    Call<Void> updatePassword(@Body Map<String, String> body);
}