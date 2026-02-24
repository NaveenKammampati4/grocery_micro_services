package com.grocery.auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefreshRequest {

    @NotBlank(message = "Refresh token is required")
    @Size(max = 500, message = "Refresh token is too long")
    private String refreshToken;
}
