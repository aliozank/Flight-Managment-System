package com.alikaracor.learning.flight_archive_service.service;

import com.alikaracor.learning.flight_archive_service.dto.ArchivedFlightResponse;
import com.alikaracor.learning.flight_archive_service.event.FlightEvent;
import com.alikaracor.learning.flight_archive_service.mapper.ArchiveFlightMapper;
import com.alikaracor.learning.flight_archive_service.model.ArchivedFlight;
import com.alikaracor.learning.flight_archive_service.model.FlightStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alikaracor.learning.flight_archive_service.repository.FlightArchiveRepository;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FlightArchiveService {

    private final FlightArchiveRepository flightArchiveRepository;
    private final ArchiveFlightMapper archiveFlightMapper;

    public FlightArchiveService(FlightArchiveRepository flightArchiveRepository, ArchiveFlightMapper archiveFlightMapper) {
        this.flightArchiveRepository = flightArchiveRepository;
        this.archiveFlightMapper = archiveFlightMapper;
    }

    @Transactional
    public void archiveFlight(FlightEvent flightEvent) {

        validateFlightEvent(flightEvent);

        if (flightArchiveRepository.existsByEventId(flightEvent.getEventId())) {

            return;

        }

        boolean finalStatus = flightEvent.getFlightStatus() == FlightStatus.ARRIVED || flightEvent.getFlightStatus() == FlightStatus.CANCELLED;

        if (!finalStatus) {

            return;
        }

        ArchivedFlight archivedFlight = flightArchiveRepository
                .findByFlightId(flightEvent.getFlightId())
                .orElseGet(ArchivedFlight::new);

        if (archivedFlight.getFlightVersion() != null && flightEvent.getFlightVersion() <= archivedFlight.getFlightVersion()) {

            return;

        }

        archivedFlight.setEventId(flightEvent.getEventId());
        archivedFlight.setFlightId(flightEvent.getFlightId());
        archivedFlight.setFlightNumber(flightEvent.getFlightNumber());

        archivedFlight.setAirlineId(flightEvent.getAirlineId());
        archivedFlight.setAircraftId(flightEvent.getAircraftId());
        archivedFlight.setAircraftTypeId(flightEvent.getAircraftTypeId());

        archivedFlight.setOriginAirportId(flightEvent.getOriginAirportId());
        archivedFlight.setDestinationAirportId(flightEvent.getDestinationAirportId());
        archivedFlight.setFlightTypeId(flightEvent.getFlightTypeId());

        archivedFlight.setFlightDate(flightEvent.getFlightDate());
        archivedFlight.setScheduledDepartureTime(flightEvent.getScheduledDepartureTime());
        archivedFlight.setScheduledArrivalTime(flightEvent.getScheduledArrivalTime());

        archivedFlight.setFlightStatus(flightEvent.getFlightStatus());
        archivedFlight.setFlightVersion(flightEvent.getFlightVersion());
        archivedFlight.setChangedByUserId(flightEvent.getChangedByUserId());
        archivedFlight.setEventOccurredAt(flightEvent.getOccurredAt());

        archivedFlight.setScheduledArrivalDate(flightEvent.getScheduledArrivalDate());
        archivedFlight.setScheduledDepartureAt(flightEvent.getScheduledDepartureAt());
        archivedFlight.setScheduledArrivalAt(flightEvent.getScheduledArrivalAt());

        flightArchiveRepository.save(archivedFlight);

    }

    public List<ArchivedFlightResponse> getAllArchivedFlights() {

        return flightArchiveRepository.findAll()
                .stream()
                .map(archiveFlightMapper::toArchivedFlightResponse)
                .toList();

    }

    public ArchivedFlightResponse getArchivedFlightByFlightId(Long flightId) {

        ArchivedFlight archivedFlight = flightArchiveRepository.findByFlightId(flightId)
                .orElseThrow(() -> new ResponseStatusException( HttpStatus.NOT_FOUND,
                        "Bu id ile bir kayıt bulunamadı"));

        return archiveFlightMapper.toArchivedFlightResponse(archivedFlight);

    }

    public ArchivedFlightResponse getArchivedFlightByArchiveId(Long archiveId) {

        ArchivedFlight archivedFlight = flightArchiveRepository.findById(archiveId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Archived flight not found"
                ));

        return archiveFlightMapper.toArchivedFlightResponse(archivedFlight);
    }

    private void validateFlightEvent(FlightEvent flightEvent) {

        if (flightEvent == null) {

            throw new IllegalArgumentException("Flight event cannot be null");

        }

        if (flightEvent.getEventId() == null
                || flightEvent.getFlightId() == null
                || flightEvent.getFlightStatus() == null
                || flightEvent.getFlightVersion() == null
                || flightEvent.getFlightVersion() < 1) {

            throw new IllegalArgumentException("Flight event identity/status fields are missing");

        }

        boolean finalStatus = flightEvent.getFlightStatus() == FlightStatus.ARRIVED || flightEvent.getFlightStatus() == FlightStatus.CANCELLED;

        if (!finalStatus) {

            return;

        }

        if (flightEvent.getFlightNumber() == null
                || flightEvent.getFlightNumber().isBlank()
                || flightEvent.getAirlineId() == null
                || flightEvent.getAircraftTypeId() == null
                || flightEvent.getOriginAirportId() == null
                || flightEvent.getDestinationAirportId() == null
                || flightEvent.getFlightTypeId() == null
                || flightEvent.getFlightDate() == null
                || flightEvent.getScheduledDepartureTime() == null
                || flightEvent.getScheduledArrivalDate() == null
                || flightEvent.getScheduledArrivalTime() == null
                || flightEvent.getScheduledDepartureAt() == null
                || flightEvent.getScheduledArrivalAt() == null
                || flightEvent.getOccurredAt() == null) {

            throw new IllegalArgumentException("Final flight event contains missing archive fields");

        }
    }

}
