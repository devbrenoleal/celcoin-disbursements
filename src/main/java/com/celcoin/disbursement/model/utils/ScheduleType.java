package com.celcoin.disbursement.model.utils;

import lombok.Getter;

@Getter
public enum ScheduleType {
    IMMEDIATE("IMMEDIATE"),
    SCHEDULED("SCHEDULED"),
    RECURRENT("RECURRENT");

    ScheduleType(String type) {
        this.type = type;
    }

    private final String type;
}
