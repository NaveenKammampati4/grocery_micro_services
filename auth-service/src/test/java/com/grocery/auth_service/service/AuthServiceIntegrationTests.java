package com.grocery.auth_service.service;

import com.grocery.auth_service.client.UserServiceClient;
import com.grocery.auth_service.dto.request.RegisterRequest;
import com.grocery.auth_service.entity.User;
import com.grocery.auth_service.repository.UserRepository;
import com.grocery.auth_service.security.UserDetailsImpl;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
@DisplayName("AuthService Integration Tests")
public class AuthServiceIntegrationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    UserServiceClient userServiceClient;


    private static final String EMAIL="naveen@example.com";
    private static final String NAME="Naveen";
    private static final String RAW_PASSWORD = "Pass1234";

    @Test
    @Transactional
    @Order(1)
    @DisplayName("register: should persist user and set encoded password")
    void register_shouldPersistUserAndSetEncodedPassword(){
        RegisterRequest request=RegisterRequest.builder()
                .name(NAME)
                .email(EMAIL)
                .password(RAW_PASSWORD)
                .build();
        User saved = authService.register(request);
        Optional<User> dbUserOpt = userRepository.findByEmail(EMAIL);
        assertThat(dbUserOpt).isPresent();
        User dbUser = dbUserOpt.get();
        assertThat(dbUser.getName()).isEqualTo(NAME);
        assertThat(dbUser.getRole()).isEqualTo(User.Role.CUSTOMER);
        assertThat(dbUser.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
        assertThat(dbUser.isEnabled()).isTrue();
        assertThat(passwordEncoder.matches(RAW_PASSWORD, dbUser.getPassword())).isTrue();
        verify(userServiceClient).initProfile(eq(saved.getId()),any());
    }

    @Test
    @DisplayName("should reject duplicate email with RuntimeException")
    void duplicateEmailRejected(){
        RegisterRequest request=RegisterRequest.builder()
                .name(NAME)
                .email(EMAIL)
                .password(RAW_PASSWORD)
                .build();
        authService.register(request);

        assertThatThrownBy(()-> authService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already exists");
        assertThat(userRepository.existsByEmail(EMAIL)).isTrue();
    }

    @Test
    @Order(2)
    @Transactional
    @DisplayName("authenticate: should authenticate registered user")
    void authenticate_shouldAuthenticateRegisteredUser(){
        RegisterRequest request=RegisterRequest.builder()
                .name(NAME)
                .email(EMAIL)
                .password(RAW_PASSWORD)
                .build();
        authService.register(request);
        Authentication result = authService.authenticate(EMAIL, RAW_PASSWORD);
        assertThat(result).isNotNull();
        assertThat(result.getPrincipal()).isInstanceOf(UserDetailsImpl.class);
        UserDetailsImpl  userDetails = (UserDetailsImpl) result.getPrincipal();
        assertThat(userDetails.getUsername()).isEqualTo(String.valueOf(userDetails.getId()));
    }

    @Test
    @Transactional
    @Order(3)
    @DisplayName("deleteUser: should soft‑delete user in DB")
    void deleteUser_shouldSoftDeleteUser(){
        RegisterRequest request=RegisterRequest.builder()
                .name(NAME)
                .email(EMAIL)
                .password(RAW_PASSWORD)
                .build();
        User user = authService.register(request);
        authService.deleteUser(user.getId(),"admin@example.com");
        Optional<User> dbUser = userRepository.findById(user.getId());
        assertThat(dbUser).isPresent();
        User deletedUser = dbUser.get();
        assertThat(deletedUser.isEnabled()).isFalse();
        assertThat(deletedUser.getDeletedAt()).isNotNull();
    }




}
