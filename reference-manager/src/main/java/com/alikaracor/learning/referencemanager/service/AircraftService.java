package com.alikaracor.learning.referencemanager.service;

import com.alikaracor.learning.referencemanager.dto.AircraftRequest;
import com.alikaracor.learning.referencemanager.dto.AircraftResponse;
import com.alikaracor.learning.referencemanager.event.ReferenceEvent;
import com.alikaracor.learning.referencemanager.event.ReferenceEventType;
import com.alikaracor.learning.referencemanager.event.ReferenceResourceType;
import com.alikaracor.learning.referencemanager.mapper.AircraftMapper;
import com.alikaracor.learning.referencemanager.model.Aircraft;
import com.alikaracor.learning.referencemanager.model.AircraftStatus;
import com.alikaracor.learning.referencemanager.model.AircraftType;
import com.alikaracor.learning.referencemanager.model.Airline;
import com.alikaracor.learning.referencemanager.publisher.ReferenceEventPublisher;
import com.alikaracor.learning.referencemanager.repository.AircraftRepository;
import com.alikaracor.learning.referencemanager.repository.AircraftTypeRepository;
import com.alikaracor.learning.referencemanager.repository.AirlineRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
public class AircraftService {

    private final AircraftRepository aircraftRepository;

    private final AircraftMapper aircraftMapper;
    private final AircraftTypeRepository aircraftTypeRepository;
    private final AirlineRepository airlineRepository;
    private final ReferenceEventPublisher publisher;

    public AircraftService(AircraftMapper aircraftMapper, AircraftRepository aircraftRepository, AircraftTypeRepository aircraftTypeRepository, AirlineRepository airlineRepository, ReferenceEventPublisher publisher) {
        this.aircraftMapper = aircraftMapper;
        this.aircraftRepository = aircraftRepository;
        this.aircraftTypeRepository = aircraftTypeRepository;
        this.airlineRepository = airlineRepository;
        this.publisher = publisher;
    }

    public List<AircraftResponse> getAllAircrafts() {

        return aircraftRepository.findAll()
                .stream()
                .map(aircraftMapper::toAircraftResponse)
                .toList();
    }

    public AircraftResponse getAircraftById(Long aircraftId) {

        Aircraft aircraft = aircraftRepository.findById(aircraftId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bu id ile aircraft nicht gefunden! :D"
                ));

        return aircraftMapper.toAircraftResponse(aircraft);

    }

    @Transactional
    public AircraftResponse addAircraft(AircraftRequest aircraftRequest) {

        if(aircraftRepository.existsByAircraftRegistrationNumberIgnoreCase(aircraftRequest.getAircraftRegistrationNumber())){

            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Bu aircraft registration number daha önce kullanılmış");

        }

        AircraftType aircraftType = aircraftTypeRepository.findById(aircraftRequest.getAircraftTypeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Bu aircraft type id ile type bulunamadı"));

        Airline operatorAirline = null;

        if (aircraftRequest.getOperatorAirlineId() != null){

            operatorAirline = airlineRepository.findById(aircraftRequest.getOperatorAirlineId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Bu airline id ile airline bulunamadı"));

        }

        if(aircraftRequest.getAircraftManufactureYear() > Year.now().getValue()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "İleri tarihli üretim uçak yazılamaz");
        }

        Aircraft newAircraft = aircraftMapper.toAircraft(aircraftRequest);

        newAircraft.setOperatorAirline(operatorAirline);
        newAircraft.setAircraftType(aircraftType);

        Aircraft savedAircraft = aircraftRepository.save(newAircraft);

        ReferenceEvent referenceEvent = new ReferenceEvent();
        referenceEvent.setEventId(UUID.randomUUID());
        referenceEvent.setEventType(ReferenceEventType.CREATED);
        referenceEvent.setResourceType(ReferenceResourceType.AIRCRAFT);
        referenceEvent.setResourceId(savedAircraft.getAircraftId());
        referenceEvent.setOccurredAt(Instant.now());

        publisher.publish(referenceEvent);


        return aircraftMapper.toAircraftResponse(savedAircraft);


    }

    @Transactional
    public AircraftResponse updateAircraftById(Long aircraftId, AircraftRequest aircraftRequest) {

        Aircraft oldAircraft = aircraftRepository.findById(aircraftId)
                .orElseThrow(() ->new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bu id ile herhangi bir aircraft yok"));


        if(aircraftRepository.existsByAircraftRegistrationNumberIgnoreCaseAndAircraftIdNot(aircraftRequest.getAircraftRegistrationNumber(), aircraftId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Bu registration number ile mevcut bir kayıt var");

        }

        AircraftType aircraftType = aircraftTypeRepository.findById(aircraftRequest.getAircraftTypeId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                        "Bu id ile herhangi bir aircraft type yok"));

        Airline operatorAirline = null;

        if(aircraftRequest.getOperatorAirlineId() != null) {
            operatorAirline = airlineRepository.findById(aircraftRequest.getOperatorAirlineId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Bu airline id ile airline bulunamadı"));
        }

        if(aircraftRequest.getAircraftManufactureYear() > Year.now().getValue()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "İleri tarihli üretim uçak yazılamaz");
        }

        oldAircraft.setAircraftRegistrationNumber(aircraftRequest.getAircraftRegistrationNumber());
        oldAircraft.setAircraftCapacity(aircraftRequest.getAircraftCapacity());
        oldAircraft.setAircraftManufactureYear(aircraftRequest.getAircraftManufactureYear());
        oldAircraft.setAircraftStatus(aircraftRequest.getAircraftStatus());
        oldAircraft.setAircraftType(aircraftType);
        oldAircraft.setOperatorAirline(operatorAirline);

        Aircraft savedAircraft = aircraftRepository.saveAndFlush(oldAircraft);

        ReferenceEvent referenceEvent = new ReferenceEvent();
        referenceEvent.setEventId(UUID.randomUUID());
        referenceEvent.setEventType(ReferenceEventType.UPDATED);
        referenceEvent.setResourceType(ReferenceResourceType.AIRCRAFT);
        referenceEvent.setResourceId(savedAircraft.getAircraftId());
        referenceEvent.setOccurredAt(Instant.now());

        publisher.publish(referenceEvent);

        return aircraftMapper.toAircraftResponse(savedAircraft);

    }

    @Transactional
    public void deactiveAircraftById(Long aircraftId) {

        Aircraft aircraft =aircraftRepository.findById(aircraftId)
                .orElseThrow(()   -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Bu id ile aircraft bulunamadı"));

        if(aircraft.getAircraftStatus() == AircraftStatus.RETIRED) {
            return;
        }

        aircraft.setAircraftStatus(AircraftStatus.RETIRED);

        aircraftRepository.save(aircraft);

        ReferenceEvent referenceEvent = new ReferenceEvent();
        referenceEvent.setEventId(UUID.randomUUID());
        referenceEvent.setEventType(ReferenceEventType.DEACTIVATED);
        referenceEvent.setResourceType(ReferenceResourceType.AIRCRAFT);
        referenceEvent.setResourceId(aircraft.getAircraftId());
        referenceEvent.setOccurredAt(Instant.now());

        publisher.publish(referenceEvent);

    }

}
