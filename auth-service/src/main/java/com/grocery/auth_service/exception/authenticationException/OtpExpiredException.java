package com.grocery.auth_service.exception.authenticationException;

public class OtpExpiredException extends RuntimeException{

    public OtpExpiredException(String message){
        super(message);
    }
}
