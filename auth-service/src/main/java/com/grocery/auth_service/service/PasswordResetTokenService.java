package com.grocery.auth_service.service;

import com.grocery.auth_service.entity.PasswordResetToken;
import com.grocery.auth_service.entity.User;
import com.grocery.auth_service.exception.authenticationException.UserNotFoundException;
import com.grocery.auth_service.exception.tokenException.RefreshTokenRevokedException;
import com.grocery.auth_service.repository.PasswordResetTokenRepository;
import com.grocery.auth_service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@Transactional
@Slf4j
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom=new SecureRandom();

    public PasswordResetTokenService(PasswordResetTokenRepository tokenRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Scheduled(cron = "0 0 2 * * ?") // Daily 2 AM
    @Transactional
    public void cleanupExpiredTokens(){
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
        tokenRepository.deleteUsedTokens();
        log.info("Password reset token cleanup completed");
    }

    public String createResetToken(String email){
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found: " + email));

        // Delete existing tokens for this user
        tokenRepository.deleteByUserId(user.getId());

        // Generate secure raw token
        String rawToken = generateSecureToken();
        String tokenHash = passwordEncoder.encode(rawToken);
        PasswordResetToken resetToken=PasswordResetToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .build();
        tokenRepository.save(resetToken);

//        emailService.sendPasswordResetEmail(email, rawToken);

        log.info("Password reset token created for: {}", email);
        return rawToken;

    }

    @Transactional
    public PasswordResetToken validateAndUseToken(String rawToken, String email){

        // Atomic validation with hashing
        PasswordResetToken resetToken = tokenRepository.findValidTokenByEmailAndRawToken(email, rawToken, passwordEncoder.encode(rawToken)).orElseThrow(() -> new RefreshTokenRevokedException("Invalid or expired token"));

        // Mark as used
        resetToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(resetToken);
        return resetToken;
    }

    public void resetUserPassword(String rawToken, String email, String newPassword){
        PasswordResetToken resetToken = validateAndUseToken(rawToken, email);
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Password reset completed for user: {}", email);
    }



    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
