package com.grocery.auth_service.service;

import com.grocery.auth_service.client.UserServiceClient;
import com.grocery.auth_service.dto.request.InitProfileRequest;
import com.grocery.auth_service.dto.request.RegisterRequest;
import com.grocery.auth_service.entity.User;
import com.grocery.auth_service.exception.authenticationException.PasswordMismatchException;
import com.grocery.auth_service.exception.authenticationException.UserNotFoundException;
import com.grocery.auth_service.repository.UserRepository;
import com.grocery.auth_service.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
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


    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRequest;

    private static final String VALID_EMAIL="naveen@example.com";
    private static final String VALID_NAME="Naveen";
    private static final String VALID_PASSWORD="Pass1234";
    private static final String ENCODED_PW="encoded";
    private static final String VALID_ADMIN_EMAIL="admin@example.com";


    @BeforeEach
    void setUp(){
        validRequest=RegisterRequest.builder()
                .name(VALID_NAME)
                .email(VALID_EMAIL)
                .password(VALID_PASSWORD)
                .build();
    }

    @Test
    @DisplayName("register: should create user with encoded password and default role")
    void register_shouldCreateUserWithEncodedPasswordAndDefaultRole(){
        User newUser=User.builder()
                .name(VALID_NAME)
                .email(VALID_EMAIL.toLowerCase().trim())
                .password(ENCODED_PW)
                .role(User.Role.CUSTOMER)
                .status(User.UserStatus.ACTIVE)
                .enabled(true)
                .build();

        //when
        when(userRepository.existsByEmail(newUser.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn(ENCODED_PW);
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        User result = authService.register(validRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(VALID_NAME);
        assertThat(result.getEmail()).isEqualTo(VALID_EMAIL);
        assertThat(result.getRole()).isEqualTo(User.Role.CUSTOMER);
        assertThat(result.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
        assertThat(result.isEnabled()).isTrue();
        verify(userRepository).existsByEmail(newUser.getEmail());
        verify(passwordEncoder).encode(validRequest.getPassword());
        verify(userRepository).save(any(User.class));
        verify(userServiceClient).initProfile(eq(newUser.getId()),any(InitProfileRequest.class));
    }

    @Test
    @DisplayName("register: should throw exception when name is too short")
    void register_shouldThrowExceptionWhenNameTooShort(){
        validRequest.setName("A");
        assertThatThrownBy(()->authService.register(validRequest))
                .isInstanceOf(PasswordMismatchException.class)
                .hasMessage("Name must be at least 2 characters");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: should throw exception when name is null")
    void register_shouldThrowExceptionWhenNameNull() {
        validRequest.setName(null);

        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(PasswordMismatchException.class)
                .hasMessage("Name must be at least 2 characters");

        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    @DisplayName("register: should throw exception when email is null")
    void register_shouldThrowExceptionWhenEmailNull() {
        validRequest.setEmail(null);

        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(PasswordMismatchException.class)
                .hasMessage("Valid email required");

        verify(userRepository, never()).existsByEmail(anyString());
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
        String rawPassword= VALID_PASSWORD;
        User user=User.builder()
                .id(1L)
                .name(VALID_NAME)
                .email(VALID_EMAIL)
                .password(ENCODED_PW)
                .role(User.Role.CUSTOMER)
                .status(User.UserStatus.ACTIVE)
                .enabled(true)
                .build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword,ENCODED_PW)).thenReturn(true);
        Authentication result = authService.authenticate(user.getEmail(), rawPassword);
        assertThat(result).isNotNull();
        assertThat(result.getPrincipal()).isInstanceOf(UserDetailsImpl.class);
        UserDetailsImpl userDetails = (UserDetailsImpl) result.getPrincipal();
        assertThat(userDetails.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("authenticate: should throw UsernameNotFoundException when user not found")
    void authenticate_shouldThrowUsernameNotFoundWhenUserNotFound(){
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(()->authService.authenticate("notfound@example.com",VALID_PASSWORD))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("authenticate: should throw PasswordMismatchException for invalid password")
    void authenticate_shouldThrowPasswordMismatchExceptionForInvalidPassword(){
        User user=User.builder()
                .id(1L)
                .email(VALID_EMAIL)
                .password(ENCODED_PW)
                .build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        assertThatThrownBy(() -> authService.authenticate(user.getEmail(), "wrong"))
                .isInstanceOf(PasswordMismatchException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("findUserByEmail: should return user when exists")
    void findUserByEmail_shouldReturnUserIfExists(){
        User user=User.builder()
                .id(1L)
                .email(VALID_EMAIL)
                .build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        Optional<User> result = authService.findUserByEmail(user.getEmail());
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("incrementFailedLogin: should increment attempts and lock if threshold reached")
    void incrementFailedLogin_shouldIncrementAttemptsAndLockIfThresholdReached(){
        User user=User.builder()
                .id(1L)
                .email(VALID_EMAIL)
                .status(User.UserStatus.ACTIVE)
                .enabled(true)
                .failedLoginAttempts(4)
                .build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        authService.incrementFailedLogin(user.getEmail());
        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getLockTime()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("incrementFailedLogin: should not lock non‑active user")
    void incrementFailedLogin_shouldNotLockNonActiveUser(){
        User user=User.builder()
                .id(1L)
                .email(VALID_EMAIL)
                .status(User.UserStatus.SUSPENDED)
                .enabled(false)
                .failedLoginAttempts(4)
                .build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        authService.incrementFailedLogin(user.getEmail());
        assertThat(user.getFailedLoginAttempts()).isEqualTo(4);
        assertThat(user.getLockTime()).isNull();
    }

    @Test
    @DisplayName("incrementFailedLogin: should skip for ACTIVE but DISABLED user")
    void incrementFailedLogin_shouldSkipForActiveButDisabledUser(){
        User user = User.builder()
                .id(1L)
                .email(VALID_EMAIL)
                .status(User.UserStatus.ACTIVE)  // ACTIVE but !enabled
                .enabled(false)
                .failedLoginAttempts(3)
                .build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        authService.incrementFailedLogin(user.getEmail());

        // Should NOT increment attempts or set lock time (early return hit)
        assertThat(user.getFailedLoginAttempts()).isEqualTo(3);
        assertThat(user.getLockTime()).isNull();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("incrementFailedLogin: should increment but NOT lock at 4 attempts")
    void incrementFailedLogin_shouldIncrementButNotLockAt4Attempts() {
        User user = User.builder()
                .id(1L)
                .email(VALID_EMAIL)
                .status(User.UserStatus.ACTIVE)
                .enabled(true)
                .failedLoginAttempts(3)  // Goes to 4, below threshold
                .build();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        authService.incrementFailedLogin(user.getEmail());

        assertThat(user.getFailedLoginAttempts()).isEqualTo(4);
        assertThat(user.getLockTime()).isNull();  // NOT locked yet
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("incrementUser: should throw UserNotFoundException when user does not exist")
    void incrementFailedLogin_shouldThrowUserNotFound(){
        User user=User.builder()
                .id(1L)
                .email(VALID_EMAIL)
                .build();
        assertThatThrownBy(()->authService.incrementFailedLogin(user.getEmail()))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("deleteUser: should soft‑delete user")
    @Transactional
    void deleteUser_shouldSoftDeleteUser(){
        User user=User.builder()
                .id(1L)
                .email(VALID_EMAIL)
                .status(User.UserStatus.ACTIVE)
                .enabled(true)
                .build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.countByStatusAndRole(User.UserStatus.ACTIVE, User.Role.ADMIN)).thenReturn(2L);
        when(userRepository.isUserAdmin(user.getId())).thenReturn(false);
        authService.deleteUser(user.getId(),VALID_ADMIN_EMAIL);
        assertThat(user.getStatus()).isEqualTo(User.UserStatus.INACTIVE);
        assertThat(user.isEnabled()).isFalse();
        assertThat(user.getDeletedAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("deleteUser: should throw UserNotFoundException when user does not exist")
    void deleteUser_deleteUserNotFound(){
        User user=User.builder()
                .id(1L)
                .email(VALID_EMAIL)
                .status(User.UserStatus.ACTIVE)
                .enabled(true)
                .build();
        assertThatThrownBy(()->authService.deleteUser(user.getId(),"admin@example.com"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found: "+user.getId());
    }



    @Test
    @DisplayName("deleteUser: should throw exception when last admin is deleted")
    void deleteUser_shouldThrowWhenLastAdminIsDeleted(){
        User user=User.builder()
                .id(2L)
                .email(VALID_ADMIN_EMAIL)
                .role(User.Role.ADMIN)
                .status(User.UserStatus.ACTIVE)
                .enabled(true)
                .build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.countByStatusAndRole(User.UserStatus.ACTIVE, User.Role.ADMIN)).thenReturn(1L);
        when(userRepository.isUserAdmin(user.getId())).thenReturn(true);
        assertThatThrownBy(() -> authService.deleteUser(user.getId(), "admin@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cannot delete the last admin user");
        // Verify that the user was NOT modified/saved since delete is blocked
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("deleteUser: should not delete already inactive user")
    void deleteUser_shouldNotDeleteInactiveUser() {
        User inactiveUser = User.builder()
                .id(1L)
                .email(VALID_EMAIL)
                .status(User.UserStatus.INACTIVE)
                .enabled(false)
                .build();

        when(userRepository.findById(inactiveUser.getId())).thenReturn(Optional.of(inactiveUser));

        authService.deleteUser(inactiveUser.getId(), VALID_ADMIN_EMAIL);

        // Should not call validateDeleteOperation, softDelete, or save
        verify(userRepository, times(1)).findById(inactiveUser.getId());
        verify(userRepository, never()).countByStatusAndRole(any(), any());
        verify(userRepository, never()).save(any());
        verify(userRepository, never()).isUserAdmin(anyLong());
    }


    @Test
    @DisplayName("activateUser: should activate user")
    @Transactional
    void activateUser_shouldActivateUser(){
        LocalDateTime now=LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        User user=User.builder()
                .id(1L)
                .status(User.UserStatus.SUSPENDED)
                .enabled(false)
                .deletedAt(now)
                .build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        authService.activateUser(user.getId());
        assertThat(user.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getDeletedAt()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("validatePassword: should pass valid password")
    void validatePassword_shouldPassValidPassword(){
        assertThatCode(()->authService.validatePassword(VALID_PASSWORD))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validatePassword: should throw for too short password")
    void validatePassword_shouldThrowForShortPassword(){
        assertThatThrownBy(()-> authService.validatePassword("short"))
                .isInstanceOf(PasswordMismatchException.class)
                .hasMessageContaining("Password must be at least 8 characters");
    }

    @Test
    @DisplayName("validatePassword: should throw for missing uppercase/lowercase/digit")
    void validatePassword_shouldThrowForMissingComplexity(){
        assertThatThrownBy(() -> authService.validatePassword("Password"))
                .isInstanceOf(PasswordMismatchException.class)
                .hasMessageContaining("Password must contain uppercase, lowercase, and number");
    }

}
