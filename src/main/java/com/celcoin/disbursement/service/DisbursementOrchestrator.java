package com.celcoin.disbursement.service;

import com.celcoin.disbursement.model.utils.StepType;
import com.celcoin.disbursement.model.entity.DisbursementStep;
import com.celcoin.disbursement.strategy.DisbursementStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DisbursementOrchestrator {

    private final Map<StepType, DisbursementStrategy> strategies;

    @Autowired
    public DisbursementOrchestrator(List<DisbursementStrategy> strategies) {
        this.strategies = strategies.stream().collect(Collectors.toMap(DisbursementStrategy::getChannelType, Function.identity()));
    }

    public void process(DisbursementStep step) {
        DisbursementStrategy strategy = strategies.get(step.getType());

        if (strategy == null) throw new UnsupportedOperationException("Channel not supported: " + step.getType());

        strategy.execute(step);
    }
}
