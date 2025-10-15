package com.celcoin.disbursement.adapter;

import com.celcoin.disbursement.exception.DisbursementProcessingException;
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

            logger.info("Enviando requisiçao PIX para clientCode {}", step.getBatch().getClientCode());
            //restTemplate.postForEntity("") Aqui faríamos um fetch dos dados do cliente do banco para fazer a requisição
            // poderíamos também usar webClient para fazer a chamada http

            // na resposta, acredito que obteríamos um externalId para guardar a transação
            // vou simular esse Id gerando ele de forma randômica
            step.setExternalId(UUID.randomUUID().toString());
            step.setUpdatedAt(LocalDateTime.now());
            stepRepository.saveAndFlush(step);
        } catch (JsonProcessingException e) {
            logger.error("Erro ao serializar paylaod {}", step.getPayload());
            throw new DisbursementProcessingException("VLP048", "Transação não concluída, cheque suas informações");
        }
    }
}
