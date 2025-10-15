package com.celcoin.disbursement.gateway;


import com.celcoin.disbursement.exception.UnexpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(String topic, Object payload) {
        logger.info("Publicando evento no tópico [{}]: {}", topic, payload);
        try {
            kafkaTemplate.send(topic, payload);
        } catch (Exception e) {
            logger.error("Erro ao publicar evento no tópico [{}]: {}", topic, payload, e);
            throw new UnexpectedException("Failed publishing event", e);
        }
    }
}