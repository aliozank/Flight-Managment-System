package com.alikaracor.learning.flightservice.service;

import com.alikaracor.learning.flightservice.model.FlightStatus;
import com.alikaracor.learning.flightservice.repository.FlightRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class AircraftScheduleConflictValidationService {

    private final FlightRepository flightRepository;

    public AircraftScheduleConflictValidationService(
            FlightRepository flightRepository
    ) {
        this.flightRepository = flightRepository;
    }

    public void validateAircraftScheduleForCreate(
            Long aircraftId,
            Instant departureAt,
            Instant arrivalAt
    ) {
        if (aircraftId == null) {
            return;
        }

        boolean conflict =
                flightRepository.existsByAircraftIdAndFlightStatusNotAndScheduledDepartureAtLessThanAndScheduledArrivalAtGreaterThan(
                aircraftId,
                FlightStatus.CANCELLED,
                arrivalAt,
                departureAt
        );

        if (conflict) {
            throwScheduleConflict();
        }
    }

    public void validateAircraftScheduleForUpdate(
            Long flightId,
            Long aircraftId,
            Instant departureAt,
            Instant arrivalAt
    ) {
        if (aircraftId == null) {
            return;
        }

        boolean conflict =
                flightRepository.existsByAircraftIdAndFlightIdNotAndFlightStatusNotAndScheduledDepartureAtLessThanAndScheduledArrivalAtGreaterThan(
                aircraftId,
                flightId,
                FlightStatus.CANCELLED,
                arrivalAt,
                departureAt
        );

        if (conflict) {
            throwScheduleConflict();
        }
    }

    private void throwScheduleConflict() {
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Aircraft is occupied during this time window"
        );
    }
}
