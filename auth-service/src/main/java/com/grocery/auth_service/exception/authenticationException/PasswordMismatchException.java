package com.grocery.auth_service.exception.authenticationException;

public class PasswordMismatchException extends RuntimeException{

    public PasswordMismatchException(String message){
        super(message);
    }
}
