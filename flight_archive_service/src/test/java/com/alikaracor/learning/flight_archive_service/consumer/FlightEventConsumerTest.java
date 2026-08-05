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
    @DisplayName("consume - Gelen FlightEvent nesnesini FlightArchiveService.archiveFlight metoduna iletmelidir")
    void consume_shouldPassFlightEventToFlightArchiveService() {
        FlightEvent flightEvent = new FlightEvent();

        flightEventConsumer.consume(flightEvent);

        verify(flightArchiveService).archiveFlight(flightEvent);
    }
}
