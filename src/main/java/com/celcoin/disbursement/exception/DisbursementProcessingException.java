package com.celcoin.disbursement.exception;

public class DisbursementProcessingException extends ExceptionDefinition {

    public DisbursementProcessingException(String errorCode, String message, Throwable ex) {
        super(errorCode, message, ex);
    }

    public DisbursementProcessingException(String message, String errorCode) {
        super(message, errorCode);
    }
}
