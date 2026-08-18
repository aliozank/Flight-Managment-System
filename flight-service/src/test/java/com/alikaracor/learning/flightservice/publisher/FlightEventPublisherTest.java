package com.alikaracor.learning.flightservice.publisher;

import com.alikaracor.learning.flightservice.event.FlightEvent;
import com.alikaracor.learning.flightservice.event.FlightEventType;
import com.alikaracor.learning.flightservice.mapper.FlightMapper;
import com.alikaracor.learning.flightservice.model.Flight;
import com.alikaracor.learning.flightservice.service.OutboxService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlightEventPublisherTest {

    @Mock
    private OutboxService outboxService;

    @Mock
    private FlightMapper flightMapper;

    @InjectMocks
    private FlightEventPublisher flightEventPublisher;

    private Flight sampleFlight;

    @BeforeEach
    void setUp() {
        sampleFlight = new Flight();
        sampleFlight.setFlightId(100L);
        sampleFlight.setFlightNumber("TK1234");
    }

    @Test
    @DisplayName("publish - FlightEvent oluşturup outboxService.saveFlightEvent metoduna iletmelidir")
    void publish_shouldSaveEventToOutboxService() {
        FlightEvent baseEvent = new FlightEvent();
        when(flightMapper.toFlightEvent(sampleFlight)).thenReturn(baseEvent);

        Long actorUserId = 50L;
        FlightEventType eventType = FlightEventType.CREATED;

        flightEventPublisher.publish(sampleFlight, eventType, actorUserId);

        ArgumentCaptor<FlightEvent> eventCaptor = ArgumentCaptor.forClass(FlightEvent.class);
        verify(outboxService).saveFlightEvent(eventCaptor.capture());

        FlightEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent).isNotNull();
        assertThat(capturedEvent.getEventId()).isNotNull();
        assertThat(capturedEvent.getEventType()).isEqualTo(FlightEventType.CREATED);
        assertThat(capturedEvent.getChangedByUserId()).isEqualTo(50L);
        assertThat(capturedEvent.getOccurredAt()).isNotNull();
    }
}
