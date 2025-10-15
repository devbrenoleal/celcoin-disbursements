package com.celcoin.disbursement.model.utils;

import lombok.Getter;

@Getter
public enum JobStatus {
    PENDING("PENDING"),
    SENT("SENT"),
    FAILED("FAILED");

    JobStatus(String status) {
        this.status = status;
    }

    private final String status;
}
