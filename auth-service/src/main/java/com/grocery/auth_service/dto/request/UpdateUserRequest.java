package com.grocery.auth_service.dto.request;

import com.grocery.auth_service.entity.User;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String name;
    private User.Role role;
    private User.UserStatus status;
    private Boolean enabled;
}
