package com.celcoin.disbursement.kafka;

import com.celcoin.disbursement.config.KafkaTopicConfig;
import com.celcoin.disbursement.exception.DisbursementProcessingException;
import com.celcoin.disbursement.exception.ResourceNotFoundException;
import com.celcoin.disbursement.model.dto.ExternalRequestResponse;
import com.celcoin.disbursement.model.entity.DisbursementStep;
import com.celcoin.disbursement.model.entity.ProcessedEvent;
import com.celcoin.disbursement.model.event.DisbursementRequestEvent;
import com.celcoin.disbursement.model.utils.StepStatus;
import com.celcoin.disbursement.repository.DisbursementStepRepository;
import com.celcoin.disbursement.repository.ProcessedEventRepository;
import com.celcoin.disbursement.service.DisbursementNotificationService;
import com.celcoin.disbursement.service.DisbursementOrchestrator;
import com.celcoin.disbursement.service.DisbursementProcessingService;
import com.celcoin.disbursement.service.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DisbursementRequestConsumer {

    private static final Logger logger = LoggerFactory.getLogger(DisbursementRequestConsumer.class);
    private static final String GROUP_ID = "disbursement-processor";

    @Autowired
    private DisbursementProcessingService processingService;

    @Autowired
    private IdempotencyService idempotencyService;



    // Usando a anotação customizada e unificando os dois listeners
    @CustomKafkaListener(
            topics = {KafkaTopicConfig.PIX_REQUEST_TOPIC, KafkaTopicConfig.TED_REQUEST_TOPIC},
            groupId = GROUP_ID
    )
    public void consumeDisbursementRequest(DisbursementRequestEvent event) {
        logger.info("Evento de requisição de desembolso recebido para o stepId: {}", event.stepId());

        if (idempotencyService.isDuplicate(event.stepId(), GROUP_ID)) {
            return; // Se for duplicado, apenas encerra.
        }

        // Delega a lógica de negócio para um serviço dedicado
        processingService.execute(event.stepId());
    }

    @DltHandler
    public void handleDlt(DisbursementRequestEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        logger.error(
                "==> MENSAGEM MOVIDA PARA O DEAD LETTER QUEUE (DLT) <==\n" +
                        "Tópico de Origem: {}\n" +
                        "Step ID com Falha: {}\n" +
                        "Ação: Registrar para análise manual.\n",
                topic, event.stepId()
        );
    }
}
