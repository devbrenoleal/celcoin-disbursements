package com.celcoin.disbursement.strategy;

import com.celcoin.disbursement.adapter.TedAdapter;
import com.celcoin.disbursement.model.utils.StepType;
import com.celcoin.disbursement.model.entity.DisbursementStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TedDisbursementStrategy implements DisbursementStrategy {

    private static final Logger logger = LoggerFactory.getLogger(TedDisbursementStrategy.class);

    @Autowired
    private TedAdapter adapter;

    @Override
    public void execute(DisbursementStep step) {
        logger.info("INICIANDO DESEMBOLSO TED para o step: {}, Valor: {}", step.getId(), step.getAmount());
        adapter.send(step);
    }

    @Override
    public StepType getChannelType() {
        return StepType.TED;
    }
}
