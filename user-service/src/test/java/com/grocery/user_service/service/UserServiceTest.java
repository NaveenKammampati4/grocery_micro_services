package com.grocery.user_service.service;

import com.grocery.user_service.dto.AddressDto;
import com.grocery.user_service.dto.UserDto;
import com.grocery.user_service.dto.request.UserUpdateRequest;
import com.grocery.user_service.entity.Address;
import com.grocery.user_service.entity.UserProfile;
import com.grocery.user_service.exception.UserNotFoundException;
import com.grocery.user_service.mapper.UserMapper;
import com.grocery.user_service.repository.AddressRepository;
import com.grocery.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private UserProfile userProfile;
    private UserDto userDto;

    private static final String EMAIL="test@example.com";

    @BeforeEach
    void setUp(){
        userProfile=new UserProfile();
        userProfile.setUserId(1L);
        userProfile.setEmail(EMAIL);
        userProfile.setDeleted(false);
        userProfile.setAddresses(new ArrayList<>());
        userDto=new UserDto();
        userDto.setUserId(1L);
        userDto.setEmail(EMAIL);
    }

    @Test
    @DisplayName("should return user profile when valid user Id provided")
    void shouldReturnUserProfile(){
        when(userRepository.findById(1L)).thenReturn(Optional.of(userProfile));
        when(userMapper.toDto(userProfile)).thenReturn(userDto);
        UserDto result = userService.getProfile(1L);

        assertNotNull(result);
        assertEquals(EMAIL,result.getEmail());
        verify(userRepository).findById(1L);
        verify(userMapper).toDto(userProfile);
    }

    @Test
    @DisplayName("should throw UserNotFoundException when user profile not found")
    void shouldThrowExceptionWhenUserNotFound(){
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, ()-> userService.getProfile(1L));
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("should update user profile successfully")
    void shouldUpdateUserProfile(){
        UserUpdateRequest request=new UserUpdateRequest();
        request.setEmail("john@example.com");
        request.setName("Naveen");
        when(userRepository.findById(1L)).thenReturn(Optional.of(userProfile));
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.save(userProfile)).thenReturn(userProfile);
        when(userMapper.toDto(userProfile)).thenReturn(userDto);

        UserDto result = userService.updateProfile(1L, request);
        assertNotNull(result);
        verify(userMapper).updateEntity(request,userProfile);
        verify(userRepository).save(userProfile);
    }

    @Test
    @DisplayName("should throw exception when updating with duplicate email")
    void shouldThrowExceptionForDuplicateEmail(){
        UserUpdateRequest request=new UserUpdateRequest();
        request.setEmail("duplicate@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(userProfile));
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);
        assertThrows(IllegalArgumentException.class, ()->userService.updateProfile(1L,request));
    }

    @Test
    @DisplayName("should add address for user")
    void shouldAddAddress(){
        AddressDto dto=new AddressDto();
        dto.setCity("City");
        dto.setStreet("Street 1");
        Address address=new Address();
        when(userRepository.findById(1L)).thenReturn(Optional.of(userProfile));
        when(userMapper.toEntity(dto)).thenReturn(address);
        when(addressRepository.save(address)).thenReturn(address);
        when(userMapper.toDto(address)).thenReturn(dto);
        AddressDto result = userService.addAddress(1L, dto);
        assertNotNull(result);
        verify(addressRepository).save(address);
        verify(userMapper).toDto(address);
    }

    @Test
    @DisplayName("should delete address when address belongs to user")
    void shouldDeleteAddress(){
        Address address=new Address();
        address.setId(10L);
        address.setUserProfile(userProfile);
        address.setDefault(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(userProfile));
        when(addressRepository.findById(10L)).thenReturn(Optional.of(address));
        userService.deleteAddress(1L,10L);
    }

    @Test
    @DisplayName("should throw exception when deleting address that does not belong to user")
    void shouldThrowExceptionWhenDeletingForeignAddress(){
        UserProfile anotherUser=new UserProfile();
        anotherUser.setUserId(2L);
        Address address=new Address();
        address.setId(10L);
        address.setUserProfile(anotherUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(userProfile));
        when(addressRepository.findById(10L)).thenReturn(Optional.of(address));
        assertThrows(IllegalArgumentException.class, ()->userService.deleteAddress(1L,10L));
    }

    @Test
    @DisplayName("should mark user email is verified")
    void shouldVerifyEmail(){
        when(userRepository.findById(1L)).thenReturn(Optional.of(userProfile));
        when(userRepository.save(userProfile)).thenReturn(userProfile);
        when(userMapper.toDto(userProfile)).thenReturn(userDto);

        UserDto result = userService.verifyEmail(1L);
        assertTrue(userProfile.isEmailVerified());
        assertNotNull(result);
        verify(userRepository).save(userProfile);
    }

}
