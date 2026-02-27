package com.grocery.auth_service.dto.request;

import lombok.Data;

@Data
public class InitProfileRequest {
    private String email;
    private String role;
    public InitProfileRequest(String email) {
        this.email = email;
    }
}
