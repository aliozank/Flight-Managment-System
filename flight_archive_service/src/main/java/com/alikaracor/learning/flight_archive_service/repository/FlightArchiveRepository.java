package com.alikaracor.learning.flight_archive_service.repository;

import com.alikaracor.learning.flight_archive_service.model.ArchivedFlight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FlightArchiveRepository extends JpaRepository<ArchivedFlight, Long> {


    Optional<ArchivedFlight> findByFlightId(Long flightId);

    boolean existsByEventId(UUID eventId);





}
