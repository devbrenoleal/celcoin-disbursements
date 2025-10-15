package com.celcoin.disbursement.controller;

import com.celcoin.disbursement.config.KafkaTopicConfig;
import com.celcoin.disbursement.exception.BusinessException;
import com.celcoin.disbursement.exception.ResourceNotFoundException;
import com.celcoin.disbursement.exception.UnexpectedException;
import com.celcoin.disbursement.gateway.EventPublisher;
import com.celcoin.disbursement.model.dto.DisbursementDto;
import com.celcoin.disbursement.model.dto.DisbursementRequest;
import com.celcoin.disbursement.model.dto.DisbursementResponse;
import com.celcoin.disbursement.model.dto.DisbursementStatusResponse;
import com.celcoin.disbursement.model.dto.StepStatusResponse;
import com.celcoin.disbursement.model.entity.DisbursementBatch;
import com.celcoin.disbursement.model.entity.DisbursementStep;
import com.celcoin.disbursement.model.event.DisbursementRequestEvent;
import com.celcoin.disbursement.model.utils.BatchStatus;
import com.celcoin.disbursement.model.utils.ScheduleType;
import com.celcoin.disbursement.model.utils.StepStatus;
import com.celcoin.disbursement.model.utils.StepType;
import com.celcoin.disbursement.repository.DisbursementBatchRepository;
import com.celcoin.disbursement.repository.DisbursementStepRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/disbursements")
public class DisbursementController {

    private static final Logger logger = LoggerFactory.getLogger(DisbursementController.class);

    @Autowired
    private DisbursementBatchRepository batchRepository;

    @Autowired
    private DisbursementStepRepository stepRepository;

    @Autowired
    private EventPublisher publisher;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping
    @Transactional
    public ResponseEntity<?> createDisbursement(@RequestBody DisbursementRequest request) {
        Optional<DisbursementBatch> existingBatch = batchRepository.findByClientCode(request.clientCode());

        if (existingBatch.isPresent()) {
            throw new BusinessException("409", "clientCode " + request.clientCode() + " já está no status de " +
                    existingBatch.get().getStatus());
        }
        ScheduleType scheduleType = request.schedule().type();
        BatchStatus initialStatus = switch (scheduleType) {
            case IMMEDIATE -> BatchStatus.PROCESSING; // Já começa em andamento
            case SCHEDULED -> BatchStatus.NOT_EXECUTED;
            case RECURRENT -> BatchStatus.RECURRENT;
        };

        DisbursementBatch batch = DisbursementBatch.builder()
                .id(UUID.randomUUID().toString())
                .clientCode(request.clientCode())
                .status(initialStatus)
                .scheduleType(request.schedule().type())
                .scheduleDate(request.schedule().date())
                .createdAt(LocalDateTime.now())
                .build();

        batchRepository.saveAndFlush(batch);

        List<DisbursementStep> steps = new LinkedList<>();

        for (DisbursementDto disbursement : request.disbursements()) {
            DisbursementStep step = DisbursementStep.builder()
                    .id(UUID.randomUUID().toString())
                    .batch(batch)
                    .type(disbursement.type())
                    .amount(disbursement.disbursementStep().amount())
                    .status(StepStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            try {
                step.setPayload(objectMapper.writeValueAsString(disbursement.disbursementStep()));
            } catch (JsonProcessingException e) {
                logger.error("Erro ao serializar payload do step {}", disbursement.disbursementStep());
                throw new UnexpectedException("Payload do step não é válido");
            }

            steps.add(stepRepository.saveAndFlush(step));
        }

        if (scheduleType == ScheduleType.IMMEDIATE) {
            logger.info("Disparando eventos para o lote imediato ID: {}", batch.getId());
            publishStepEvents(steps);
        } else {
            logger.info("Lote {} agendado para processamento futuro. Tipo: {}", batch.getId(), scheduleType);
        }

        return ResponseEntity.ok(new DisbursementResponse(batch.getId(), batch.getStatus().toString()));
    }

    @GetMapping("/{clientCode}/status")
    public ResponseEntity<DisbursementStatusResponse> getDisbursementStatus(@PathVariable String clientCode) {
        logger.info("Recebida requisição de status para o batch com clientCode: {}", clientCode);

        DisbursementBatch batch = batchRepository.findByClientCode(clientCode)
                .orElseThrow(() -> new ResourceNotFoundException("400", "Lote de desembolso não encontrado com o clientCode: " + clientCode));

        List<StepStatusResponse> stepStatuses = batch.getSteps().stream()
                .map(step -> new StepStatusResponse(
                        step.getId(),
                        step.getStatus(),
                        step.getExternalId()
                ))
                .collect(Collectors.toList());

        DisbursementStatusResponse response = new DisbursementStatusResponse(
                batch.getId(),
                batch.getStatus(),
                batch.getClientCode(),
                stepStatuses
        );

        return ResponseEntity.ok(response);
    }

    private void publishStepEvents(List<DisbursementStep> steps) {
        for (DisbursementStep step : steps) {
            String topic = getTopicForChannel(step.getType());
            DisbursementRequestEvent event = new DisbursementRequestEvent(step.getId());
            publisher.publish(topic, event);
        }
    }

    private String getTopicForChannel(StepType type) {
        return switch (type) {
            case PIX -> KafkaTopicConfig.PIX_REQUEST_TOPIC;
            case TED -> KafkaTopicConfig.TED_REQUEST_TOPIC;
        };
    }
}
