package com.celcoin.disbursement.model.dto;

import com.celcoin.disbursement.model.utils.StepType;

public record DisbursementDto(StepType type, DisbursementStepRequest disbursementStep) {
}
