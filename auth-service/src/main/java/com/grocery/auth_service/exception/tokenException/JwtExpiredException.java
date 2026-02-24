package com.grocery.auth_service.exception.tokenException;

public class JwtExpiredException extends RuntimeException{

    public JwtExpiredException(String message){
        super(message);
    }
}
