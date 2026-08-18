package com.alikaracor.learning.flightservice.service;

import com.alikaracor.learning.flightservice.event.FlightEvent;
import com.alikaracor.learning.flightservice.event.FlightEventType;
import com.alikaracor.learning.flightservice.model.OutboxEvent;
import com.alikaracor.learning.flightservice.model.OutboxStatus;
import com.alikaracor.learning.flightservice.repository.OutboxEventRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private FlightEvent sampleEvent;

    @BeforeEach
    void setUp() {
        sampleEvent = new FlightEvent();
        sampleEvent.setEventId(UUID.randomUUID());
        sampleEvent.setFlightId(42L);
        sampleEvent.setEventType(FlightEventType.CREATED);
        sampleEvent.setOccurredAt(Instant.now());
    }

    @Test
    @DisplayName("saveFlightEvent - FlightEvent nesnesini PENDING durumunda outbox_events tablosuna kaydetmelidir")
    void saveFlightEvent_shouldSaveOutboxEventWithPendingStatus() {
        outboxService.saveFlightEvent(sampleEvent);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved).isNotNull();
        assertThat(saved.getOutboxId()).isNotBlank();
        assertThat(saved.getEventId()).isEqualTo(sampleEvent.getEventId().toString());
        assertThat(saved.getAggregateType()).isEqualTo("FLIGHT");
        assertThat(saved.getAggregateId()).isEqualTo("42");
        assertThat(saved.getEventType()).isEqualTo("CREATED");
        assertThat(saved.getTopicName()).isEqualTo("flight.events");
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getAttemptCount()).isEqualTo(0);
        assertThat(saved.getPayload()).contains("42");
    }
}
