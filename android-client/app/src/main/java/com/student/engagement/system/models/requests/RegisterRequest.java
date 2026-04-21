package com.student.engagement.system.models.requests;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    @SerializedName("email")
    private final String email;
    @SerializedName("password")
    private final String password;
    @SerializedName("data")
    private final UserData data;

    public RegisterRequest(String firstName, String lastName, String email, String password) {
        this.email = email;
        this.password = password;
        this.data = new UserData(firstName, lastName);
    }

    public UserData getData() {
        return data;
    }
    public static class UserData {
        @SerializedName("first_name")
        private final String firstName;
        @SerializedName("last_name")
        private final String lastName;

        public UserData(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }
}