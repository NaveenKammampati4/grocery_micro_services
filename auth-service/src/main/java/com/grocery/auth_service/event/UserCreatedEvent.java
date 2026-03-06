package com.grocery.auth_service.event;

import lombok.Data;

@Data
public class UserCreatedEvent {
    private Long userId;
    private String email;
}
