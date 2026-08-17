package com.alikaracor.learning.flight_archive_service.consumer;

import com.alikaracor.learning.flight_archive_service.event.FlightEvent;
import com.alikaracor.learning.flight_archive_service.service.FlightArchiveService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlightEventConsumerTest {

    @Mock
    private FlightArchiveService flightArchiveService;

    @InjectMocks
    private FlightEventConsumer flightEventConsumer;

    @Test
    @DisplayName("consume - ARRIVED statüsündeki FlightEvent nesnesini FlightArchiveService.archiveFlight metoduna iletmelidir")
    void consume_shouldPassFlightEventToFlightArchiveService_forArrived() {
        FlightEvent flightEvent = new FlightEvent();
        flightEvent.setFlightStatus(com.alikaracor.learning.flight_archive_service.model.FlightStatus.ARRIVED);

        flightEventConsumer.consume(flightEvent);

        verify(flightArchiveService).archiveFlight(flightEvent);
    }

    @Test
    @DisplayName("consume - CANCELLED statüsündeki FlightEvent nesnesini FlightArchiveService.archiveFlight metoduna iletmelidir")
    void consume_shouldPassFlightEventToFlightArchiveService_forCancelled() {
        FlightEvent flightEvent = new FlightEvent();
        flightEvent.setFlightStatus(com.alikaracor.learning.flight_archive_service.model.FlightStatus.CANCELLED);

        flightEventConsumer.consume(flightEvent);

        verify(flightArchiveService).archiveFlight(flightEvent);
    }
}
