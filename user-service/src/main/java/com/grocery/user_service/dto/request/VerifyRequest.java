package com.grocery.user_service.dto.request;

import lombok.Data;

@Data
public class VerifyRequest {
    private String phoneOrEmail;
    private String otp;
}
