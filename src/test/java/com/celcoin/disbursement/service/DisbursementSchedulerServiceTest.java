package com.celcoin.disbursement.service;

import com.celcoin.disbursement.gateway.EventPublisher;
import com.celcoin.disbursement.model.entity.DisbursementBatch;
import com.celcoin.disbursement.model.entity.DisbursementStep;
import com.celcoin.disbursement.model.event.DisbursementRequestEvent;
import com.celcoin.disbursement.model.utils.BatchStatus;
import com.celcoin.disbursement.model.utils.Recurrency;
import com.celcoin.disbursement.model.utils.ScheduleType;
import com.celcoin.disbursement.model.utils.StepType;
import com.celcoin.disbursement.repository.DisbursementBatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisbursementSchedulerServiceTest {

    @Mock
    private DisbursementBatchRepository batchRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private Clock clock;

    @InjectMocks
    private DisbursementSchedulerService schedulerService;

    private DisbursementBatch scheduledBatch;
    private DisbursementBatch recurringMonthlyBatch;

    @BeforeEach
    void setUp() {
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        DisbursementStep scheduledStep = new DisbursementStep();
        scheduledStep.setType(StepType.TED);
        scheduledBatch = new DisbursementBatch();
        scheduledBatch.setId("batch-scheduled-01");
        scheduledBatch.setSteps(List.of(scheduledStep));

        DisbursementStep recurringStep = new DisbursementStep();
        recurringStep.setType(StepType.PIX);
        recurringMonthlyBatch = new DisbursementBatch();
        recurringMonthlyBatch.setId("batch-recurring-01");
        recurringMonthlyBatch.setRecurrency(Recurrency.MONTHLY);
        recurringMonthlyBatch.setScheduleDate(LocalDateTime.of(2025, 10, 15, 10, 0));
        recurringMonthlyBatch.setSteps(List.of(recurringStep));
    }

    private void setFixedTime(LocalDateTime dateTime) {
        when(clock.instant()).thenReturn(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    @Test
    @DisplayName("Deve processar um lote agendado cuja data de execução já passou")
    void triggerEligibleBatches_shouldProcessDueScheduledBatch() {
        // Arrange
        setFixedTime(LocalDateTime.of(2025, 10, 15, 17, 0, 0));

        when(batchRepository.findByStatusAndScheduleTypeAndScheduleDateLessThanEqual(
                eq(BatchStatus.NOT_EXECUTED), eq(ScheduleType.SCHEDULED), any(LocalDateTime.class)))
                .thenReturn(List.of(scheduledBatch));
        when(batchRepository.findByStatusAndScheduleType(any(), any())).thenReturn(Collections.emptyList());

        // Act
        schedulerService.triggerEligibleBatches();

        // Assert
        ArgumentCaptor<DisbursementBatch> batchCaptor = ArgumentCaptor.forClass(DisbursementBatch.class);
        verify(batchRepository).save(batchCaptor.capture());

        // Verifica o estado do objeto capturado
        DisbursementBatch capturedBatch = batchCaptor.getValue();
        assertThat(capturedBatch.getStatus()).isEqualTo(BatchStatus.PROCESSING);

        // Verifica se o evento foi publicado
        verify(eventPublisher).publish(eq(com.celcoin.disbursement.config.KafkaTopicConfig.TED_REQUEST_TOPIC), any(DisbursementRequestEvent.class));
    }

    @Test
    @DisplayName("Deve disparar um ciclo recorrente se for o dia correto e não for duplicado")
    void triggerEligibleBatches_shouldProcessRecurringBatch_whenOnDateAndNotDuplicate() {
        // Arrange
        setFixedTime(LocalDateTime.of(2025, 10, 15, 11, 0, 0));

        when(batchRepository.findByStatusAndScheduleTypeAndScheduleDateLessThanEqual(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(batchRepository.findByStatusAndScheduleType(BatchStatus.RECURRENT, ScheduleType.RECURRENT))
                .thenReturn(List.of(recurringMonthlyBatch));

        String expectedIdempotencyKey = "batch-recurring-01_2025_10";
        when(idempotencyService.isDuplicate(expectedIdempotencyKey, "RECURRENT_SCHEDULER")).thenReturn(false);

        // Act
        schedulerService.triggerEligibleBatches();

        // Assert
        verify(eventPublisher).publish(eq(com.celcoin.disbursement.config.KafkaTopicConfig.PIX_REQUEST_TOPIC), any(DisbursementRequestEvent.class));
        verify(batchRepository, never()).save(recurringMonthlyBatch);
    }

    @Test
    @DisplayName("NÃO deve disparar um ciclo recorrente se for duplicado (já processado)")
    void triggerEligibleBatches_shouldNotProcessRecurringBatch_whenIsDuplicate() {
        // Arrange
        setFixedTime(LocalDateTime.of(2025, 10, 15, 11, 0, 0));

        when(batchRepository.findByStatusAndScheduleTypeAndScheduleDateLessThanEqual(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(batchRepository.findByStatusAndScheduleType(BatchStatus.RECURRENT, ScheduleType.RECURRENT))
                .thenReturn(List.of(recurringMonthlyBatch));

        String expectedIdempotencyKey = "batch-recurring-01_2025_10";
        when(idempotencyService.isDuplicate(expectedIdempotencyKey, "RECURRENT_SCHEDULER")).thenReturn(true);

        // Act
        schedulerService.triggerEligibleBatches();

        // Assert
        verify(eventPublisher, never()).publish(anyString(), any());
    }

    @Test
    @DisplayName("NÃO deve disparar um ciclo recorrente se ainda não for o dia correto")
    void triggerEligibleBatches_shouldNotProcessRecurringBatch_whenNotOnDate() {
        // Arrange
        setFixedTime(LocalDateTime.of(2025, 10, 14, 11, 0, 0));

        when(batchRepository.findByStatusAndScheduleTypeAndScheduleDateLessThanEqual(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(batchRepository.findByStatusAndScheduleType(BatchStatus.RECURRENT, ScheduleType.RECURRENT))
                .thenReturn(List.of(recurringMonthlyBatch));

        // Act
        schedulerService.triggerEligibleBatches();

        // Assert
        verify(idempotencyService, never()).isDuplicate(anyString(), anyString());
        verify(eventPublisher, never()).publish(anyString(), any());
    }
}
