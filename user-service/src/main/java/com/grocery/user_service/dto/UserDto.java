package com.grocery.user_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserDto {
    private Long id;
    private String name, email, phone, avatarUrl;
    private boolean phoneVerified, emailVerified;
    private List<AddressDto> addresses;
//    private String role;
//    private boolean enabled;
}
