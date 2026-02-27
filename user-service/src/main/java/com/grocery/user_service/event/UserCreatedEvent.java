package com.grocery.user_service.event;

import lombok.Data;

@Data
public class UserCreatedEvent {
    private Long userId;
    private String email;
    private String role;

    public UserCreatedEvent(Long userId, String email) {
        this.userId = userId;
        this.email = email;
    }
}
