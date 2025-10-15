package com.celcoin.disbursement.model.utils;

import lombok.Getter;

@Getter
public enum BatchStatus {
    PROCESSING("PROCESSING"),
    RECURRENT("RECURRENT"),
    EXECUTED_COMPLETELY("EXECUTED_COMPLETELY"),
    PARTIALLY_EXECUTED("PARTIALLY_EXECUTED"),
    FAILED("FAILED"),
    NOT_EXECUTED("NOT_EXECUTED");

    BatchStatus(String status) {
        this.status = status;
    }

    private final String status;
}
