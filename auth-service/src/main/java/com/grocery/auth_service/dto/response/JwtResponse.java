package com.grocery.auth_service.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class JwtResponse {

    private String accessToken;
    private String refreshToken;
    private String type="Bearer";
    private String email;
    private List<String> roles;

    public JwtResponse(String email, List<String> roles) {
        this.email = email;
        this.roles = roles;
    }

    public JwtResponse(String accessToken, String refreshToken, String email, List<String> roles) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.email = email;
        this.roles = roles;
    }

}
