package com.celcoin.disbursement.service;

import com.celcoin.disbursement.exception.DisbursementProcessingException;
import com.celcoin.disbursement.model.dto.ExternalRequestResponse;
import com.celcoin.disbursement.model.entity.DisbursementBatch;
import com.celcoin.disbursement.model.entity.DisbursementStep;
import com.celcoin.disbursement.model.utils.BatchStatus;
import com.celcoin.disbursement.model.utils.ScheduleType;
import com.celcoin.disbursement.model.utils.StepStatus;
import com.celcoin.disbursement.repository.DisbursementBatchRepository;
import com.celcoin.disbursement.repository.DisbursementStepRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DisbursementNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(DisbursementNotificationService.class);
    private static final String GROUP_ID = "disbursement-processor";

    @Autowired
    private DisbursementStepRepository stepRepository;

    @Autowired
    private DisbursementBatchRepository batchRepository;

    @Autowired
    private IdempotencyService idempotencyService;

    @Transactional
    public void processPixResponse(ExternalRequestResponse response) {
        DisbursementStep step = stepRepository.findByExternalId(response.externalId())
                .orElseThrow(() -> new DisbursementProcessingException("400", "Erro ao recuperar informações de desembolso para o id externo: " + response.externalId()));

        if (idempotencyService.isDuplicate(response.externalId(), GROUP_ID)) {
            return; // Se for duplicado, encerra o processamento imediatamente.
        }

        step.setStatus(response.status());
        step.setUpdatedAt(LocalDateTime.now());

        if (response.status().equals(StepStatus.FAILED)) {
            step.setFailureReason(response.failureReason());
        }

        stepRepository.saveAndFlush(step);

        checkBatchCompletion(step.getBatch());
    }

    @Transactional
    public void processTedUpdate(ExternalRequestResponse response) {
        DisbursementStep step = stepRepository.findByExternalId(response.externalId())
                .orElseThrow(() -> new DisbursementProcessingException("400", "Erro ao recuperar informações de desembolso para o id externo: " + response.externalId()));

        if (step.getStatus() == StepStatus.SUCCESS || step.getStatus() == StepStatus.FAILED) {
            return; // guarantee that we will not process a response that already has been processed
        }

        step.setStatus(response.status());
        step.setUpdatedAt(LocalDateTime.now());

        if (response.status().equals(StepStatus.FAILED)) {
            step.setFailureReason(response.failureReason());
        }

        stepRepository.saveAndFlush(step);

        checkBatchCompletion(step.getBatch());
    }

    private void checkBatchCompletion(DisbursementBatch batch) {
        if (batch.getScheduleType() == ScheduleType.RECURRENT) {
            return;
        }

        long totalSteps = batchRepository.countSteps(batch.getId());
        long successfulSteps = batchRepository.countStepsByStatus(batch.getId(), StepStatus.SUCCESS);
        long failedSteps = batchRepository.countStepsByStatus(batch.getId(), StepStatus.FAILED);

        boolean statusHasChanged = false;
        BatchStatus newStatus = batch.getStatus();

        if (totalSteps == successfulSteps) {
            newStatus = BatchStatus.EXECUTED_COMPLETELY;
            statusHasChanged = true;
        } else if (totalSteps == failedSteps) {
            newStatus = BatchStatus.FAILED;
            statusHasChanged = true;
        } else if (totalSteps == successfulSteps + failedSteps) {
            newStatus = BatchStatus.PARTIALLY_EXECUTED;
            statusHasChanged = true;
        }

        if (statusHasChanged) {
            batch.setStatus(newStatus);
            batchRepository.saveAndFlush(batch);
            logger.info("Status do lote {} atualizado para {}.", batch.getId(), newStatus);
        }
    }
}
