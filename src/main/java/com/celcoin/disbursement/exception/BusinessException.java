package com.celcoin.disbursement.exception;

import lombok.Getter;

@Getter
public class BusinessException extends ExceptionDefinition {

    public BusinessException(String errorCode, String message, Throwable ex) {
        super(errorCode, message, ex);
    }

    public BusinessException(String errorCode, String message) {
        super(errorCode, message);
    }
}
