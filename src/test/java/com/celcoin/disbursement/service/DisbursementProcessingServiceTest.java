package com.celcoin.disbursement.service;

import com.celcoin.disbursement.exception.ResourceNotFoundException;
import com.celcoin.disbursement.model.entity.DisbursementStep;
import com.celcoin.disbursement.model.utils.StepStatus;
import com.celcoin.disbursement.repository.DisbursementStepRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisbursementProcessingServiceTest {

    @Mock
    private DisbursementStepRepository stepRepository;

    @Mock
    private DisbursementOrchestrator orchestrator;

    @InjectMocks
    private DisbursementProcessingService processingService;

    @Test
    void execute_whenStepIsPending_shouldChangeStatusAndCallOrchestrator() {
        // Arrange
        String stepId = UUID.randomUUID().toString();
        DisbursementStep pendingStep = new DisbursementStep();
        pendingStep.setId(stepId);
        pendingStep.setStatus(StepStatus.PENDING);

        when(stepRepository.findById(stepId)).thenReturn(Optional.of(pendingStep));

        // Act
        processingService.execute(stepId);

        // Assert
        verify(stepRepository).save(pendingStep);
        verify(orchestrator).process(pendingStep);
        assert pendingStep.getStatus() == StepStatus.PROCESSING;
    }

    @Test
    void execute_whenStepIsAlreadyProcessed_shouldDoNothing() {
        // Arrange
        String stepId = UUID.randomUUID().toString();
        DisbursementStep successStep = new DisbursementStep();
        successStep.setId(stepId);
        successStep.setStatus(StepStatus.SUCCESS);

        when(stepRepository.findById(stepId)).thenReturn(Optional.of(successStep));

        // Act
        processingService.execute(stepId);

        // Assert
        // Verifica que save() e process() NUNCA foram chamados
        verify(stepRepository, never()).save(any());
        verify(orchestrator, never()).process(any());
    }

    @Test
    void execute_whenStepNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        String stepId = UUID.randomUUID().toString();
        when(stepRepository.findById(stepId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            processingService.execute(stepId);
        });
    }
}
