package com.celcoin.disbursement.kafka;

import com.celcoin.disbursement.config.KafkaTopicConfig;
import com.celcoin.disbursement.model.event.DisbursementRequestEvent;
import com.celcoin.disbursement.service.DisbursementProcessingService;
import com.celcoin.disbursement.service.IdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;


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
                "MENSAGEM MOVIDA PARA O DEAD LETTER QUEUE (DLT)\n" +
                        "Tópico de Origem: {}\n" +
                        "Step ID com Falha: {}\n",
                topic, event.stepId()
        );
    }
}
