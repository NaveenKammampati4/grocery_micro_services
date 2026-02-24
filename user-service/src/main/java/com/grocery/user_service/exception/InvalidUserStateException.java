package com.grocery.user_service.exception;

public class InvalidUserStateException extends RuntimeException{

    public InvalidUserStateException(String message){
        super(message);
    }
}
