package com.celcoin.disbursement.model.dto;

import com.celcoin.disbursement.model.utils.BatchStatus;

import java.util.List;

public record DisbursementStatusResponse(String batchId, BatchStatus status, String clientCode, List<StepStatusResponse> steps) {
}
