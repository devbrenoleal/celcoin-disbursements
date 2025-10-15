package com.celcoin.disbursement.service;

import com.celcoin.disbursement.gateway.EventPublisher;
import com.celcoin.disbursement.model.entity.DisbursementBatch;
import com.celcoin.disbursement.model.entity.DisbursementStep;
import com.celcoin.disbursement.model.event.DisbursementRequestEvent;
import com.celcoin.disbursement.model.utils.BatchStatus;
import com.celcoin.disbursement.model.utils.ScheduleType;
import com.celcoin.disbursement.model.utils.StepType;
import com.celcoin.disbursement.repository.DisbursementBatchRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.List;

@Service
public class DisbursementSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(DisbursementSchedulerService.class);

    private final DisbursementBatchRepository batchRepository;
    private final EventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;

    public DisbursementSchedulerService(DisbursementBatchRepository batchRepository, EventPublisher eventPublisher, IdempotencyService idempotencyService) {
        this.batchRepository = batchRepository;
        this.eventPublisher = eventPublisher;
        this.idempotencyService = idempotencyService;
    }

    @Scheduled(fixedRate = 60000) // Roda a cada minuto
    @Transactional
    public void triggerEligibleBatches() {
        LocalDateTime now = LocalDateTime.now();
        logger.info("Scheduler iniciado em {}: procurando por lotes elegíveis", now);

        List<DisbursementBatch> scheduledBatches = batchRepository
                .findByStatusAndScheduleTypeAndScheduleDateLessThanEqual(
                        BatchStatus.NOT_EXECUTED,
                        ScheduleType.SCHEDULED,
                        now
                );
        scheduledBatches.forEach(this::processScheduledBatch);

        List<DisbursementBatch> recurringBatches = batchRepository
                .findByStatusAndScheduleType(BatchStatus.RECURRENT, ScheduleType.RECURRENT);
        recurringBatches.forEach(batch -> processRecurringBatch(batch, now));
    }

    private void processScheduledBatch(DisbursementBatch batch) {
        logger.info("Processando lote agendado ID: {}", batch.getId());
        batch.setStatus(BatchStatus.PROCESSING);
        batchRepository.save(batch);
        publishStepEvents(batch);
    }

    private void processRecurringBatch(DisbursementBatch batch, LocalDateTime now) {
        LocalDateTime templateDateTime = batch.getScheduleDate();
        boolean isExecutionTime = now.toLocalTime().isAfter(templateDateTime.toLocalTime());

        if (!isExecutionTime) {
            return;
        }

        String idempotencyKey = null;
        boolean shouldRunToday = false;
        LocalDate currentDate = now.toLocalDate();

        switch (batch.getRecurrency()) {
            case DAILY:
                shouldRunToday = true;
                idempotencyKey = batch.getId() + "_" + currentDate; // Ex: batchId_2025-10-15
                break;

            case WEEKLY:
                shouldRunToday = currentDate.getDayOfWeek() == templateDateTime.getDayOfWeek();
                if (shouldRunToday) {
                    // Chave baseada no ano e no número da semana
                    idempotencyKey = String.format("%s_%d_W%02d",
                            batch.getId(),
                            currentDate.getYear(),
                            currentDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
                }
                break;

            case MONTHLY:
                shouldRunToday = currentDate.getDayOfMonth() == templateDateTime.getDayOfMonth();
                if (shouldRunToday) {
                    idempotencyKey = String.format("%s_%d_%02d",
                            batch.getId(),
                            currentDate.getYear(),
                            currentDate.getMonthValue());
                }
                break;

            case ANNUALLY:
                shouldRunToday = currentDate.getMonth() == templateDateTime.getMonth() &&
                        currentDate.getDayOfMonth() == templateDateTime.getDayOfMonth();
                if (shouldRunToday) {
                    idempotencyKey = String.format("%s_%d", batch.getId(), currentDate.getYear());
                }
                break;
        }

        if (shouldRunToday) {
            logger.debug("Verificando recorrência para lote {}, tipo {}, chave {}", batch.getId(), batch.getRecurrency(), idempotencyKey);

            if (!idempotencyService.isDuplicate(idempotencyKey, "RECURRENT_SCHEDULER")) {
                logger.info("Disparando ciclo recorrente para o lote ID: {}, Tipo: {}", batch.getId(), batch.getRecurrency());
                publishStepEvents(batch);
            }
        }
    }

    private void publishStepEvents(DisbursementBatch batch) {
        for (DisbursementStep step : batch.getSteps()) {
            DisbursementRequestEvent event = new DisbursementRequestEvent(step.getId());
            String topic = getTopicForChannel(step.getType());
            eventPublisher.publish(topic, event);
        }
    }

    private String getTopicForChannel(StepType type) {
        return switch (type) {
            case PIX -> com.celcoin.disbursement.config.KafkaTopicConfig.PIX_REQUEST_TOPIC;
            case TED -> com.celcoin.disbursement.config.KafkaTopicConfig.TED_REQUEST_TOPIC;
        };
    }
}
