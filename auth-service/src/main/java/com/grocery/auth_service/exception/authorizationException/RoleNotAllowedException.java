package com.grocery.auth_service.exception.authorizationException;

public class RoleNotAllowedException extends RuntimeException{
    public RoleNotAllowedException(String message){
        super(message);
    }

}
