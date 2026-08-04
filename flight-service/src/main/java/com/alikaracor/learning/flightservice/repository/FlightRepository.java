package com.alikaracor.learning.flightservice.repository;

import com.alikaracor.learning.flightservice.model.Flight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    boolean existsByFlightNumberAndFlightDate(String flightNumber, LocalDate flightDate);

    boolean existsByFlightNumberAndFlightDateAndFlightIdNot(String flightNumber, LocalDate flightDate, Long flightId);
}
