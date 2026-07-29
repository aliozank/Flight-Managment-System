package com.alikaracor.learning.referencemanager.service;

import com.alikaracor.learning.referencemanager.dto.AirlineRequest;
import com.alikaracor.learning.referencemanager.dto.AirlineResponse;
import com.alikaracor.learning.referencemanager.event.ReferenceEvent;
import com.alikaracor.learning.referencemanager.event.ReferenceEventType;
import com.alikaracor.learning.referencemanager.event.ReferenceResourceType;
import com.alikaracor.learning.referencemanager.mapper.AirlineMapper;
import com.alikaracor.learning.referencemanager.model.Airline;
import com.alikaracor.learning.referencemanager.model.AirlineStatus;
import com.alikaracor.learning.referencemanager.publisher.ReferenceEventPublisher;
import com.alikaracor.learning.referencemanager.repository.AirlineRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AirlineService {

    private final AirlineRepository airlineRepository;
    private final AirlineMapper airlineMapper;
    private final ReferenceEventPublisher publisher;

    public AirlineService(AirlineRepository airlineRepository, AirlineMapper airlineMapper, ReferenceEventPublisher publisher) {
        this.airlineRepository = airlineRepository;
        this.airlineMapper = airlineMapper;
        this.publisher = publisher;
    }

    @Transactional
    public AirlineResponse addAirline(AirlineRequest airlineRequest) {

        boolean existsByIcao = airlineRepository.existsByAirlineIcaoCodeIgnoreCase(airlineRequest.getAirlineIcaoCode());

        if (existsByIcao) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Icao code already exists"
            );
        }

        boolean existsByIata = airlineRepository.existsByAirlineIataCodeIgnoreCase(airlineRequest.getAirlineIataCode());

        if (existsByIata) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Iata code already exists"
            );
        }

        boolean existsByAirlineName = airlineRepository.existsByAirlineNameIgnoreCase(airlineRequest.getAirlineName());

        if (existsByAirlineName) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Airline already exists"
            );
        }

        Airline newAirline = airlineMapper.toAirline(airlineRequest);

        String newAirlineName = airlineRequest.getAirlineName().trim();
        String newAirlineCountry = airlineRequest.getAirlineCountry().trim();

        newAirline.setAirlineName(newAirlineName);
        newAirline.setAirlineCountry(newAirlineCountry);

        Airline savedAirline = airlineRepository.save(newAirline);

        ReferenceEvent referenceEvent = new ReferenceEvent();
        referenceEvent.setOccurredAt(Instant.now());
        referenceEvent.setResourceType(ReferenceResourceType.AIRLINE);
        referenceEvent.setEventType(ReferenceEventType.CREATED);
        referenceEvent.setEventId(UUID.randomUUID());
        referenceEvent.setResourceId(savedAirline.getAirlineId());

        publisher.publish(referenceEvent);


        return airlineMapper.toAirlineResponse(savedAirline);

    }

    public List<AirlineResponse> getAllAirlines() {


        return airlineRepository.findAll()
                .stream()
                .map(airlineMapper::toAirlineResponse)
                .toList();


    }

    public AirlineResponse getAirlineById(Long airlineId) {

        Airline airline = airlineRepository.findById(airlineId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Airline not found"
                ));

        return airlineMapper.toAirlineResponse(airline);


    }

    @Transactional
    public AirlineResponse updateAirline(Long airlineId, AirlineRequest airlineRequest) {

        Airline updatedAirline = airlineRepository.findById(airlineId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bu id ile herhangi bir havayolu bulunamadı"));

        String airlineIataCode = airlineRequest.getAirlineIataCode();

        if (airlineRepository.existsByAirlineIataCodeIgnoreCaseAndAirlineIdNot(airlineIataCode, airlineId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Iata code already exists");
        }

        String airlineName = airlineRequest.getAirlineName().trim();

        if (airlineRepository.existsByAirlineNameIgnoreCaseAndAirlineIdNot(airlineName, airlineId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Airline name already exists");
        }

        String airlineIcaoCode = airlineRequest.getAirlineIcaoCode();

        if (airlineRepository.existsByAirlineIcaoCodeIgnoreCaseAndAirlineIdNot(airlineIcaoCode, airlineId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Icao code already exists"
            );
        }

        updatedAirline.setAirlineName(airlineName);
        updatedAirline.setAirlineIataCode(airlineIataCode);
        updatedAirline.setAirlineCountry(airlineRequest.getAirlineCountry().trim());
        updatedAirline.setAirlineIcaoCode(airlineIcaoCode);
        updatedAirline.setAirlineStatus(airlineRequest.getAirlineStatus());

        Airline savedAirline = airlineRepository.saveAndFlush(updatedAirline);

        ReferenceEvent referenceEvent = new ReferenceEvent();
        referenceEvent.setOccurredAt(Instant.now());
        referenceEvent.setResourceType(ReferenceResourceType.AIRLINE);
        referenceEvent.setEventType(ReferenceEventType.UPDATED);
        referenceEvent.setEventId(UUID.randomUUID());
        referenceEvent.setResourceId(savedAirline.getAirlineId());

        publisher.publish(referenceEvent);


        return airlineMapper.toAirlineResponse(savedAirline);

    }

    @Transactional
    public void deactivateAirline(Long airlineId) {

        Airline airline = airlineRepository.findById(airlineId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bu id ile havayolu bulunamadı"
                ));

        if (airline.getAirlineStatus() == AirlineStatus.INACTIVE) {

            return;

        }

        airline.setAirlineStatus(AirlineStatus.INACTIVE);

        airlineRepository.save(airline);


        ReferenceEvent referenceEvent = new ReferenceEvent();
        referenceEvent.setOccurredAt(Instant.now());
        referenceEvent.setResourceType(ReferenceResourceType.AIRLINE);
        referenceEvent.setEventType(ReferenceEventType.DEACTIVATED);
        referenceEvent.setEventId(UUID.randomUUID());
        referenceEvent.setResourceId(airline.getAirlineId());

        publisher.publish(referenceEvent);



    }

}




