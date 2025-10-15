package com.celcoin.disbursement.adapter;

import com.celcoin.disbursement.model.entity.DisbursementStep;
import com.celcoin.disbursement.repository.DisbursementStepRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class TedAdapter implements DisbursementChannelAdapter {
    private static final Logger logger = LoggerFactory.getLogger(TedAdapter.class);

    private final RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DisbursementStepRepository stepRepository;

    public TedAdapter() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void send(DisbursementStep step) {
        //transform the payload into the payload required by the client
        //TedRequest tedRequest = objectMapper.convertValue(payload, TedRequest.class);

        logger.info("Sending TED request for client {}", step.getBatch().getClientCode());
        //restTemplate.postForEntity("") Here we would fetch client information from DB and use restTemplate
        // or webClient to make the http call.
        step.setExternalId(UUID.randomUUID().toString());
        step.setUpdatedAt(LocalDateTime.now());
        stepRepository.saveAndFlush(step);
    }
}
