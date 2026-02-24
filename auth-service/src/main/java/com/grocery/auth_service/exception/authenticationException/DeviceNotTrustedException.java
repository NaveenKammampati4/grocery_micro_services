package com.grocery.auth_service.exception.authenticationException;

public class DeviceNotTrustedException extends RuntimeException{
    public DeviceNotTrustedException(String message){
        super(message);
    }
}
