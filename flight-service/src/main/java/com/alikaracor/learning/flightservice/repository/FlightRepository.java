package com.alikaracor.learning.flightservice.repository;

import com.alikaracor.learning.flightservice.model.Flight;
import com.alikaracor.learning.flightservice.model.FlightStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    boolean existsByFlightNumberAndFlightDate(String flightNumber, LocalDate flightDate);

    boolean existsByFlightNumberAndFlightDateAndFlightIdNot(String flightNumber, LocalDate flightDate, Long flightId);

    boolean existsByAircraftIdAndFlightStatusNotAndScheduledDepartureAtLessThanAndScheduledArrivalAtGreaterThan(Long aircraftId, FlightStatus cancelledStatus, Instant arrivalAt, Instant departureAt);

    boolean existsByAircraftIdAndFlightIdNotAndFlightStatusNotAndScheduledDepartureAtLessThanAndScheduledArrivalAtGreaterThan(Long aircraftId, Long flightId, FlightStatus cancelledStatus, Instant arrivalAt, Instant departureAt);

    List<Flight> findAllByFlightStatusInAndScheduledDepartureAtLessThanEqual(List<FlightStatus> flightStatuses, Instant scheduledDepartureAt);


    List<Flight> findAllByFlightStatusAndScheduledArrivalAtLessThanEqual(FlightStatus flightStatus, Instant scheduledArrivalAt);


}
