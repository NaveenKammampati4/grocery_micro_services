package com.grocery.user_service.exception;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        int status,
        String errorCode,
        String message,
        LocalDateTime timestamp
) {}
