package com.insightwrite.dto;

public class AuthResponse {

    private String token;
    private UserInfo user;

    public AuthResponse(String token, UserInfo user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() { return token; }
    public UserInfo getUser() { return user; }

    // 内嵌用户信息，对应 API 合同 { token, user: { id, username, email, credits } }
    public static class UserInfo {
        private Integer id;
        private String username;
        private String email;
        private Integer credits;
        private String avatar;

        public UserInfo(Integer id, String username, String email, Integer credits, String avatar) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.credits = credits;
            this.avatar = avatar;
        }

        public Integer getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public Integer getCredits() { return credits; }
        public String getAvatar() { return avatar; }
    }
}
