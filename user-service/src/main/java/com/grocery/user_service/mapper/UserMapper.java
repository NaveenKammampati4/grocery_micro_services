package com.grocery.user_service.mapper;

import com.grocery.user_service.dto.AddressDto;
import com.grocery.user_service.dto.UserDto;
import com.grocery.user_service.dto.request.UserUpdateRequest;
import com.grocery.user_service.entity.Address;
import com.grocery.user_service.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);
    User toEntity(UserDto dto);

    AddressDto toDto(Address address);
    Address toEntity(AddressDto dto);

    default void updateEntity(UserUpdateRequest req, @MappingTarget User user) {
        user.setName(req.getName());
        user.setPhone(req.getPhone());
        user.setAvatarUrl(req.getAvatarUrl());
    }
}
