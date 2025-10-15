package com.celcoin.disbursement.controller;

import com.celcoin.disbursement.exception.UnexpectedException;
import com.celcoin.disbursement.gateway.EventPublisher;
import com.celcoin.disbursement.model.dto.ExternalRequestResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/messaging")
public class MessagingController {

    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public MessagingController(EventPublisher eventPublisher, ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/send/{topic}")
    public ResponseEntity<String> postMessage(
            @PathVariable("topic") String topic,
            @RequestBody String messagePayload) {
        
        try {

            ExternalRequestResponse jsonPayload = objectMapper.readValue(messagePayload, ExternalRequestResponse.class);
            eventPublisher.publish(topic, jsonPayload);
            return ResponseEntity.ok("Mensagem enviada com sucesso para o tópico: " + topic);
        } catch (JsonProcessingException e) {
            throw new UnexpectedException("Payload da mensagem não é um JSON válido: " + e.getMessage());
        }
    }
}
