package com.celcoin.disbursement.service;

import com.celcoin.disbursement.model.entity.ProcessedEvent;
import com.celcoin.disbursement.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class IdempotencyService {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyService.class);
    private final ProcessedEventRepository eventRepository;

    public IdempotencyService(ProcessedEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Verifica se um evento já foi processado. Se não, marca como processado.
     * @param eventId O ID único do evento.
     * @param consumerGroup O grupo de consumidores processando.
     * @return {@code true} se o evento já foi processado (duplicado), {@code false} caso contrário.
     */
    public boolean isDuplicate(String eventId, String consumerGroup) {
        Optional<ProcessedEvent> processedEvent = eventRepository.findById(eventId);

        if(processedEvent.isPresent()) {
            logger.warn("Evento duplicado detectado para eventId [{}]. Ignorando.", eventId);
            return true;
        }

        ProcessedEvent eventRecord = ProcessedEvent.builder()
                .eventId(eventId)
                .consumerGroup(consumerGroup)
                .processedAt(LocalDateTime.now())
                .build();
        eventRepository.saveAndFlush(eventRecord);
        return false;
    }
}
