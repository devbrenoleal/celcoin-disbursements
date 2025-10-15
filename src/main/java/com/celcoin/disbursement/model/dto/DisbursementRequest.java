package com.celcoin.disbursement.model.dto;

import java.util.List;

public record DisbursementRequest(String clientCode, Schedule schedule, List<DisbursementDto> disbursements) {
}
