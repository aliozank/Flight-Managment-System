package com.alikaracor.learning.flightservice.repository;

import com.alikaracor.learning.flightservice.model.FlightVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FlightVersionRepository extends JpaRepository<FlightVersion, Long> {

    List<FlightVersion> findAllByFlight_FlightIdOrderByFlightVersionNumberAsc(Long flightId);

    Optional<FlightVersion> findByFlight_FlightIdAndFlightVersionNumber(Long flightId, Integer flightVersionNumber);

    Optional<FlightVersion> findFirstByFlight_FlightIdOrderByFlightVersionNumberDesc(Long flightId);

}
