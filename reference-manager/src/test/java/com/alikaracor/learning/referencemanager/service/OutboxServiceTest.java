package com.alikaracor.learning.referencemanager.service;

import com.alikaracor.learning.referencemanager.event.ReferenceEvent;
import com.alikaracor.learning.referencemanager.event.ReferenceEventType;
import com.alikaracor.learning.referencemanager.event.ReferenceResourceType;
import com.alikaracor.learning.referencemanager.model.OutboxEvent;
import com.alikaracor.learning.referencemanager.model.OutboxStatus;
import com.alikaracor.learning.referencemanager.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OutboxService outboxService;

    private ReferenceEvent sampleEvent;

    @BeforeEach
    void setUp() {
        sampleEvent = new ReferenceEvent();
        sampleEvent.setEventId(UUID.randomUUID());
        sampleEvent.setResourceType(ReferenceResourceType.AIRLINE);
        sampleEvent.setResourceId(10L);
        sampleEvent.setEventType(ReferenceEventType.CREATED);
        sampleEvent.setOccurredAt(Instant.now());
    }

    @Test
    @DisplayName("saveReferenceEvent - ReferenceEvent nesnesini PENDING durumunda outbox_events tablosuna kaydetmelidir")
    void saveReferenceEvent_shouldSaveOutboxEventWithPendingStatus() {
        outboxService.saveReferenceEvent(sampleEvent);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved).isNotNull();
        assertThat(saved.getOutboxId()).isNotBlank();
        assertThat(saved.getEventId()).isEqualTo(sampleEvent.getEventId().toString());
        assertThat(saved.getAggregateType()).isEqualTo("AIRLINE");
        assertThat(saved.getAggregateId()).isEqualTo("10");
        assertThat(saved.getEventType()).isEqualTo("CREATED");
        assertThat(saved.getTopicName()).isEqualTo("reference.events");
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getAttemptCount()).isEqualTo(0);
        assertThat(saved.getPayload()).contains("AIRLINE");
    }
}
