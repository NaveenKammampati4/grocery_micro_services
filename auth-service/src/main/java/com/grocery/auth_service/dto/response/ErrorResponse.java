package com.grocery.auth_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String statusCode; // AUTH_401
    private int httpStatus;
    private String error;
    private String message;
    private LocalDateTime timestamp;

    public ErrorResponse(String statusCode, int httpStatus, String error, String message, LocalDateTime timestamp) {
        this.statusCode = statusCode;
        this.httpStatus = httpStatus;
        this.error = error;
        this.message = message;
        this.timestamp = timestamp;
    }
}
