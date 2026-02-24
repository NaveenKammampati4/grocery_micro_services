package com.grocery.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressRequest {

    @NotBlank
    private String street;

    private String city;
    private String state;
    private String zipCode;
    private boolean isDefault;
}
