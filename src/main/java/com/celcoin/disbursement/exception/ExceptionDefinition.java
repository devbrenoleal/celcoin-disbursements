package com.celcoin.disbursement.exception;

import lombok.Getter;

@Getter
public abstract class ExceptionDefinition extends RuntimeException {

    private final String errorCode;

    public ExceptionDefinition(String errorCode, String message, Throwable ex) {
        super(message, ex);
        this.errorCode = errorCode;
    }

    public ExceptionDefinition(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}