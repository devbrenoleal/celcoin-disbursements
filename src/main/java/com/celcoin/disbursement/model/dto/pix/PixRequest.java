package com.celcoin.disbursement.model.dto.pix;


import com.celcoin.disbursement.model.dto.CreditParty;

import java.math.BigDecimal;

public record PixRequest(BigDecimal amount, String clientRequestId, CreditParty creditParty, String initiationType) {
}
