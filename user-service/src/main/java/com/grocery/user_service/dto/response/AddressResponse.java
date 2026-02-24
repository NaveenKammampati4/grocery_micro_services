package com.grocery.user_service.dto.response;

import lombok.Data;

@Data
public class AddressResponse {

    private Long id;
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private boolean isDefault;
}
