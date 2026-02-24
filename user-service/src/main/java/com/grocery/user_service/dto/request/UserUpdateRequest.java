package com.grocery.user_service.dto.request;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String name;
    private String phone;
    private String email;
    private Boolean phoneVerified;
    private Boolean emailVerified;
    private String avatarUrl;
}
