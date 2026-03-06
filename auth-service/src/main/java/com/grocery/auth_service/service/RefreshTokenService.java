package com.grocery.auth_service.service;

import com.grocery.auth_service.entity.RefreshToken;
import com.grocery.auth_service.exception.tokenException.RefreshTokenRevokedException;
import com.grocery.auth_service.repository.RefreshTokenRepository;
import com.grocery.auth_service.security.JwtUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class RefreshTokenService {

    public final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtils jwtUtils;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtUtils jwtUtils) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtils = jwtUtils;
    }

    public RefreshToken createRefreshToken(Long userId){
        refreshTokenRepository.deleteByUserId(userId);
        RefreshToken refreshToken=new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(jwtUtils.generateRefreshToken(userId));
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
        Long userId  = existingToken.getUserId();
        existingToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(existingToken);


        // create new token consistently
        RefreshToken newRefreshToken=new RefreshToken();
        newRefreshToken.setToken(jwtUtils.generateRefreshToken(userId));
        newRefreshToken.setUserId(userId);
        newRefreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        newRefreshToken.setRevokedAt(null);
        refreshTokenRepository.save(newRefreshToken);
        return newRefreshToken;
    }
}
