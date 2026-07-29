package com.alikaracor.learning.referencemanager.repository;

import com.alikaracor.learning.referencemanager.model.Airline;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AirlineRepository extends JpaRepository<Airline, Long> {

    boolean existsByAirlineNameIgnoreCase(String airlineName);

    boolean existsByAirlineIataCodeIgnoreCase(String airlineIataCode);

    boolean existsByAirlineIcaoCodeIgnoreCase(String airlineIcaoCode);

    boolean existsByAirlineNameIgnoreCaseAndAirlineIdNot(String airlineName, Long airlineId);

    boolean existsByAirlineIataCodeIgnoreCaseAndAirlineIdNot(String airlineIataCode, Long airlineId);

    boolean existsByAirlineIcaoCodeIgnoreCaseAndAirlineIdNot(String airlineIcaoCode, Long airlineId);





}
