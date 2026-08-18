package com.alikaracor.learning.referencemanager.service;

import com.alikaracor.learning.referencemanager.event.ReferenceEvent;
import com.alikaracor.learning.referencemanager.model.OutboxEvent;
import com.alikaracor.learning.referencemanager.model.OutboxStatus;
import com.alikaracor.learning.referencemanager.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    public void saveReferenceEvent(ReferenceEvent referenceEvent) {
        try {

            String payloadJson = objectMapper.writeValueAsString(referenceEvent);

            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setOutboxId(UUID.randomUUID().toString());
            outboxEvent.setEventId(referenceEvent.getEventId().toString());
            outboxEvent.setAggregateType(referenceEvent.getResourceType().name());
            outboxEvent.setAggregateId(referenceEvent.getResourceId().toString());
            outboxEvent.setEventType(referenceEvent.getEventType().name());
            outboxEvent.setTopicName("reference.events");
            outboxEvent.setPayload(payloadJson);
            outboxEvent.setStatus(OutboxStatus.PENDING);
            outboxEvent.setAttemptCount(0);
            outboxEvent.setCreatedAt(Instant.now());

            outboxEventRepository.save(outboxEvent);
        }

        catch (Exception e) {

            throw new RuntimeException("ReferenceEvent JSON serileştirme hatası", e);

        }

    }
}
