package com.celcoin.disbursement.controller;

import com.celcoin.disbursement.exception.BusinessException;
import com.celcoin.disbursement.exception.DisbursementProcessingException;
import com.celcoin.disbursement.exception.ResourceNotFoundException;
import com.celcoin.disbursement.model.dto.DisbursementErrorResponse;
import com.celcoin.disbursement.model.dto.DisbursementResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionControllerAdvice.class);

    @ExceptionHandler(exception = {BusinessException.class, ResourceNotFoundException.class})
    public ResponseEntity<DisbursementErrorResponse> handleBusinessException(BusinessException ex) {
        logger.warn("{} - {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(400).body(new DisbursementErrorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(DisbursementProcessingException.class)
    public ResponseEntity<DisbursementErrorResponse> handleDisbursementProcessingException(DisbursementProcessingException ex) {
        logger.warn("{} - {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(400).body(new DisbursementErrorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DisbursementErrorResponse> handleUnkownException(Exception ex) {
        logger.error("Unexpected Exception while processing data", ex);
        return ResponseEntity.internalServerError().body(new DisbursementErrorResponse("999", "Ocorreu um erro inesperado"));
    }
}
