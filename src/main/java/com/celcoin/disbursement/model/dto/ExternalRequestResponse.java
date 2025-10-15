package com.celcoin.disbursement.model.dto;

import com.celcoin.disbursement.model.utils.StepStatus;

public record ExternalRequestResponse(String clientRequestId, String externalId, StepStatus status, String failureReason) {
}
