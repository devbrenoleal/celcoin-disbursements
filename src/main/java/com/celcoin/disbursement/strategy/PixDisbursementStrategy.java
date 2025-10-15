package com.celcoin.disbursement.strategy;

import com.celcoin.disbursement.adapter.PixAdapter;
import com.celcoin.disbursement.model.utils.StepType;
import com.celcoin.disbursement.model.entity.DisbursementStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PixDisbursementStrategy implements DisbursementStrategy {

    @Autowired
    private PixAdapter pixAdapter;

    private static final Logger logger = LoggerFactory.getLogger(PixDisbursementStrategy.class);

    @Override
    public void execute(DisbursementStep step) {
        logger.info("INICIANDO DESEMBOLSO PIX para o step: {}, Valor: {}", step.getId(), step.getAmount());
        pixAdapter.send(step);
    }

    @Override
    public StepType getChannelType() {
        return StepType.PIX;
    }
}