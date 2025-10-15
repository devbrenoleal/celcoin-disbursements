package com.celcoin.disbursement.model.utils;

import lombok.Getter;

import java.io.Serializable;

@Getter
public enum StepType implements Serializable {
    PIX("PIX"),
    TED("TED");

    StepType(String type) {
        this.type = type;
    }

    private final String type;
}
