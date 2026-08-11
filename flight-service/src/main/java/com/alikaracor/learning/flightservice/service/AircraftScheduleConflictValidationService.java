package com.alikaracor.learning.flightservice.service;

import com.alikaracor.learning.flightservice.model.Flight;
import com.alikaracor.learning.flightservice.model.FlightStatus;
import com.alikaracor.learning.flightservice.repository.FlightRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class AircraftScheduleConflictValidationService {

    private final FlightRepository flightRepository;

    public AircraftScheduleConflictValidationService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    public void validateAircraftScheduleForCreate(Long aircraftId, LocalDate flightDate, LocalTime departureTime, LocalTime arrivalTime) {
        if (aircraftId == null) {
            return;
        }

        List<Flight> activeFlights = flightRepository.findActiveFlightsByAircraftIdAndFlightDate(aircraftId, flightDate, FlightStatus.CANCELLED);

        for (Flight existingFlight : activeFlights) {
            if (hasTimeConflict(departureTime, arrivalTime, existingFlight.getScheduledDepartureTime(), existingFlight.getScheduledArrivalTime())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Aircraft is occupied during this time window"
                );
            }
        }
    }

    public void validateAircraftScheduleForUpdate(Long flightId, Long aircraftId, LocalDate flightDate, LocalTime departureTime, LocalTime arrivalTime) {
        if (aircraftId == null) {
            return;
        }

        List<Flight> activeFlights = flightRepository.findActiveFlightsByAircraftIdAndFlightDate(aircraftId, flightDate, FlightStatus.CANCELLED);

        for (Flight existingFlight : activeFlights) {
            if (!existingFlight.getFlightId().equals(flightId) && 
                hasTimeConflict(departureTime, arrivalTime, existingFlight.getScheduledDepartureTime(), existingFlight.getScheduledArrivalTime())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Aircraft is occupied during this time window"
                );
            }
        }
    }

    private boolean hasTimeConflict(LocalTime newDeparture, LocalTime newArrival, LocalTime existingDeparture, LocalTime existingArrival) {
        return newDeparture.isBefore(existingArrival) && newArrival.isAfter(existingDeparture);
    }

}
