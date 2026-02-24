package com.grocery.auth_service.dto.request;

import com.grocery.auth_service.entity.User;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BulkStatusUpdateRequest {

    @NotEmpty(message = "User IDs list cannot be empty")
    private List<@NotNull(message = "User ID cannot be null")Long> userIds;

    @NotNull(message = "Status is required")
    private User.UserStatus status;

    @NotNull(message = "Enabled flag is required")
    private Boolean enabled;
}
