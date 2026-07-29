package com.alikaracor.learning.referencemanager.service;

import com.alikaracor.learning.referencemanager.dto.AirportRequest;
import com.alikaracor.learning.referencemanager.dto.AirportResponse;
import com.alikaracor.learning.referencemanager.event.ReferenceEvent;
import com.alikaracor.learning.referencemanager.event.ReferenceEventType;
import com.alikaracor.learning.referencemanager.event.ReferenceResourceType;
import com.alikaracor.learning.referencemanager.mapper.AirportMapper;
import com.alikaracor.learning.referencemanager.model.Airport;
import com.alikaracor.learning.referencemanager.model.AirportStatus;
import com.alikaracor.learning.referencemanager.publisher.ReferenceEventPublisher;
import com.alikaracor.learning.referencemanager.repository.AirportRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class AirportService {

    private final AirportRepository airportRepository;
    private final AirportMapper airportMapper;
    private final ReferenceEventPublisher  publisher;

    public AirportService(AirportRepository airportRepository, AirportMapper airportMapper, ReferenceEventPublisher publisher) {
        this.airportRepository = airportRepository;
        this.airportMapper = airportMapper;
        this.publisher = publisher;
    }

    @Transactional
    public AirportResponse addAirport(AirportRequest airportRequest) {

        String airportIataCode = airportRequest.getAirportIataCode();
        String airportIcaoCode = airportRequest.getAirportIcaoCode();

        if (airportRepository.existsByAirportIataCodeIgnoreCase(airportIataCode)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Iata code already exists"
            );
        }

        if (airportRepository.existsByAirportIcaoCodeIgnoreCase(airportIcaoCode)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Icao code already exists"
            );
        }

        String airportTimezone = validateTimezone(airportRequest.getAirportTimezone());

        Airport newAirport = airportMapper.toAirport(airportRequest);

        newAirport.setAirportTimezone(airportTimezone);
        newAirport.setAirportIataCode(airportIataCode);
        newAirport.setAirportIcaoCode(airportIcaoCode);

        Airport savedAirport = airportRepository.save(newAirport);

        ReferenceEvent referenceEvent = new ReferenceEvent();
        referenceEvent.setOccurredAt(Instant.now());
        referenceEvent.setResourceType(ReferenceResourceType.AIRPORT);
        referenceEvent.setEventType(ReferenceEventType.CREATED);
        referenceEvent.setEventId(UUID.randomUUID());
        referenceEvent.setResourceId(savedAirport.getAirportId());

        publisher.publish(referenceEvent);

        return airportMapper.toAirportResponse(savedAirport);
    }

    public List<AirportResponse> getAllAirports() {

        return airportRepository.findAll()
                .stream()
                .map(airportMapper::toAirportResponse)
                .toList();
    }

    public AirportResponse getAirportById(Long airportId) {

        Airport airport = airportRepository.findById(airportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Airport not found"
                ));

        return airportMapper.toAirportResponse(airport);
    }

    public AirportResponse getAirportByIataCode(String airportIataCode) {

        Airport airport = airportRepository.findByAirportIataCodeIgnoreCase(airportIataCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Airport not found"
                ));

        return airportMapper.toAirportResponse(airport);
    }

    public AirportResponse getAirportByIcaoCode(String airportIcaoCode) {

        Airport airport = airportRepository.findByAirportIcaoCodeIgnoreCase(airportIcaoCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Airport not found"
                ));

        return airportMapper.toAirportResponse(airport);
    }

    @Transactional
    public AirportResponse updateAirport(Long airportId, AirportRequest airportRequest) {

        Airport updatedAirport = airportRepository.findById(airportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bu id ile herhangi bir havalimanı bulunamadı"
                ));

        String airportIataCode = airportRequest.getAirportIataCode();
        String airportIcaoCode = airportRequest.getAirportIcaoCode();

        if (airportRepository.existsByAirportIataCodeIgnoreCaseAndAirportIdNot(airportIataCode, airportId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Iata code already exists"
            );
        }

        if (airportRepository.existsByAirportIcaoCodeIgnoreCaseAndAirportIdNot(airportIcaoCode, airportId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Icao code already exists"
            );
        }

        String airportTimezone = validateTimezone(airportRequest.getAirportTimezone());


        updatedAirport.setAirportName(airportRequest.getAirportName());
        updatedAirport.setAirportCity(airportRequest.getAirportCity());
        updatedAirport.setAirportCountry(airportRequest.getAirportCountry());
        updatedAirport.setAirportIataCode(airportIataCode);
        updatedAirport.setAirportIcaoCode(airportIcaoCode);
        updatedAirport.setAirportTimezone(airportTimezone);
        updatedAirport.setAirportStatus(airportRequest.getAirportStatus());

        Airport savedAirport = airportRepository.saveAndFlush(updatedAirport);

        ReferenceEvent referenceEvent = new ReferenceEvent();
        referenceEvent.setOccurredAt(Instant.now());
        referenceEvent.setResourceType(ReferenceResourceType.AIRPORT);
        referenceEvent.setEventType(ReferenceEventType.UPDATED);
        referenceEvent.setEventId(UUID.randomUUID());
        referenceEvent.setResourceId(savedAirport.getAirportId());

        publisher.publish(referenceEvent);

        return airportMapper.toAirportResponse(savedAirport);
    }

    @Transactional
    public void deactivateAirport(Long airportId) {

        Airport airport = airportRepository.findById(airportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bu id ile havalimanı bulunamadı"
                ));

        if (airport.getAirportStatus() == AirportStatus.PERMANENTLY_CLOSED) {
            return;
        }

        airport.setAirportStatus(AirportStatus.PERMANENTLY_CLOSED);

        airportRepository.save(airport);

        ReferenceEvent referenceEvent = new ReferenceEvent();
        referenceEvent.setOccurredAt(Instant.now());
        referenceEvent.setResourceType(ReferenceResourceType.AIRPORT);
        referenceEvent.setEventType(ReferenceEventType.DEACTIVATED);
        referenceEvent.setEventId(UUID.randomUUID());
        referenceEvent.setResourceId(airport.getAirportId());

        publisher.publish(referenceEvent);
    }

    // KONUM ZAMAN
    private String validateTimezone(String timezone) {

        if (timezone == null || timezone.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Timezone boş olamaz"
            );
        }

        String normalizedTimezone = timezone.trim();

        try {
            return ZoneId.of(normalizedTimezone).getId();
        } catch (DateTimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Geçersiz timezone: " + timezone
            );
        }
    }
}
