package com.student.engagement.system.models.response;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("refresh_token")
    private String refreshToken;

    @SerializedName("expires_in")
    private int expiresIn;

    @SerializedName("token_type")
    private String tokenType;

    @SerializedName("user")
    private UserData user;

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public UserData getUser() {
        return user;
    }

    public static class UserData {

        @SerializedName("id")
        private String id;

        @SerializedName("email")
        private String email;

        @SerializedName("user_metadata")
        private UserMetadata userMetadata;

        public String getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }

        public UserMetadata getUserMetadata() {
            return userMetadata;
        }

        public static class UserMetadata {

            @SerializedName("first_name")
            private String firstName;

            @SerializedName("last_name")
            private String lastName;

            public String getFirstName() {
                return firstName;
            }

            public String getLastName() {
                return lastName;
            }
        }
    }
}