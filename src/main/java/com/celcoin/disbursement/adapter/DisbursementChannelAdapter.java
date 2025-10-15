package com.celcoin.disbursement.adapter;

import com.celcoin.disbursement.model.dto.DisbursementDto;
import com.celcoin.disbursement.model.dto.DisbursementResponse;
import com.celcoin.disbursement.model.entity.DisbursementStep;

@FunctionalInterface
public interface DisbursementChannelAdapter {
    void send(DisbursementStep step);
}
