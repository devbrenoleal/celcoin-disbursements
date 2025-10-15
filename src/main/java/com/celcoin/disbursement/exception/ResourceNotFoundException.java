package com.celcoin.disbursement.exception;

public class ResourceNotFoundException extends ExceptionDefinition {
    public ResourceNotFoundException(String errorCode, String message, Throwable ex) {
        super(errorCode, message, ex);
    }

    public ResourceNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
