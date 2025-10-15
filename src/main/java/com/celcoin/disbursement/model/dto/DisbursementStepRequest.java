package com.celcoin.disbursement.model.dto;

import java.math.BigDecimal;

public record DisbursementStepRequest(BigDecimal amount, CreditParty creditParty, String initiationType) {
}
