package com.grocery.auth_service.exception;
import com.grocery.auth_service.dto.response.ErrorResponse;
import com.grocery.auth_service.exception.authenticationException.*;
import com.grocery.auth_service.exception.tokenException.RefreshTokenRevokedException;
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
