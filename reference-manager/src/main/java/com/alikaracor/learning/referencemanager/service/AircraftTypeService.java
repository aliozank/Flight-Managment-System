package com.alikaracor.learning.referencemanager.service;

import com.alikaracor.learning.referencemanager.dto.AircraftTypeRequest;
import com.alikaracor.learning.referencemanager.dto.AircraftTypeResponse;
import com.alikaracor.learning.referencemanager.event.ReferenceEvent;
import com.alikaracor.learning.referencemanager.event.ReferenceEventType;
import com.alikaracor.learning.referencemanager.event.ReferenceResourceType;
import com.alikaracor.learning.referencemanager.mapper.AircraftTypeMapper;
import com.alikaracor.learning.referencemanager.model.AircraftType;
import com.alikaracor.learning.referencemanager.model.AircraftTypeStatus;
import com.alikaracor.learning.referencemanager.publisher.ReferenceEventPublisher;
import com.alikaracor.learning.referencemanager.repository.AircraftTypeRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AircraftTypeService {

    private final AircraftTypeRepository aircraftTypeRepository;

    private final AircraftTypeMapper aircraftTypeMapper;

    private final ReferenceEventPublisher publisher;

    public AircraftTypeService(AircraftTypeRepository aircraftTypeRepository, AircraftTypeMapper aircraftTypeMapper, ReferenceEventPublisher publisher) {
        this.aircraftTypeRepository = aircraftTypeRepository;
        this.aircraftTypeMapper = aircraftTypeMapper;
        this.publisher = publisher;
    }

    @Transactional
    public AircraftTypeResponse addAircraftType(AircraftTypeRequest aircraftTypeRequest) {

        String newIcaoCode = aircraftTypeRequest.getAircraftTypeIcaoCode();

        if (aircraftTypeRepository.existsByAircraftTypeIcaoCodeIgnoreCase(newIcaoCode)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Aircraft type already exists");

        }

        AircraftType newAircraftType = aircraftTypeMapper.toAircraftType(aircraftTypeRequest);

        AircraftType savedAircraftType = aircraftTypeRepository.save(newAircraftType);

        ReferenceEvent referenceEvent = new ReferenceEvent();
        referenceEvent.setOccurredAt(Instant.now());
        referenceEvent.setResourceType(ReferenceResourceType.AIRCRAFT_TYPE);
        referenceEvent.setEventType(ReferenceEventType.CREATED);
        referenceEvent.setEventId(UUID.randomUUID());
        referenceEvent.setResourceId(savedAircraftType.getAircraftTypeId());

        publisher.publish(referenceEvent);

        return aircraftTypeMapper.toAircraftTypeResponse(savedAircraftType);

    }

    public List<AircraftTypeResponse> getAllAircraftTypes() {

        return aircraftTypeRepository.findAll()
                .stream()
                .map(aircraftTypeMapper::toAircraftTypeResponse)
                .toList();
    }

    public AircraftTypeResponse getAircraftTypeById(Long aircraftTypeId) {

        AircraftType aircraftType = aircraftTypeRepository.findById(aircraftTypeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bu id ile aircraft type bulunamadı"
                ));

        return aircraftTypeMapper.toAircraftTypeResponse(aircraftType);
    }

    @Transactional
    public AircraftTypeResponse updateAircraftType(Long aircraftTypeId, AircraftTypeRequest aircraftTypeRequest) {

        AircraftType updatedAircraftType = aircraftTypeRepository.findById(aircraftTypeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bu id ile aircraft type bulunamadı"
                ));

        String aircraftTypeIcaoCode = aircraftTypeRequest.getAircraftTypeIcaoCode();

        if (aircraftTypeRepository.existsByAircraftTypeIcaoCodeIgnoreCaseAndAircraftTypeIdNot(aircraftTypeIcaoCode, aircraftTypeId)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Aircraft type already exists"
            );
        }

        updatedAircraftType.setAircraftTypeManufacturer(aircraftTypeRequest.getAircraftTypeManufacturer());
        updatedAircraftType.setAircraftTypeModel(aircraftTypeRequest.getAircraftTypeModel());
        updatedAircraftType.setAircraftTypeIcaoCode(aircraftTypeIcaoCode);
        updatedAircraftType.setAircraftTypeCategory(aircraftTypeRequest.getAircraftTypeCategory());
        updatedAircraftType.setAircraftTypeStatus(aircraftTypeRequest.getAircraftTypeStatus());

        AircraftType savedAircraftType = aircraftTypeRepository.saveAndFlush(updatedAircraftType);

        ReferenceEvent referenceEvent = new ReferenceEvent();
        referenceEvent.setOccurredAt(Instant.now());
        referenceEvent.setResourceType(ReferenceResourceType.AIRCRAFT_TYPE);
        referenceEvent.setEventType(ReferenceEventType.UPDATED);
        referenceEvent.setEventId(UUID.randomUUID());
        referenceEvent.setResourceId(savedAircraftType.getAircraftTypeId());

        publisher.publish(referenceEvent);

        return aircraftTypeMapper.toAircraftTypeResponse(savedAircraftType);
    }

    @Transactional
    public void deactivateAircraftType(Long aircraftTypeId) {

        AircraftType aircraftType = aircraftTypeRepository.findById(aircraftTypeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bu id ile aircraft type bulunamadı"
                ));

        if (aircraftType.getAircraftTypeStatus() == AircraftTypeStatus.INACTIVE) {
            return;
        }

        aircraftType.setAircraftTypeStatus(AircraftTypeStatus.INACTIVE);

        aircraftTypeRepository.save(aircraftType);

        ReferenceEvent referenceEvent = new ReferenceEvent();
        referenceEvent.setOccurredAt(Instant.now());
        referenceEvent.setResourceType(ReferenceResourceType.AIRCRAFT_TYPE);
        referenceEvent.setEventType(ReferenceEventType.DEACTIVATED);
        referenceEvent.setEventId(UUID.randomUUID());
        referenceEvent.setResourceId(aircraftType.getAircraftTypeId());

        publisher.publish(referenceEvent);
    }

}
