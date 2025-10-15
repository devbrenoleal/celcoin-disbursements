package com.celcoin.disbursement.service;

import com.celcoin.disbursement.exception.DisbursementProcessingException;
import com.celcoin.disbursement.exception.UnexpectedException;
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

    @Autowired
    private DisbursementStepRepository stepRepository;

    @Autowired
    private DisbursementBatchRepository batchRepository;

    @Transactional
    public void processPixResponse(ExternalRequestResponse response) {
        DisbursementStep step = stepRepository.findByExternalId(response.externalId())
                .orElseThrow(() -> new DisbursementProcessingException("400", "Erro ao recuperar informações de desembolso para o id externo: " + response.externalId()));

        if(step.getStatus().equals(StepStatus.FAILED) || step.getStatus().equals(StepStatus.SUCCESS)) {
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

        stepRepository.save(step);

        checkBatchCompletion(step.getBatch());
    }

    private void checkBatchCompletion(DisbursementBatch batch) {
        // Não faz sentido verificar lotes recorrentes, pois eles nunca "terminam"
        if (batch.getScheduleType() == ScheduleType.RECURRENT) {
            return;
        }

        // Recarrega o lote para garantir que temos todos os steps
        DisbursementBatch freshBatch = batchRepository.findById(batch.getId())
                .orElseThrow(() -> new UnexpectedException("Lote não encontrado de forma inesperada para o id " + batch.getId()));

        long totalSteps = freshBatch.getSteps().size();
        long successfulSteps = freshBatch.getSteps().stream().filter(s -> s.getStatus() == StepStatus.SUCCESS).count();
        long failedSteps = freshBatch.getSteps().stream().filter(s -> s.getStatus() == StepStatus.FAILED).count();

        if (totalSteps == successfulSteps) {
            batch.setStatus(BatchStatus.EXECUTED_COMPLETELY);
            logger.info("Lote {} concluído com sucesso.", batch.getId());
        } else if(totalSteps == failedSteps) {
            batch.setStatus(BatchStatus.FAILED);
        } else if (totalSteps == successfulSteps + failedSteps) {
            batch.setStatus(BatchStatus.PARTIALLY_EXECUTED);
            logger.warn("Lote {} concluído com erros.", batch.getId());
        }

        batchRepository.save(batch);
    }
}
