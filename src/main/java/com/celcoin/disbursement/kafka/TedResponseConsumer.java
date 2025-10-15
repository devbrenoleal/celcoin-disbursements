package com.celcoin.disbursement.kafka;

import com.celcoin.disbursement.config.KafkaTopicConfig;
import com.celcoin.disbursement.exception.DisbursementProcessingException;
import com.celcoin.disbursement.model.dto.ExternalRequestResponse;
import com.celcoin.disbursement.service.DisbursementNotificationService;
import com.celcoin.disbursement.service.IdempotencyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TedResponseConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TedResponseConsumer.class);
    private static final String GROUP_ID = "disbursement-processor";

    @Autowired
    private DisbursementNotificationService notificationService;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private ObjectMapper objectMapper;

    @CustomKafkaListener(topics = KafkaTopicConfig.TED_RESPONSE_TOPIC, groupId = GROUP_ID)
    public void consume(ExternalRequestResponse response) {
        logger.info("Mensagem de resposta TED recebida.");

        if (idempotencyService.isDuplicate(response.externalId(), GROUP_ID)) {
            logger.error("Duplicate key found for externalId {}", response.externalId());
            return;
        }

        notificationService.processTedUpdate(response);
    }
}