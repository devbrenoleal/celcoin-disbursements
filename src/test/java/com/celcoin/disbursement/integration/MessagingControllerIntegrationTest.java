package com.celcoin.disbursement.integration;


import com.celcoin.disbursement.model.dto.ExternalRequestResponse;
import com.celcoin.disbursement.model.utils.StepStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MessagingControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private KafkaMessageListenerContainer<String, String> testConsumerContainer;
    private BlockingQueue<ConsumerRecord<String, String>> consumerRecords;
    
    private static final String TEST_TOPIC = "external-responses-topic";


    @BeforeAll
    void initializeKafkaConsumer() {
        consumerRecords = new LinkedBlockingQueue<>();
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                kafkaContainer.getBootstrapServers(),
                "messaging-integration-test-group",
                "true" // auto-commit
        );

        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        ContainerProperties containerProperties = new ContainerProperties(TEST_TOPIC);
        testConsumerContainer = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        testConsumerContainer.setupMessageListener((MessageListener<String, String>) consumerRecords::add);
        testConsumerContainer.start();
    }

    @AfterAll
    void stopKafkaConsumer() {
        testConsumerContainer.stop();
    }

    @BeforeEach
    void setUp() {
        consumerRecords.clear();
    }

    @AfterEach
    void tearDown() {
        testConsumerContainer.stop();
    }

    @Test
    @DisplayName("POST /messaging/send/{topic} - Deve publicar mensagem no Kafka com payload válido")
    @WithMockUser
    void postMessage_withValidPayload_shouldPublishToKafka() throws Exception {
        // Arrange
        ExternalRequestResponse payload = new ExternalRequestResponse("client-req-123", "external-tx-456", StepStatus.SUCCESS, null);
        String jsonPayload = objectMapper.writeValueAsString(payload);

        // Act
        mockMvc.perform(post("/messaging/send/{topic}", TEST_TOPIC)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(content().string("Mensagem enviada com sucesso para o tópico: " + TEST_TOPIC));

        // Assert
        ConsumerRecord<String, String> received = consumerRecords.poll(10, TimeUnit.SECONDS);
        assertThat(received).isNotNull(); // Agora não deve ser nulo
        assertThat(received.topic()).isEqualTo(TEST_TOPIC);

        ExternalRequestResponse receivedPayload = objectMapper.readValue(received.value(), ExternalRequestResponse.class);
        assertThat(receivedPayload).isEqualTo(payload);
    }

    @Test
    @DisplayName("POST /messaging/send/{topic} - Deve retornar 400 com payload de JSON inválido")
    @WithMockUser
    void postMessage_withInvalidJson_shouldReturnBadRequest() throws Exception {
        // Arrange
        String invalidJsonPayload = "{ \"clientRequestId\": \"123\", \"status\": \"SUCCESS\"";

        // Act & Assert
        mockMvc.perform(post("/messaging/send/{topic}", TEST_TOPIC)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJsonPayload))
                .andExpect(status().is(400));

        // Assert (Kafka)
        ConsumerRecord<String, String> received = consumerRecords.poll(500, TimeUnit.MILLISECONDS);
        assertThat(received).isNull();
    }
}
