package com.grocery.auth_service.exception.tokenException;

public class JwtSignatureInvalidException extends RuntimeException{
    public JwtSignatureInvalidException(String message){
        super(message);
    }
}
