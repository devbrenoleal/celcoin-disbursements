package com.celcoin.disbursement.model.utils;

import lombok.Getter;

@Getter
public enum StepStatus {
    PENDING("PENDING"),
    PROCESSING("PROCESSING"),
    SUCCESS("SUCCESS"),
    FAILED("FAILED");

    StepStatus(String status) {
        this.status = status;
    }

    private final String status;
}
