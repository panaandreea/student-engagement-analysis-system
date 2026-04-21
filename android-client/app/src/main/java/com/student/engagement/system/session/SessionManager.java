package com.student.engagement.system.session;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "app_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_FIRST_NAME = "firstName";
    private static final String KEY_LAST_NAME = "lastName";
    private static final String KEY_EMAIL = "email";

    private static SharedPreferences getSharedPreferences(Context context){
        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void saveSession(Context context, String token, String refreshToken, String userId, String firstName, String lastName, String email){
        getSharedPreferences(context)
                .edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_TOKEN, token)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putString(KEY_FIRST_NAME, firstName)
                .putString(KEY_LAST_NAME, lastName)
                .putString(KEY_EMAIL, email)
                .apply();
    }

    public static void updateTokens(Context context, String newToken, String newRefreshToken){
        getSharedPreferences(context)
                .edit()
                .putString(KEY_TOKEN, newToken)
                .putString(KEY_REFRESH_TOKEN, newRefreshToken)
                .apply();
    }

    public static String getToken(Context context){
        return getSharedPreferences(context).getString(KEY_TOKEN, null);
    }

    public static String getRefreshToken(Context context){
        return getSharedPreferences(context).getString(KEY_REFRESH_TOKEN, null);
    }

    public static String getUserId(Context context){
        return getSharedPreferences(context).getString(KEY_USER_ID, null);
    }

    public static boolean isLoggedIn(Context context){
        String token = getToken(context);
        return token != null && !token.isEmpty();
    }

    public static String getFirstName(Context context){
        return getSharedPreferences(context).getString(KEY_FIRST_NAME, "");
    }

    public static String getLastName(Context context){
        return getSharedPreferences(context).getString(KEY_LAST_NAME, "");
    }

    public static String getEmail(Context context){
        return getSharedPreferences(context).getString(KEY_EMAIL, "");
    }

    public  static void clearSession(Context context){
        getSharedPreferences(context).edit().clear().apply();
    }
}