package com.celcoin.disbursement.service;

import com.celcoin.disbursement.exception.ResourceNotFoundException;
import com.celcoin.disbursement.model.entity.DisbursementStep;
import com.celcoin.disbursement.model.utils.StepStatus;
import com.celcoin.disbursement.repository.DisbursementStepRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DisbursementProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(DisbursementProcessingService.class);

    private final DisbursementStepRepository stepRepository;
    private final DisbursementOrchestrator orchestrator;

    public DisbursementProcessingService(DisbursementStepRepository stepRepository, DisbursementOrchestrator orchestrator) {
        this.stepRepository = stepRepository;
        this.orchestrator = orchestrator;
    }

    @Transactional
    public void execute(String stepId) {
        logger.info("Iniciando processamento do stepId: {}", stepId);

        DisbursementStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new ResourceNotFoundException("400", "Step não encontrado para o id: " + stepId));

        // Simplificamos a checagem de idempotência, confiando mais no IdempotencyService.
        // A checagem de status ainda é uma boa prática como uma segunda barreira (guarda de estado).
        if (step.getStatus() != StepStatus.PENDING) {
            logger.warn("Step {} já foi processado ou está em andamento. Status atual: {}. Ignorando.", stepId, step.getStatus());
            return;
        }

        step.setStatus(StepStatus.PROCESSING);
        step.setUpdatedAt(LocalDateTime.now());
        stepRepository.save(step);

        orchestrator.process(step);
        logger.info("Processamento do stepId {} delegado para o orquestrador.", stepId);
    }
}
