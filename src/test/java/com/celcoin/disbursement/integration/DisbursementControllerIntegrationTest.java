package com.celcoin.disbursement.integration;

import com.celcoin.disbursement.config.KafkaTopicConfig;
import com.celcoin.disbursement.exception.ResourceNotFoundException;
import com.celcoin.disbursement.model.dto.DisbursementRequest;
import com.celcoin.disbursement.model.entity.DisbursementBatch;
import com.celcoin.disbursement.model.utils.BatchStatus;
import com.celcoin.disbursement.model.utils.ScheduleType;
import com.celcoin.disbursement.model.utils.StepType;
import com.celcoin.disbursement.repository.DisbursementBatchRepository;
import com.celcoin.disbursement.repository.DisbursementStepRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.servlet.ServletException;
import jakarta.transaction.Transactional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class DisbursementControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DisbursementBatchRepository batchRepository;

    @Autowired
    private DisbursementStepRepository stepRepository;

    @Autowired
    private EntityManager entityManager;

    private KafkaMessageListenerContainer<String, String> testConsumerContainer;
    private BlockingQueue<ConsumerRecord<String, String>> consumerRecords;

    @BeforeEach
    void setUp() {
        stepRepository.deleteAll();
        batchRepository.deleteAll();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                kafkaContainer.getBootstrapServers(),
                "test-controller-group-" + UUID.randomUUID(),
                "true"
        );
        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        ContainerProperties containerProperties = new ContainerProperties(KafkaTopicConfig.PIX_REQUEST_TOPIC, KafkaTopicConfig.TED_REQUEST_TOPIC);
        testConsumerContainer = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        consumerRecords = new LinkedBlockingQueue<>();
        testConsumerContainer.setupMessageListener((MessageListener<String, String>) consumerRecords::add);
        testConsumerContainer.start();
    }

    @AfterEach
    void tearDown() {
        testConsumerContainer.stop();
    }

    @Test
    @DisplayName("POST /disbursements - Deve criar um desembolso IMEDIATO, persistir e publicar evento")
    @WithMockUser
    @Transactional
    void createImmediateDisbursement_shouldPersistAndPublishEvent() throws Exception {
        // Arrange
        String clientCode = "immediate-client-" + UUID.randomUUID();
        DisbursementRequest request = createDisbursementRequest(clientCode, ScheduleType.IMMEDIATE, null, StepType.PIX);

        // Act
        mockMvc.perform(post("/disbursements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());


        entityManager.clear();

        // Assert (Banco de Dados)
        Optional<DisbursementBatch> batchOpt = batchRepository.findByClientCode(clientCode);
        assertThat(batchOpt).isPresent();
        DisbursementBatch batch = batchOpt.get();
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.PROCESSING);
        assertThat(batch.getSteps()).hasSize(1);

        // Assert (Kafka)
        ConsumerRecord<String, String> received = consumerRecords.poll(10, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.topic()).isEqualTo(KafkaTopicConfig.PIX_REQUEST_TOPIC);
        assertThat(received.value()).contains(batch.getSteps().get(0).getId());
    }

    @Test
    @DisplayName("POST /disbursements - Deve criar um desembolso AGENDADO e NÃO publicar evento")
    @WithMockUser
    @Transactional
    void createScheduledDisbursement_shouldPersistButNotPublish() throws Exception {
        // Arrange
        String clientCode = "scheduled-client-" + UUID.randomUUID();
        LocalDateTime scheduleDate = LocalDateTime.of(2025, 12, 25, 10, 0, 0);
        DisbursementRequest request = createDisbursementRequest(clientCode, ScheduleType.SCHEDULED, scheduleDate, StepType.TED);

        // Act
        mockMvc.perform(post("/disbursements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_EXECUTED"));

        // Assert (Banco de Dados)
        Optional<DisbursementBatch> batchOpt = batchRepository.findByClientCode(clientCode);
        assertThat(batchOpt).isPresent();
        assertThat(batchOpt.get().getStatus()).isEqualTo(BatchStatus.NOT_EXECUTED);
        assertThat(batchOpt.get().getScheduleDate()).isEqualTo(scheduleDate);

        // Assert (Kafka)
        ConsumerRecord<String, String> received = consumerRecords.poll(2, TimeUnit.SECONDS);
        assertThat(received).isNull();
    }

    @Test
    @DisplayName("POST /disbursements - Deve retornar 400 Conflict ao usar um clientCode duplicado")
    @WithMockUser
    void createDisbursement_withDuplicateClientCode_shouldReturnConflict() throws Exception {
        // Arrange
        String clientCode = "duplicate-client-" + UUID.randomUUID();
        DisbursementBatch existingBatch = DisbursementBatch.builder()
                .id(UUID.randomUUID().toString())
                .clientCode(clientCode)
                .status(BatchStatus.PROCESSING)
                .createdAt(LocalDateTime.now())
                .build();
        batchRepository.save(existingBatch);

        DisbursementRequest request = createDisbursementRequest(clientCode, ScheduleType.IMMEDIATE, null, StepType.PIX);

        // Act & Assert
        mockMvc.perform(post("/disbursements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(400));
    }

    @Test
    @DisplayName("GET /disbursements/{clientCode}/status - Deve retornar o status do lote quando o clientCode existe")
    @WithMockUser
    void getStatus_whenClientCodeExists_shouldReturnStatus() throws Exception {
        // Arrange
        String clientCode = "status-client-" + UUID.randomUUID();
        DisbursementBatch batch = DisbursementBatch.builder()
                .id(UUID.randomUUID().toString())
                .clientCode(clientCode)
                .status(BatchStatus.PROCESSING)
                .createdAt(LocalDateTime.now())
                .build();
        batchRepository.save(batch);

        // Act & Assert
        mockMvc.perform(get("/disbursements/{clientCode}/status", clientCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").value(batch.getId()))
                .andExpect(jsonPath("$.clientCode").value(clientCode))
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    @DisplayName("GET /disbursements/{clientCode}/status - Deve lançar ResourceNotFoundException quando o clientCode não existe")
    @WithMockUser
    void getStatus_whenClientCodeDoesNotExist_shouldThrowException() {
        // Arrange
        String nonExistingCode = "non-existing-code";

        // Act & Assert
        Exception thrown = assertThrows(ServletException.class, () -> {
            mockMvc.perform(get("/disbursements/{clientCode}/status", nonExistingCode));
        });

        Throwable rootCause = thrown.getCause();
        assertInstanceOf(ResourceNotFoundException.class, rootCause, "A causa raiz da exceção deveria ser ResourceNotFoundException");
        assertThat(rootCause.getMessage()).contains("Lote de desembolso não encontrado com o clientCode: " + nonExistingCode);
    }
}
