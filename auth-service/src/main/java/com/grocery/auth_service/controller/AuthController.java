package com.grocery.auth_service.controller;

import com.grocery.auth_service.dto.request.ForgotPasswordRequest;
import com.grocery.auth_service.dto.request.LoginRequest;
import com.grocery.auth_service.dto.request.RegisterRequest;
import com.grocery.auth_service.dto.request.ResetPasswordRequest;
import com.grocery.auth_service.dto.response.JwtResponse;
import com.grocery.auth_service.dto.response.PasswordResetResponse;
import com.grocery.auth_service.entity.RefreshToken;
import com.grocery.auth_service.exception.authenticationException.UserNotFoundException;
import com.grocery.auth_service.exception.tokenException.RefreshTokenRevokedException;
import com.grocery.auth_service.security.JwtUtils;
import com.grocery.auth_service.security.UserDetailsImpl;
import com.grocery.auth_service.service.AuthService;
import com.grocery.auth_service.service.PasswordResetTokenService;
import com.grocery.auth_service.service.RefreshTokenService;
import com.grocery.auth_service.util.CookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RequestMapping("/api/auth")
@RestController
public class AuthController {

    private static final Logger logger= LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final CookieService cookieService;
    private final PasswordResetTokenService passwordResetTokenService;


    public AuthController(AuthService authService, JwtUtils jwtUtils, RefreshTokenService refreshTokenService, CookieService cookieService, PasswordResetTokenService passwordResetTokenService) {
        this.authService = authService;
        this.jwtUtils = jwtUtils;
        this.refreshTokenService = refreshTokenService;
        this.cookieService = cookieService;
        this.passwordResetTokenService = passwordResetTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletResponse response) {
        logger.info("Register attempt for email: {}", request.getEmail());
        var user = authService.register(request);
        String accessToken = jwtUtils.generateJwtTokenFromUser(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
        cookieService.setAccessTokenCookie(response, accessToken);
        cookieService.setRefreshTokenCookie(response, refreshToken.getToken());
        logger.info("User registered successfully: {}", user.getEmail());
        return ResponseEntity.ok(Map.of("message", "Registration successful", "userId", user.getId()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        logger.info("Login attempt for email: {}", request.getEmail());
        Authentication authentication = authService.authenticate(request.getEmail(), request.getPassword());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        var userDetails = (UserDetailsImpl) authentication.getPrincipal();
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());
        cookieService.setAccessTokenCookie(response, jwt);
        cookieService.setRefreshTokenCookie(response, refreshToken.getToken());
        logger.info("User logged in successfully: {}", userDetails.getUsername());
        return ResponseEntity.ok(new JwtResponse(jwt, refreshToken.getToken(),userDetails.getUsername(), userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response){
        logger.info("Token refresh attempt");
        String  refreshToken = cookieService.getCookieValue(request,"refreshToken");
        if (refreshToken == null) {
            logger.warn("Refresh token missing in request");
            throw new RefreshTokenRevokedException("Refresh token not found");
        }
        RefreshToken newRefreshToken = refreshTokenService.verifyAndRotate(refreshToken);
        String email = String.valueOf(newRefreshToken.getId());
//        String newAccessToken = jwtUtils.generateJwtTokenFromUser(email);
        String newAccessToken = jwtUtils.generateTokenFromEmail(email);
        cookieService.setAccessTokenCookie(response, newAccessToken);
        cookieService.setRefreshTokenCookie(response, newRefreshToken.getToken());
        logger.info("Token refreshed successfully for user: {}", email);
        return ResponseEntity.ok(Map.of("message", "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        logger.info("Logout attempt for refresh token");
        String refreshToken = cookieService.getCookieValue(request, "refreshToken");
        if (refreshToken!=null){
            refreshTokenService.findByToken(refreshToken)
                    .ifPresent(rt -> {
                        rt.setRevokedAt(LocalDateTime.now());
                        refreshTokenService.refreshTokenRepository.save(rt);
                        logger.info("Refresh token revoked for user: {}", rt.getUserId());
                    });
        }

        cookieService.clearAccessTokenCookie(response);
        cookieService.clearRefreshTokenCookie(response);
        SecurityContextHolder.clearContext();
        logger.info("User logged out successfully");
        return ResponseEntity.ok(Map.of("message","Logout successful"));
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId.toString() == authentication.name")
    public ResponseEntity<?> deleteUser(@PathVariable @Positive Long userId, Authentication authentication){
        try {
           authService.deleteUser(userId,authentication.getName());
            return ResponseEntity.ok("User deleted successfully");
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found: " + userId);
        }catch (Exception e){
            logger.error("Unexpected error deleting user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to deactivate user");
        }
    }

    @PatchMapping("/users/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> activateUser(@PathVariable @Positive Long userId){
        try {
           authService.activateUser(userId);
           return ResponseEntity.ok("User activated successfully");
        }catch (UserNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found: " + userId);
        } catch (Exception e) {
            logger.error("Unexpected error activating user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to activate user");
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request){
        logger.info("Forgot password request for: {}", request.getEmail());
        try {
            passwordResetTokenService.createResetToken(request.getEmail());
            return ResponseEntity.ok(PasswordResetResponse.builder()
                    .success(true)
                    .message("If account exists, check your email for reset instructions.")
                    .build()
            );
        } catch (UserNotFoundException e) {
            logger.warn("Forgot password for non-existent email: {}", request.getEmail());
            return ResponseEntity.ok(PasswordResetResponse.builder()
                    .success(false)
                    .message("If account exists, check your email for reset instructions.")
                    .build()
            );
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<PasswordResetResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request){
        try {
           passwordResetTokenService.resetUserPassword(request.getToken(), request.getEmail(),request.getPassword());
           return ResponseEntity.ok(PasswordResetResponse.builder()
                   .success(true)
                   .message("Password reset successfully")
                   .build()
           );
        } catch (RefreshTokenRevokedException e) {
            logger.warn("Invalid reset token: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(PasswordResetResponse.builder()
                            .success(false)
                            .message("Invalid or expired reset token")
                            .build());
        }

    }

}
