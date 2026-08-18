package com.alikaracor.learning.flightservice.service;


import com.alikaracor.learning.flightservice.event.FlightEvent;
import com.alikaracor.learning.flightservice.model.OutboxEvent;
import com.alikaracor.learning.flightservice.model.OutboxStatus;
import com.alikaracor.learning.flightservice.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }


    @Transactional
    public void saveFlightEvent(FlightEvent flightEvent) {

        try {

            String payloadJson = objectMapper.writeValueAsString(flightEvent);

            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setOutboxId(UUID.randomUUID().toString());
            outboxEvent.setEventId(flightEvent.getEventId().toString());
            outboxEvent.setAggregateType("FLIGHT");
            outboxEvent.setAggregateId(flightEvent.getFlightId().toString());
            outboxEvent.setEventType(flightEvent.getEventType().name());
            outboxEvent.setTopicName("flight.events");
            outboxEvent.setPayload(payloadJson);
            outboxEvent.setStatus(OutboxStatus.PENDING);
            outboxEvent.setAttemptCount(0);
            outboxEvent.setCreatedAt(Instant.now());

            outboxEventRepository.save(outboxEvent);

        }
        catch (Exception e) {

            throw new RuntimeException("FlightEvent JSON serileştirme hatası", e);

        }
    }



}