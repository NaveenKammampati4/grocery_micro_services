package com.grocery.auth_service.repository;

import com.grocery.auth_service.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken,Long> {

//    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);


    @Query("SELECT prt FROM PasswordResetToken prt WHERE " +
            "prt.user.email = :email AND " +
            "prt.expiryDate > CURRENT_TIMESTAMP AND " +
            "prt.usedAt IS NULL AND " +
            ":tokenHash = FUNCTION('encode', :rawToken, 'bcrypt')")
    Optional<PasswordResetToken> findValidTokenByEmailAndRawToken(
           @Param("email") String email,
           @Param("rawToken") String rawToken,
           @Param("tokenHash") String tokenHash
    );

    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.expiryDate < :expiryDate")
    void deleteExpiredTokens(@Param("expiryDate") LocalDateTime expiryDate);

    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.usedAt IS NOT NULL")
    void deleteUsedTokens();
}
