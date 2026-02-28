package com.grocery.auth_service.service;

import com.grocery.auth_service.client.UserServiceClient;
import com.grocery.auth_service.dto.request.InitProfileRequest;
import com.grocery.auth_service.dto.request.RegisterRequest;
import com.grocery.auth_service.entity.User;
import com.grocery.auth_service.exception.authenticationException.PasswordMismatchException;
import com.grocery.auth_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("AuthService Unit Tests")
@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRequest;

    @BeforeEach
    void setUp(){
        validRequest=RegisterRequest.builder()
                .name("Naveen")
                .email("naveen@example.com")
                .password("Pass1234")
                .build();
    }

    @Test
    @DisplayName("register: should create user with encoded password and default role")
    void register_shouldCreateUserWithEncodedPasswordAndDefaultRole(){
        String encodedPassword="encoded";
        User newUser=User.builder()
                .name("Naveen")
                .email("naveen@example.com".toLowerCase().trim())
                .password(encodedPassword)
                .role(User.Role.CUSTOMER)
                .status(User.UserStatus.ACTIVE)
                .enabled(true)
                .build();

        //when
        when(userRepository.existsByEmail(newUser.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        User result = authService.register(validRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Naveen");
        assertThat(result.getEmail()).isEqualTo("naveen@example.com");
        assertThat(result.getRole()).isEqualTo(User.Role.CUSTOMER);
        assertThat(result.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
        assertThat(result.isEnabled()).isTrue();
        verify(userRepository).existsByEmail(newUser.getEmail());
        verify(passwordEncoder).encode(validRequest.getPassword());
        verify(userRepository).save(any(User.class));
        verify(userServiceClient).initProfile(eq(newUser.getId()),any(InitProfileRequest.class));
    }

    @Test
    @DisplayName("register: should throw exception when email already exists")
    void register_shouldThrowExceptionWhenEmailAlreadyExists(){
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);
        assertThatThrownBy(()->authService.register(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email already exists");
    }

    @Test
    @DisplayName("register: should throw exception when password does not meet criteria")
    void register_shouldThrowExceptionWhenPasswordInvalid(){
        validRequest.setPassword("week");
        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(PasswordMismatchException.class)
                .hasMessageContaining("Password must be at least 8 characters");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("authenticate: should succeed with valid credentials")
    void authenticate_shouldAuthenticateSuccessfully(){
        String rawPassword= "Pass1234";
        String encodedPassword="encoded";
        User user=User.builder()
                .id(1L)
                .name("Naveen")
                .email("naveen@example.com")
                .password(encodedPassword)
                .role(User.Role.CUSTOMER)
                .status(User.UserStatus.ACTIVE)
                .enabled(true)
                .build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

}
