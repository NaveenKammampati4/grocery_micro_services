package com.grocery.auth_service.service;

import com.grocery.auth_service.entity.PasswordResetToken;
import com.grocery.auth_service.entity.User;
import com.grocery.auth_service.exception.authenticationException.UserNotFoundException;
import com.grocery.auth_service.exception.tokenException.RefreshTokenRevokedException;
import com.grocery.auth_service.repository.PasswordResetTokenRepository;
import com.grocery.auth_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PasswordResetTokenServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetTokenService service;

    private User user;
    private PasswordResetToken resetToken;

    private static final String EMAIL="user@example.com";
    private static final String RAW_TOKEN = "raw-token";
    private static final String HASHED_TOKEN = "hashed-token";
    private static final String NEW_PASSWORD = "newPassword123";

    @BeforeEach
    void setUp(){
        user=User.builder()
                .id(1L)
                .email(EMAIL)
                .password("oldPassword")
                .build();
        resetToken=PasswordResetToken.builder()
                .id(1L)
                .tokenHash(HASHED_TOKEN)
                .user(user)
                .build();
    }

    // cleanupExpiredTokens
    @Test
    @DisplayName("cleanupExpiredTokens: should delete expired and used tokens")
    void cleanupExpiredTokens_success(){
        service.cleanupExpiredTokens();
        verify(tokenRepository).deleteExpiredTokens(any(LocalDateTime.class));
        verify(tokenRepository).deleteUsedTokens();
        verifyNoMoreInteractions(tokenRepository);
    }

    // createResetToken

    @Test
    @DisplayName("createResetToken: should create token successfully")
    void createResetToken_success(){
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString()))
                .thenReturn(HASHED_TOKEN);
        String rawToken = service.createResetToken(EMAIL);
        assertThat(rawToken).isNotBlank();
        verify(tokenRepository).deleteByUserId(user.getId());
        verify(passwordEncoder).encode(rawToken);
        ArgumentCaptor<PasswordResetToken> captor=ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        PasswordResetToken savedToken = captor.getValue();
        assertThat(savedToken.getTokenHash()).isEqualTo(HASHED_TOKEN);
        assertThat(savedToken.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("createResetToken: should throw UserNotFoundException when user does not exist")
    void createResetToken_userNotFound(){
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());
        assertThatThrownBy(()-> service.createResetToken(EMAIL))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found: "+EMAIL);
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("validateAndUseToken: should validate and mark token as used")
    void validateAndUseToken_success(){
        when(passwordEncoder.encode(RAW_TOKEN))
                .thenReturn(HASHED_TOKEN);
        when(tokenRepository.findValidTokenByEmailAndRawToken(
                eq(EMAIL),
                eq(RAW_TOKEN),
                eq(HASHED_TOKEN)
        )).thenReturn(Optional.of(resetToken));

        PasswordResetToken result=service.validateAndUseToken(RAW_TOKEN,EMAIL);
        assertThat(result).isEqualTo(resetToken);
        assertThat(resetToken.getUsedAt()).isNotNull();
        verify(tokenRepository).save(resetToken);
    }

    @Test
    @DisplayName("validateAndUseToken: should throw RefreshTokenRevokedException when token invalid")
    void validateAndUseToken_invalidToken(){
        when(passwordEncoder.encode(RAW_TOKEN))
                .thenReturn(HASHED_TOKEN);
        when(tokenRepository.findValidTokenByEmailAndRawToken(anyString(),anyString(),anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(()-> service.validateAndUseToken(RAW_TOKEN,EMAIL))
                .isInstanceOf(RefreshTokenRevokedException.class)
                .hasMessage("Invalid or expired token");
        verify(tokenRepository,never()).save(any());
    }

    @Test
    @DisplayName("resetUserPassword: should reset user password successfully")
    void resetUserPassword_success(){
        when(passwordEncoder.encode(RAW_TOKEN))
                .thenReturn(HASHED_TOKEN);
        when(tokenRepository.findValidTokenByEmailAndRawToken(
                eq(EMAIL),
                eq(RAW_TOKEN),
                eq(HASHED_TOKEN)
        )).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode(NEW_PASSWORD))
                .thenReturn("encoded-new-password");

        service.resetUserPassword(RAW_TOKEN, EMAIL, NEW_PASSWORD);

        assertThat(user.getPassword()).isEqualTo("encoded-new-password");
        assertThat(user.getUpdatedAt()).isNotNull();

        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("resetUserPassword: should propagate exception when token invalid")
    void resetUserPassword_invalidToken() {

        when(passwordEncoder.encode(RAW_TOKEN))
                .thenReturn(HASHED_TOKEN);

        when(tokenRepository.findValidTokenByEmailAndRawToken(
                anyString(), anyString(), anyString()
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.resetUserPassword(RAW_TOKEN, EMAIL, NEW_PASSWORD))
                .isInstanceOf(RefreshTokenRevokedException.class);

        verify(userRepository, never()).save(any());
    }
}
