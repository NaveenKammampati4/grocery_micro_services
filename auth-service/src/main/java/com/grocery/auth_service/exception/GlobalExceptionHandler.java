package com.grocery.auth_service.exception;
import com.grocery.auth_service.dto.response.ErrorResponse;
import com.grocery.auth_service.exception.authenticationException.*;
import com.grocery.auth_service.exception.authorizationException.PermissionMissingException;
import com.grocery.auth_service.exception.authorizationException.RoleNotAllowedException;
import com.grocery.auth_service.exception.tokenException.JwtExpiredException;
import com.grocery.auth_service.exception.tokenException.JwtSignatureInvalidException;
import com.grocery.auth_service.exception.tokenException.RefreshTokenRevokedException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log= LoggerFactory.getLogger(GlobalExceptionHandler.class);


    // WARN : Expected auth failures (expired tokens, wrong password, permission checks)
    // ERROR: Security attacks (invalid JWT signature), unexpected failures
    // No stack trace for password attempts (security best practice)


    @ExceptionHandler(DeviceNotTrustedException.class)
    public ResponseEntity<ErrorResponse> handleDeviceNotTrusted(DeviceNotTrustedException ex, HttpServletRequest request){
        log.warn("Device not trusted : {}", ex.getMessage(), ex);
        ErrorResponse response=ErrorResponse.builder()
                .statusCode("AUTH_403")
                .httpStatus(HttpStatus.FORBIDDEN.value())
                .error("DEVICE_NOT_TRUSTED")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(OtpExpiredException.class)
    public ResponseEntity<ErrorResponse> handleOtpExpired(OtpExpiredException ex){
        log.warn("OTP expired: {}", ex.getMessage());
        ErrorResponse response=ErrorResponse.builder()
                .statusCode("AUTH_401")
                .httpStatus((HttpStatus.UNAUTHORIZED.value()))
                .error("OTP_EXPIRED")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorResponse> handlePasswordMismatch(PasswordMismatchException ex){
        log.warn("Password mismatch attempt: {}", ex.getMessage());
        ErrorResponse response=ErrorResponse.builder()
                .statusCode("AUTH_400")
                .httpStatus((HttpStatus.BAD_REQUEST.value()))
                .error("PASSWORD_MISMATCH")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PermissionMissingException.class)
    public ResponseEntity<ErrorResponse> handlePermissionMissing(PermissionMissingException ex){
        log.warn("Permission denied: {}", ex.getMessage(), ex);
        ErrorResponse response=ErrorResponse.builder()
                .statusCode("AUTH_403")
                .httpStatus((HttpStatus.FORBIDDEN.value()))
                .error("PASSWORD_MISMATCH")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex){
        log.warn("Permission denied: {}", ex.getMessage(), ex);
        ErrorResponse response=ErrorResponse.builder()
                .statusCode("AUTH_404")
                .httpStatus((HttpStatus.NOT_FOUND.value()))
                .error("USER_NOT_FOUND")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RoleNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleRoleNotAllowed(RoleNotAllowedException ex){
        log.warn("Role not allowed: {}", ex.getMessage(), ex);
        ErrorResponse response=ErrorResponse.builder()
                .statusCode("AUTH_403")
                .httpStatus((HttpStatus.FORBIDDEN.value()))
                .error("ROLE_NOT_ALLOWED")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(JwtExpiredException.class)
    public ResponseEntity<ErrorResponse> handleJwtExpired(JwtExpiredException ex){
        log.warn("JWT expired: {}", ex.getMessage());
        ErrorResponse response=ErrorResponse.builder()
                .statusCode("AUTH_401")
                .httpStatus((HttpStatus.UNAUTHORIZED.value()))
                .error("JWT_EXPIRED")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(JwtSignatureInvalidException.class)
    public ResponseEntity<ErrorResponse> handleJwtSignatureInvalid(JwtSignatureInvalidException ex){
        log.error("JWT signature invalid: {}", ex.getMessage(), ex);
        ErrorResponse response=ErrorResponse.builder()
                .statusCode("AUTH_401")
                .httpStatus((HttpStatus.UNAUTHORIZED.value()))
                .error("JWT_INVALID_SIGNATURE")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(RefreshTokenRevokedException.class)
    public ResponseEntity<ErrorResponse> handleRefreshTokenRevoked(RefreshTokenRevokedException ex){
        log.warn("Refresh token revoked: {}", ex.getMessage());
        ErrorResponse response=ErrorResponse.builder()
                .statusCode("AUTH_401")
                .httpStatus((HttpStatus.UNAUTHORIZED.value()))
                .error("JWT_INVALID_SIGNATURE")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex){
        log.warn("Duplicate email found: {}", ex.getMessage());
        ErrorResponse response=ErrorResponse.builder()
                .statusCode("AUTH_409")
                .httpStatus((HttpStatus.CONFLICT.value()))
                .error("DUPLICATE_EMAIL")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex){
        ErrorResponse response=ErrorResponse.builder()
                .statusCode("SERVER_500")
                .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("INTERNAL_SERVER_ERROR")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
