package com.celcoin.disbursement.model.dto.ted;

import com.celcoin.disbursement.model.dto.CreditParty;

import java.math.BigDecimal;

public record TedRequest(BigDecimal amount, String clientRequestId, CreditParty creditParty) {
}
