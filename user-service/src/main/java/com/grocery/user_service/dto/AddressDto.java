package com.grocery.user_service.dto;

import lombok.Data;

@Data
public class AddressDto {
    private Long id;
    private String street, city, state, zipCode, label;
    private boolean isDefault;
}
