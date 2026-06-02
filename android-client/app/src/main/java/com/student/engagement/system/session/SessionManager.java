package com.student.engagement.system.session;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;


public class SessionManager {

    private static final String PREF_NAME = "app_session";

    private static final String KEY_TOKEN = "token";

    private static final String KEY_REFRESH_TOKEN = "refresh_token";

    private static final String KEY_USER_ID = "user_id";

    private static final String KEY_FIRST_NAME = "first_name";

    private static final String KEY_LAST_NAME = "last_name";
    private static final String KEY_EMAIL = "email";

    private static SharedPreferences sharedPreferences;

    private static synchronized SharedPreferences getSharedPreferences(Context context) {
        if (sharedPreferences != null) return sharedPreferences;

        try {
            MasterKey masterKey = new MasterKey.Builder(context.getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            sharedPreferences = EncryptedSharedPreferences.create(
                    context.getApplicationContext(),
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

        } catch (Exception e) {
            context.deleteSharedPreferences(PREF_NAME);

            sharedPreferences = context.getSharedPreferences(
                    PREF_NAME,
                    Context.MODE_PRIVATE
            );
        }

        return sharedPreferences;
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
        String token = getSharedPreferences(context).getString(KEY_TOKEN, "");
        return token != null ? token : "";
    }

    public static String getRefreshToken(Context context){
        String token = getSharedPreferences(context).getString(KEY_REFRESH_TOKEN, "");
        return token != null ? token : "";
    }

    public static String getUserId(Context context){
        return getSharedPreferences(context).getString(KEY_USER_ID, "");
    }

    public static boolean isLoggedIn(Context context){
        return !getToken(context).isEmpty() && !getUserId(context).isEmpty();
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

    public static void clearSession(Context context) {
        try {
            if (sharedPreferences != null) {
                sharedPreferences.edit().clear().apply();
            }
        } catch (Exception e) {
            try {
                context.deleteSharedPreferences(PREF_NAME);
            } catch (Exception ignored) {}
        }
        sharedPreferences = null;
    }
}