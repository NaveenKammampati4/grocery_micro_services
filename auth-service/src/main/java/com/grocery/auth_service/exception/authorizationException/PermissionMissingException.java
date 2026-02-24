package com.grocery.auth_service.exception.authorizationException;

public class PermissionMissingException extends RuntimeException{

    public PermissionMissingException(String message){
        super(message);
    }
}
