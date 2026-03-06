package com.grocery.auth_service.service;

import com.grocery.auth_service.dto.request.RegisterRequest;
import com.grocery.auth_service.entity.User;
import com.grocery.auth_service.exception.authenticationException.DuplicateEmailException;
import com.grocery.auth_service.exception.authenticationException.UserNotFoundException;
import com.grocery.auth_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserService adminUserService;

    private RegisterRequest request;
    private User user;

    private static final String NAME="Naveen";
    private static final String ADMIN_EMAIL="admin@test.com";
    private static final String PASS="password";

    @BeforeEach
    void setUp(){
        request=new RegisterRequest();
        request.setName(NAME);
        request.setEmail(ADMIN_EMAIL);
        request.setPassword(PASS);
        user=User.builder()
                .id(1L)
                .name(NAME)
                .email(ADMIN_EMAIL)
                .role(User.Role.ADMIN)
                .enabled(true)
                .status(User.UserStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("should create admin successfully when email does not exist")
    void createAdmin_shouldCreateAdminSuccessfully(){
        when(userRepository.existsByEmail(ADMIN_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(PASS)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = adminUserService.createAdmin(request);
        assertNotNull(result);
        assertEquals(User.Role.ADMIN,result.getRole());

        verify(userRepository).existsByEmail(ADMIN_EMAIL);
        verify(passwordEncoder).encode(PASS);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("should create delivery partner successfully when email does not exist")
    void createDeliveryPartner_shouldCreateDeliveryPartner(){
        when(userRepository.existsByEmail(ADMIN_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(PASS)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = adminUserService.createDeliveryPartner(request);
        assertNotNull(result);
        assertEquals(User.Role.DELIVERY_PARTNER, result.getRole());
        assertEquals("encodedPassword", result.getPassword());
        assertTrue(result.isEnabled());
        assertEquals(User.UserStatus.ACTIVE, result.getStatus());

        verify(userRepository).existsByEmail(ADMIN_EMAIL);
        verify(passwordEncoder).encode(PASS);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("should throw DuplicateEmailException when email already exists")
    void createAdmin_shouldThrowException_whenEmailExist(){
        when(userRepository.existsByEmail(ADMIN_EMAIL)).thenReturn(true);
        assertThrows(DuplicateEmailException.class, ()->adminUserService.createAdmin(request));
        verify(userRepository).existsByEmail(ADMIN_EMAIL);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw DuplicateEmailException when email already exists")
    void createDeliveryPartner_shouldThrowException_whenEmailExist(){
        when(userRepository.existsByEmail(ADMIN_EMAIL)).thenReturn(true);
        assertThrows(DuplicateEmailException.class, ()->adminUserService.createDeliveryPartner(request));
        verify(userRepository).existsByEmail(ADMIN_EMAIL);
        verify(userRepository, never()).save(any());
    }


    @Test
    @DisplayName("show return user when user Id exists")
    void findByUserId_shouldReturnUser_whenExists(){
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        User result = adminUserService.findByUserId(1L);
        assertEquals(1L,result.getId());
    }

    @Test
    @DisplayName("should throw UserNotFoundException when user Id does not exist")
    void findByUserId_shouldThrowException_whenNotFound(){
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, ()->adminUserService.findByUserId(1L));
    }

    @Test
    @DisplayName("should return security information for existing user")
    void getUserSecurityInfo_shouldReturnSecurityInfo(){
        user.setFailedLoginAttempts(2);
        user.setLockTime(null);
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(user));
        Map<String, Object> result = adminUserService.getUserSecurityInfo(ADMIN_EMAIL);
        assertEquals(ADMIN_EMAIL, result.get("email"));
        assertEquals(User.Role.ADMIN, result.get("role"));
        assertEquals(true, result.get("enabled"));
    }

    @Test
    @DisplayName("should throw UsernameNotFoundException when email does not exist")
    void getUserSecurityInfo_shouldThrowException_whenEmailNotFound(){
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class,()->adminUserService.getUserSecurityInfo(ADMIN_EMAIL));
        verify(userRepository).findByEmail(ADMIN_EMAIL);
    }

    @Test
    @DisplayName("should return true when lock time in future")
    void isAccountLocked_shouldReturnTrue_whenLockTimeInFuture(){
        user.setLockTime(LocalDateTime.now().plusMinutes(30));
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(user));
        boolean result = adminUserService.isAccountLocked(ADMIN_EMAIL);
        assertTrue(result);
    }

    @Test
    @DisplayName("should update user status and enabled flag successfully")
    void updateUserStatus_shouldUpdateUser(){
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        User result = adminUserService.updateUserStatus(1L, User.UserStatus.SUSPENDED, false);
        assertEquals(User.UserStatus.SUSPENDED,result.getStatus());
        assertFalse(result.isEnabled());
        verify(userRepository).save(user);
    }

}
