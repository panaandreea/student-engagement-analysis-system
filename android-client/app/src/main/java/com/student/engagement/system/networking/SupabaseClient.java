package com.student.engagement.system.networking;

import android.content.Context;
import android.util.Log;

import com.student.engagement.system.BuildConfig;
import com.student.engagement.system.session.SessionManager;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.Authenticator;
import okhttp3.logging.HttpLoggingInterceptor;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseClient {
    private static final String TAG = "SupabaseClient";
    private static Retrofit instance = null;

    public static synchronized Retrofit getInstance(Context context) {

        Context appContext = context.getApplicationContext();

        if (instance == null) {

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS);

            if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);
                builder.addInterceptor(logging);
            }

            builder.addInterceptor(chain -> {

                String token = SessionManager.getToken(appContext);

                Request.Builder requestBuilder = chain.request().newBuilder()
                        .header("apikey", BuildConfig.SUPABASE_KEY);

                if (token != null && !token.isEmpty()) {
                    requestBuilder.header("Authorization", "Bearer " + token);
                }

                return chain.proceed(requestBuilder.build());
            });

            builder.authenticator(new Authenticator() {
                @Override
                public Request authenticate(Route route, Response response) {

                    if (responseCount(response) >= 2) {
                        Log.e(TAG, "Auth loop detected. Aborting.");
                        return null;
                    }

                    String refreshToken = SessionManager.getRefreshToken(appContext);

                    if (refreshToken == null || refreshToken.isEmpty()) {
                        Log.e(TAG, "No refresh token available.");
                        return null;
                    }

                    Log.d(TAG, "Access token expired. Attempting refresh...");

                    try {

                        String baseUrl = BuildConfig.SUPABASE_URL;
                        String refreshUrl = (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/")
                                + "auth/v1/token?grant_type=refresh_token";

                        Request refreshRequest = new Request.Builder()
                                .url(refreshUrl)
                                .post(okhttp3.RequestBody.create(
                                        okhttp3.MediaType.get("application/json"),
                                        "{\"refresh_token\":\"" + refreshToken + "\"}"
                                ))
                                .header("apikey", BuildConfig.SUPABASE_KEY)
                                .header("Content-Type", "application/json")
                                .build();

                        OkHttpClient refreshClient = new OkHttpClient();

                        try (Response refreshResponse =
                                     refreshClient.newCall(refreshRequest).execute()) {

                            if (!refreshResponse.isSuccessful() || refreshResponse.body() == null) {
                                Log.e(TAG, "Token refresh failed. Clearing session.");
                                SessionManager.clearSession(appContext);
                                SupabaseClient.reset();
                                return null;
                            }

                            String body = refreshResponse.body().string();
                            JSONObject json = new JSONObject(body);

                            String newAccessToken = json.getString("access_token");
                            String newRefreshToken = json.getString("refresh_token");

                            SessionManager.updateTokens(appContext, newAccessToken, newRefreshToken);

                            Log.d(TAG, "Token refreshed successfully.");

                            return response.request().newBuilder()
                                    .header("Authorization", "Bearer " + newAccessToken)
                                    .build();
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Critical error in authenticator: " + e.getMessage());
                        return null;
                    }
                }
            });

            instance = new Retrofit.Builder()
                    .baseUrl(BuildConfig.SUPABASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(builder.build())
                    .build();
        }

        return instance;
    }

    private static int responseCount(Response response) {
        int count = 1;
        while ((response = response.priorResponse()) != null) {
            count++;
        }
        return count;
    }

    public static void reset() {
        instance = null;
    }
}