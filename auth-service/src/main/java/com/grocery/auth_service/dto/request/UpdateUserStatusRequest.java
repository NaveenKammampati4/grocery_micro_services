package com.grocery.auth_service.dto.request;

import com.grocery.auth_service.entity.User;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserStatusRequest {

    private User.UserStatus status;

    @NotNull(message = "Enabled flag is required")
    private boolean enabled;
}
