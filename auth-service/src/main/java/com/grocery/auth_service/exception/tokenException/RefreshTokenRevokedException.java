package com.grocery.auth_service.exception.tokenException;

public class RefreshTokenRevokedException extends RuntimeException{

    public RefreshTokenRevokedException(String message){
        super(message);
    }
}
