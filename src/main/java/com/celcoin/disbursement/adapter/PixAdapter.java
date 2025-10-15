package com.celcoin.disbursement.adapter;

import com.celcoin.disbursement.exception.DisbursementProcessingException;
import com.celcoin.disbursement.model.dto.DisbursementDto;
import com.celcoin.disbursement.model.dto.DisbursementResponse;
import com.celcoin.disbursement.model.dto.DisbursementStepRequest;
import com.celcoin.disbursement.model.dto.pix.PixRequest;
import com.celcoin.disbursement.model.entity.DisbursementStep;
import com.celcoin.disbursement.repository.DisbursementStepRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class PixAdapter implements DisbursementChannelAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(PixAdapter.class);

    private final RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DisbursementStepRepository stepRepository;

    public PixAdapter() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void send(DisbursementStep step) {
        try {
            DisbursementStepRequest stepRequest = objectMapper.readValue(step.getPayload(), DisbursementStepRequest.class);
            PixRequest pixRequest = new PixRequest(
                    stepRequest.amount(),
                    step.getBatch().getClientCode(),
                    stepRequest.creditParty(),
                    stepRequest.initiationType());

            logger.info("Sending pix request for client {}", step.getBatch().getClientCode());
            //restTemplate.postForEntity("") Here we would fetch client information from DB and use restTemplate
            // or webClient to make the http call.

            // after receiving a response with the ID of the external transaction, we store it to update the step later
            // I will simulate a response by just setting a random UUID to the external transaction
            step.setExternalId(UUID.randomUUID().toString());
            step.setUpdatedAt(LocalDateTime.now());
            stepRepository.saveAndFlush(step);
        } catch (JsonProcessingException e) {
            logger.error("Error while serializing the payload {}", step.getPayload());
            throw new DisbursementProcessingException("VLP048", "Transação não concluída, cheque suas informações");
        }
    }
}
