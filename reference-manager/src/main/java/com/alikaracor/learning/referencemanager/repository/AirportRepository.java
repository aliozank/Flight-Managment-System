package com.alikaracor.learning.referencemanager.repository;

import com.alikaracor.learning.referencemanager.model.Airport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AirportRepository extends JpaRepository<Airport, Long> {

    boolean existsByAirportIataCodeIgnoreCase(String airportIataCode);

    boolean existsByAirportIcaoCodeIgnoreCase(String airportIcaoCode);

    boolean existsByAirportIataCodeIgnoreCaseAndAirportIdNot(String airportIataCode, Long airportId);

    boolean existsByAirportIcaoCodeIgnoreCaseAndAirportIdNot(String airportIcaoCode, Long airportId);

    Optional<Airport> findByAirportIataCodeIgnoreCase(String airportIataCode);

    Optional<Airport> findByAirportIcaoCodeIgnoreCase(String airportIcaoCode);
}
