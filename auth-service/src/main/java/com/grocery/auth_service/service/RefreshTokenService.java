package com.grocery.auth_service.service;

import com.grocery.auth_service.entity.RefreshToken;
import com.grocery.auth_service.entity.User;
import com.grocery.auth_service.exception.authenticationException.UserNotFoundException;
import com.grocery.auth_service.exception.tokenException.JwtExpiredException;
import com.grocery.auth_service.exception.tokenException.RefreshTokenRevokedException;
import com.grocery.auth_service.repository.RefreshTokenRepository;
import com.grocery.auth_service.repository.UserRepository;
import com.grocery.auth_service.security.JwtUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class RefreshTokenService {

    public final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtUtils jwtUtils, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    public RefreshToken createRefreshToken(String email){
        refreshTokenRepository.deleteByUserEmail(email);
        RefreshToken refreshToken=new RefreshToken();
        refreshToken.setUserEmail(email);
        refreshToken.setToken(jwtUtils.generateRefreshToken(email));
        refreshToken.setRevokedAt(null);
        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token){
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyAndRotate(String token){
        RefreshToken existingToken = refreshTokenRepository.findByToken(token).orElseThrow(() ->
                new RefreshTokenRevokedException("Invalid refresh token"));
        if (existingToken.getRevokedAt()!=null){
            throw new RefreshTokenRevokedException("Refresh token already revoked");
        }
        if (existingToken.getExpiryDate().isBefore(LocalDateTime.now())){
            existingToken.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(existingToken);
            throw new RefreshTokenRevokedException("Refresh token expired");
        }
        String email = existingToken.getUserEmail();
        User user = userRepository.findByEmail(email).orElseThrow(() ->
                new UserNotFoundException("User not found: " + email));
        existingToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(existingToken);
        RefreshToken newRefreshToken=new RefreshToken();
        newRefreshToken.setToken(UUID.randomUUID().toString());
        newRefreshToken.setUserEmail(user.getEmail());
        newRefreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(newRefreshToken);
        return newRefreshToken;
    }

    public RefreshToken verifyExpiration(RefreshToken token){
        if (token.isRevoked()){
            throw new RefreshTokenRevokedException("Refresh token revoked");
        }
        if (token.isExpired()){
            refreshTokenRepository.delete(token);
            throw new JwtExpiredException("Refresh token expired");
        }
        return token;
    }

    public void revokeAllByUserEmail(String email){
        refreshTokenRepository.deleteByUserEmail(email);
    }
}
