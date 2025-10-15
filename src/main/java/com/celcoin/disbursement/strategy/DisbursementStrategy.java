package com.celcoin.disbursement.strategy;

import com.celcoin.disbursement.model.utils.StepType;
import com.celcoin.disbursement.model.entity.DisbursementStep;

public interface DisbursementStrategy {
    void execute(DisbursementStep step);
    StepType getChannelType();
}
