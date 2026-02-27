package com.grocery.auth_service.event;

import lombok.Data;

@Data
public class UserCreatedEvent {
    private Long userId;
    private String email;

    public UserCreatedEvent(Long userId, String email) {
        this.userId = userId;
        this.email = email;
    }
}
