package com.alikaracor.learning.flight_archive_service.consumer;

import com.alikaracor.learning.flight_archive_service.event.FlightEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.alikaracor.learning.flight_archive_service.service.FlightArchiveService;

@Component
public class FlightEventConsumer {

    private final FlightArchiveService flightArchiveService;

    public FlightEventConsumer(FlightArchiveService flightArchiveService) {

        this.flightArchiveService = flightArchiveService;

    }


    @KafkaListener(topics = "${app.kafka.flight-events-topic}",
                   groupId = "${spring.kafka.consumer.group-id}")
    public void consume(FlightEvent flightEvent) {

        flightArchiveService.archiveFlight(flightEvent);

    }


}
