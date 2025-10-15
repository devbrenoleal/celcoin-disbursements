package com.celcoin.disbursement.model.dto;

import com.celcoin.disbursement.model.utils.StepStatus;

public record StepStatusResponse(String stepId, StepStatus status, String externalId) {
}
