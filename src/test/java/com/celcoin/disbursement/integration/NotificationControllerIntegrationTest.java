package com.celcoin.disbursement.integration;


import com.celcoin.disbursement.model.dto.ExternalRequestResponse;
import com.celcoin.disbursement.model.entity.DisbursementBatch;
import com.celcoin.disbursement.model.entity.DisbursementStep;
import com.celcoin.disbursement.model.entity.ProcessedEvent;
import com.celcoin.disbursement.model.utils.BatchStatus;
import com.celcoin.disbursement.model.utils.ScheduleType;
import com.celcoin.disbursement.model.utils.StepStatus;
import com.celcoin.disbursement.model.utils.StepType;
import com.celcoin.disbursement.repository.DisbursementBatchRepository;
import com.celcoin.disbursement.repository.DisbursementStepRepository;
import com.celcoin.disbursement.repository.ProcessedEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class NotificationControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DisbursementBatchRepository batchRepository;

    @Autowired
    private DisbursementStepRepository stepRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        stepRepository.deleteAll();
        batchRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /notifications/pix - Deve atualizar o Step para SUCCESS e o Batch para EXECUTED_COMPLETELY")
    @WithMockUser
    @Transactional
    void handlePixNotification_onSuccess_shouldUpdateStepAndBatch() throws Exception {
        // Arrange
        String externalId = UUID.randomUUID().toString();
        DisbursementBatch batch = createAndSaveTestBatch(BatchStatus.PROCESSING);
        DisbursementStep step = createAndSaveTestStep(batch, StepStatus.PROCESSING, externalId);

        ExternalRequestResponse notificationPayload = new ExternalRequestResponse(null, externalId, StepStatus.SUCCESS, null);

        // Act
        mockMvc.perform(post("/notifications/pix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notificationPayload)))
                .andExpect(status().isOk());

        // Assert
        entityManager.clear();

        stepRepository.flush();
        batchRepository.flush();

        DisbursementStep updatedStep = stepRepository.findById(step.getId()).orElseThrow();
        assertThat(updatedStep.getStatus()).isEqualTo(StepStatus.SUCCESS);

        DisbursementBatch updatedBatch = batchRepository.findById(batch.getId()).orElseThrow();
        assertThat(updatedBatch.getStatus()).isEqualTo(BatchStatus.EXECUTED_COMPLETELY);
    }

    @Test
    @DisplayName("POST /notifications/pix - Deve atualizar o Step para FAILED e o Batch para PARTIALLY_EXECUTED")
    @WithMockUser
    @Transactional
    void handlePixNotification_onFailure_shouldUpdateStepAndBatchToPartial() throws Exception {
        // Arrange
        String externalIdSuccess = UUID.randomUUID().toString();
        String externalIdFailed = UUID.randomUUID().toString();
        DisbursementBatch batch = createAndSaveTestBatch(BatchStatus.PROCESSING);
        createAndSaveTestStep(batch, StepStatus.SUCCESS, externalIdSuccess);
        DisbursementStep stepToFail = createAndSaveTestStep(batch, StepStatus.PROCESSING, externalIdFailed);

        String failureReason = "Saldo insuficiente";
        ExternalRequestResponse notificationPayload = new ExternalRequestResponse(null, externalIdFailed, StepStatus.FAILED, failureReason);

        // Act
        mockMvc.perform(post("/notifications/pix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notificationPayload)))
                .andExpect(status().isOk());

        // Assert
        entityManager.clear();

        DisbursementStep updatedStep = stepRepository.findById(stepToFail.getId()).orElseThrow();
        assertThat(updatedStep.getStatus()).isEqualTo(StepStatus.FAILED);
        assertThat(updatedStep.getFailureReason()).isEqualTo(failureReason);

        DisbursementBatch updatedBatch = batchRepository.findById(batch.getId()).orElseThrow();
        assertThat(updatedBatch.getStatus()).isEqualTo(BatchStatus.PARTIALLY_EXECUTED);
    }

    @Test
    @DisplayName("Idempotência - Deve processar a primeira notificação e ignorar a segunda")
    @WithMockUser
    void handlePixNotification_shouldProcessFirstTimeAndIgnoreDuplicate() throws Exception {
        // Arrange
        String externalId = UUID.randomUUID().toString();
        DisbursementBatch batch = createAndSaveTestBatch(BatchStatus.PROCESSING);
        createAndSaveTestStep(batch, StepStatus.PROCESSING, externalId);

        ExternalRequestResponse notificationPayload = new ExternalRequestResponse(null, externalId, StepStatus.SUCCESS, null);

        //Primeira Chamada
        mockMvc.perform(post("/notifications/pix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notificationPayload)))
                .andExpect(status().isOk());

        //Verifica se a primeira chamada funcionou
        entityManager.clear();
        DisbursementStep updatedStep = stepRepository.findByExternalId(externalId).orElseThrow();
        assertThat(updatedStep.getStatus()).isEqualTo(StepStatus.SUCCESS);

        // Verifica se o registro de idempotência foi criado
        assertThat(processedEventRepository.count()).isEqualTo(1);
        ProcessedEvent event = processedEventRepository.findById(externalId).orElseThrow();
        assertThat(event.getEventId()).isEqualTo(externalId);

        LocalDateTime firstUpdateTimestamp = updatedStep.getUpdatedAt();

        //Segunda Chamada (Duplicada)
        mockMvc.perform(post("/notifications/pix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notificationPayload)))
                .andExpect(status().isOk());

        //Verifica se a segunda chamada foi ignorada
        entityManager.clear();
        DisbursementStep reloadedStep = stepRepository.findByExternalId(externalId).orElseThrow();
        assertThat(reloadedStep.getStatus()).isEqualTo(StepStatus.SUCCESS);
        assertThat(reloadedStep.getUpdatedAt()).isEqualTo(firstUpdateTimestamp);
        assertThat(processedEventRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /notifications/pix - Deve retornar 400 se o externalId não for encontrado")
    @WithMockUser
    void handlePixNotification_shouldReturnBadRequest_whenExternalIdNotFound() throws Exception {
        // Arrange
        String nonExistingExternalId = "ext-pix-not-found";
        ExternalRequestResponse notificationPayload = new ExternalRequestResponse(null, nonExistingExternalId, StepStatus.SUCCESS, null);

        mockMvc.perform(post("/notifications/pix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notificationPayload)))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.errorCode").value("400"))
                .andExpect(jsonPath("$.message").value("Erro ao recuperar informações de desembolso para o id externo: " + nonExistingExternalId));
    }


    private DisbursementBatch createAndSaveTestBatch(BatchStatus status) {
        DisbursementBatch batch = DisbursementBatch.builder()
                .id(UUID.randomUUID().toString())
                .clientCode(UUID.randomUUID().toString())
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
        return batchRepository.saveAndFlush(batch);
    }

    private DisbursementStep createAndSaveTestStep(DisbursementBatch batch, StepStatus status, String externalId) throws JsonProcessingException {
        DisbursementStep step = DisbursementStep.builder()
                .id(UUID.randomUUID().toString())
                .batch(batch)
                .status(status)
                .externalId(externalId)
                .type(StepType.PIX)
                .payload(objectMapper.writeValueAsString(createDisbursementRequest(
                        UUID.randomUUID().toString(),
                        ScheduleType.IMMEDIATE,
                        LocalDateTime.now(),
                        StepType.PIX
                ).disbursements().getFirst().disbursementStep()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return stepRepository.saveAndFlush(step);
    }
}
