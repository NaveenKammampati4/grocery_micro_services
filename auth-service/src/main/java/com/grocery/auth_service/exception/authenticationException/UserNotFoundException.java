package com.grocery.auth_service.exception.authenticationException;

public class UserNotFoundException extends RuntimeException{

    public UserNotFoundException(String message){
        super(message);
    }
}
