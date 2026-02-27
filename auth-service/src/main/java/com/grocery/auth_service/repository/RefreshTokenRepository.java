package com.grocery.auth_service.repository;

import com.grocery.auth_service.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Long> {
    void deleteByUserId(Long userId);
    Optional<RefreshToken> findByToken(String token);
}
