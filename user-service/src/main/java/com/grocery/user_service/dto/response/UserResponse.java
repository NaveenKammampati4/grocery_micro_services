package com.grocery.user_service.dto.response;

import com.grocery.user_service.entity.UserProfile;
import lombok.Data;

import java.util.List;

@Data
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private UserProfile.Role role;
    private List<AddressResponse> addresses;
}
