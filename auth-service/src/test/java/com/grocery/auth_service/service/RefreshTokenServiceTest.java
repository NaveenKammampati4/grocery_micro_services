package com.grocery.auth_service.service;

import com.grocery.auth_service.entity.RefreshToken;
import com.grocery.auth_service.exception.tokenException.RefreshTokenRevokedException;
import com.grocery.auth_service.repository.RefreshTokenRepository;
import com.grocery.auth_service.security.JwtUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private RefreshTokenService tokenService;

    private final Long USER_ID=1L;
    private final String TOKEN="test-refresh-token";

    @Test
    @DisplayName("should delete old token and create new token")
    void createRefreshToken_shouldDeleteOldTokenAndCreateNewOne(){
        when(jwtUtils.generateRefreshToken(USER_ID)).thenReturn(TOKEN);
        RefreshToken savedToken=new RefreshToken();
        savedToken.setUserId(USER_ID);
        savedToken.setToken(TOKEN);
        savedToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.save(any())).thenReturn(savedToken);
        RefreshToken result = tokenService.createRefreshToken(USER_ID);
        verify(refreshTokenRepository).deleteByUserId(USER_ID);
        verify(jwtUtils).generateRefreshToken(USER_ID);
        verify(refreshTokenRepository).save(any());

        assertNotNull(result);
        assertEquals(USER_ID, result.getUserId());
        assertEquals(TOKEN,result.getToken());
    }

    @Test
    @DisplayName("should throw RefreshTokenRevokedException when refresh token is not found")
    void verifyAndRotate_shouldThrowIfTokenNotFound(){
        when(refreshTokenRepository.findByToken(TOKEN))
                .thenReturn(Optional.empty());
        assertThrows(RefreshTokenRevokedException.class, ()-> tokenService.verifyAndRotate(TOKEN));
    }

    @Test
    void verifyAndRotate_shouldThrowIfAlreadyRevoked(){
        RefreshToken token=new RefreshToken();
        token.setToken(TOKEN);
        token.setRevokedAt(LocalDateTime.now());

        when(refreshTokenRepository.findByToken(TOKEN))
                .thenReturn(Optional.of(token));
        assertThrows(RefreshTokenRevokedException.class,
                () -> tokenService.verifyAndRotate(TOKEN));
    }

    @Test
    @DisplayName("Should return refresh token when token exists in repository")
    void findByToken_shouldReturnToken_whenTokenExists(){
        RefreshToken token=new RefreshToken();
        token.setToken(TOKEN);
        token.setUserId(USER_ID);

        when(refreshTokenRepository.findByToken(TOKEN))
                .thenReturn(Optional.of(token));
        Optional<RefreshToken> result = tokenService.findByToken(TOKEN);
        assertTrue(result.isPresent());
        assertEquals(TOKEN, result.get().getToken());
        assertEquals(USER_ID, result.get().getUserId());
        verify(refreshTokenRepository).findByToken(TOKEN);
    }

    @Test
    @DisplayName("should throw RefreshTokenRevokedException when token expired")
    void verifyAndRotate_shouldThrowIfExpired() {
        RefreshToken token = new RefreshToken();
        token.setToken(TOKEN);
        token.setUserId(USER_ID);
        token.setExpiryDate(LocalDateTime.now().minusDays(1));
        token.setRevokedAt(null);

        when(refreshTokenRepository.findByToken(TOKEN))
                .thenReturn(Optional.of(token));

        assertThrows(RefreshTokenRevokedException.class,
                () -> tokenService.verifyAndRotate(TOKEN));

        verify(refreshTokenRepository).save(token);
    }

    @Test
    void verifyAndRotate_shouldRevokeOldAndCreateNewToken(){
        RefreshToken token=new RefreshToken();
        token.setUserId(USER_ID);
        token.setToken(TOKEN);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));
        token.setRevokedAt(null);
        when(refreshTokenRepository.findByToken(TOKEN))
                .thenReturn(Optional.of(token));
        when(jwtUtils.generateRefreshToken(USER_ID))
                .thenReturn("new-token");
        when(refreshTokenRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        RefreshToken result = tokenService.verifyAndRotate(TOKEN);

        assertNotNull(result);
        assertEquals(USER_ID, result.getUserId());
        assertEquals("new-token", result.getToken());

        verify(refreshTokenRepository, times(2)).save(any());
    }
}
