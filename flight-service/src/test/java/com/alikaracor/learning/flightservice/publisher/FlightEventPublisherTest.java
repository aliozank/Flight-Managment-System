package com.alikaracor.learning.flightservice.publisher;

import com.alikaracor.learning.flightservice.event.FlightEvent;
import com.alikaracor.learning.flightservice.event.FlightEventType;
import com.alikaracor.learning.flightservice.mapper.FlightMapper;
import com.alikaracor.learning.flightservice.model.Flight;
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
    private KafkaTemplate<String, FlightEvent> kafkaTemplate;

    @Mock
    private FlightMapper flightMapper;

    @InjectMocks
    private FlightEventPublisher flightEventPublisher;

    private Flight sampleFlight;

    @BeforeEach
    void setUp() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.initSynchronization();
        }

        sampleFlight = new Flight();
        sampleFlight.setFlightId(100L);
        sampleFlight.setFlightNumber("TK1234");
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("publish - Transaction commit edildikten sonra (afterCommit) Kafka'ya doğru parametrelerle mesaj göndermelidir")
    void publish_shouldSendKafkaMessage_afterTransactionCommit() {
        FlightEvent baseEvent = new FlightEvent();
        when(flightMapper.toFlightEvent(sampleFlight)).thenReturn(baseEvent);

        Long actorUserId = 50L;
        FlightEventType eventType = FlightEventType.CREATED;

        flightEventPublisher.publish(sampleFlight, eventType, actorUserId);

        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);

        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }

        ArgumentCaptor<FlightEvent> eventCaptor = ArgumentCaptor.forClass(FlightEvent.class);
        verify(kafkaTemplate).send(
                eq("flight.events"),
                eq("100"),
                eventCaptor.capture()
        );

        FlightEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent).isNotNull();
        assertThat(capturedEvent.getEventId()).isNotNull();
        assertThat(capturedEvent.getEventType()).isEqualTo(FlightEventType.CREATED);
        assertThat(capturedEvent.getChangedByUserId()).isEqualTo(50L);
        assertThat(capturedEvent.getOccurredAt()).isNotNull();
    }
}
