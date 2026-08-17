package com.alikaracor.learning.flightservice.scheduler;

import com.alikaracor.learning.flightservice.model.Flight;
import com.alikaracor.learning.flightservice.model.FlightStatus;
import com.alikaracor.learning.flightservice.repository.FlightRepository;
import com.alikaracor.learning.flightservice.service.FlightService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class FlightStatusScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlightStatusScheduler.class);

    private final FlightRepository flightRepository;
    private final FlightService flightService;

    public FlightStatusScheduler(FlightRepository flightRepository, FlightService flightService) {
        this.flightRepository = flightRepository;
        this.flightService = flightService;
    }

    @Scheduled(fixedDelayString = "${app.flight-status-scheduler.fixed-delay-ms:30000}")
    public void updateFlightStatuses() {
        Instant now = Instant.now();

        updateDueFlights(flightRepository.findAllByFlightStatusInAndScheduledDepartureAtLessThanEqual(List.of(FlightStatus.SCHEDULED, FlightStatus.DELAYED), now),

                FlightStatus.DEPARTED

        );


        updateDueFlights(flightRepository.findAllByFlightStatusAndScheduledArrivalAtLessThanEqual(FlightStatus.DEPARTED, now), FlightStatus.ARRIVED);

    }

    private void updateDueFlights(List<Flight> flights, FlightStatus targetStatus) {
        for (Flight flight : flights) {

            try {

                flightService.updateFlightStatusAutomatically(flight.getFlightId(), targetStatus);


            } catch (RuntimeException exception) {
                LOGGER.warn("Automatic status update failed for flightId={} targetStatus={}",
                          flight.getFlightId(),
                          targetStatus,
                          exception
                );
            }
        }
    }
}
