package com.alikaracor.learning.flightservice.repository;

import com.alikaracor.learning.flightservice.model.Flight;
import com.alikaracor.learning.flightservice.model.FlightStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    boolean existsByFlightNumberAndFlightDate(String flightNumber, LocalDate flightDate);

    boolean existsByFlightNumberAndFlightDateAndFlightIdNot(String flightNumber, LocalDate flightDate, Long flightId);

    boolean existsByAircraftIdAndFlightDateAndFlightIdNot(Long aircraftId, LocalDate flightDate, Long flightId);

    boolean existsByAircraftIdAndFlightDate(Long aircraftId, LocalDate flightDate);

    List<Flight> findByAircraftIdAndFlightDate(Long aircraftId, LocalDate flightDate);

    @Query("SELECT f FROM Flight f WHERE f.aircraftId = :aircraftId AND f.flightDate = :flightDate AND f.flightStatus != :cancelledStatus")
    List<Flight> findActiveFlightsByAircraftIdAndFlightDate(
            @Param("aircraftId") Long aircraftId,
            @Param("flightDate") LocalDate flightDate,
            @Param("cancelledStatus") FlightStatus cancelledStatus
    );

}
