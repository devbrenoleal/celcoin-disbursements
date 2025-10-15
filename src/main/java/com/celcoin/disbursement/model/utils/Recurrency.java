package com.celcoin.disbursement.model.utils;

import lombok.Getter;

@Getter
public enum Recurrency {
    DAILY("DAILY"),
    WEEKLY("WEEKLY"),
    MONTHLY("MONTHLY"),
    ANNUALLY("ANNUALLY");

    Recurrency(String type) {
        this.type = type;
    }

    private final String type;
}
