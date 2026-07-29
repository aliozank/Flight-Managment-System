package com.alikaracor.learning.referencemanager.service;

import com.alikaracor.learning.referencemanager.dto.FlightTypeRequest;
import com.alikaracor.learning.referencemanager.dto.FlightTypeResponse;
import com.alikaracor.learning.referencemanager.event.ReferenceEvent;
import com.alikaracor.learning.referencemanager.event.ReferenceEventType;
import com.alikaracor.learning.referencemanager.event.ReferenceResourceType;
import com.alikaracor.learning.referencemanager.mapper.FlightTypeMapper;
import com.alikaracor.learning.referencemanager.model.FlightType;
import com.alikaracor.learning.referencemanager.model.FlightTypeStatus;
import com.alikaracor.learning.referencemanager.publisher.ReferenceEventPublisher;
import com.alikaracor.learning.referencemanager.repository.FlightTypeRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class FlightTypeService {

    private final FlightTypeRepository flightTypeRepository;
    private final FlightTypeMapper flightTypeMapper;
    private final ReferenceEventPublisher publisher;

    public FlightTypeService(FlightTypeRepository flightTypeRepository, FlightTypeMapper flightTypeMapper, ReferenceEventPublisher publisher) {
        this.flightTypeRepository = flightTypeRepository;
        this.flightTypeMapper = flightTypeMapper;
        this.publisher = publisher;
    }

    @Transactional
    public FlightTypeResponse addFlightType(FlightTypeRequest flightTypeRequest) {

        String flightTypeName = flightTypeRequest.getFlightTypeName().trim();
        String flightTypeCode = flightTypeRequest.getFlightTypeCode();

        if (flightTypeRepository.existsByFlightTypeNameIgnoreCase(flightTypeName)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Flight type name already exists"
            );
        }

        if (flightTypeRepository.existsByFlightTypeCodeIgnoreCase(flightTypeCode)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Flight type code already exists"
            );
        }

        FlightType newFlightType = flightTypeMapper.toFlightType(flightTypeRequest);
        newFlightType.setFlightTypeName(flightTypeName);

        FlightType savedFlightType = flightTypeRepository.save(newFlightType);

        ReferenceEvent referenceEvent = new ReferenceEvent();
        referenceEvent.setOccurredAt(Instant.now());
        referenceEvent.setResourceType(ReferenceResourceType.FLIGHT_TYPE);
        referenceEvent.setEventType(ReferenceEventType.CREATED);
        referenceEvent.setEventId(UUID.randomUUID());
        referenceEvent.setResourceId(savedFlightType.getFlightTypeId());

        publisher.publish(referenceEvent);

        return flightTypeMapper.toFlightTypeResponse(savedFlightType);
    }

    public List<FlightTypeResponse> getAllFlightTypes() {

        return flightTypeRepository.findAll()
                .stream()
                .map(flightTypeMapper::toFlightTypeResponse)
                .toList();
    }

    public List<FlightTypeResponse> getActiveFlightTypes() {

        return flightTypeRepository
                .findAllByFlightTypeStatusOrderByFlightTypeNameAsc(FlightTypeStatus.ACTIVE)
                .stream()
                .map(flightTypeMapper::toFlightTypeResponse)
                .toList();
    }

    public FlightTypeResponse getFlightTypeById(Long flightTypeId) {

        FlightType flightType = flightTypeRepository.findById(flightTypeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bu id ile flight type bulunamadı"
                ));

        return flightTypeMapper.toFlightTypeResponse(flightType);
    }

    @Transactional
    public FlightTypeResponse updateFlightTypeById(Long flightTypeId, FlightTypeRequest flightTypeRequest) {

        FlightType updatedFlightType = flightTypeRepository.findById(flightTypeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bu id ile flight type bulunamadı"
                ));

        String flightTypeName = flightTypeRequest.getFlightTypeName().trim();
        String flightTypeCode = flightTypeRequest.getFlightTypeCode();

        if (flightTypeRepository.existsByFlightTypeNameIgnoreCaseAndFlightTypeIdNot(
                flightTypeName,
                flightTypeId
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Flight type name already exists"
            );
        }

        if (flightTypeRepository.existsByFlightTypeCodeIgnoreCaseAndFlightTypeIdNot(
                flightTypeCode,
                flightTypeId
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Flight type code already exists"
            );
        }

        updatedFlightType.setFlightTypeName(flightTypeName);
        updatedFlightType.setFlightTypeCode(flightTypeCode);
        updatedFlightType.setFlightTypeStatus(flightTypeRequest.getFlightTypeStatus());

        FlightType savedFlightType = flightTypeRepository.saveAndFlush(updatedFlightType);

        ReferenceEvent referenceEvent = new ReferenceEvent();
        referenceEvent.setOccurredAt(Instant.now());
        referenceEvent.setResourceType(ReferenceResourceType.FLIGHT_TYPE);
        referenceEvent.setEventType(ReferenceEventType.UPDATED);
        referenceEvent.setEventId(UUID.randomUUID());
        referenceEvent.setResourceId(savedFlightType.getFlightTypeId());

        publisher.publish(referenceEvent);

        return flightTypeMapper.toFlightTypeResponse(savedFlightType);
    }

    @Transactional
    public void deactivateFlightTypeById(Long flightTypeId) {

        FlightType flightType = flightTypeRepository.findById(flightTypeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bu id ile flight type bulunamadı"
                ));

        if (flightType.getFlightTypeStatus() == FlightTypeStatus.INACTIVE) {
            return;
        }

        flightType.setFlightTypeStatus(FlightTypeStatus.INACTIVE);

        flightTypeRepository.save(flightType);

        ReferenceEvent referenceEvent = new ReferenceEvent();
        referenceEvent.setOccurredAt(Instant.now());
        referenceEvent.setResourceType(ReferenceResourceType.FLIGHT_TYPE);
        referenceEvent.setEventType(ReferenceEventType.DEACTIVATED);
        referenceEvent.setEventId(UUID.randomUUID());
        referenceEvent.setResourceId(flightType.getFlightTypeId());

        publisher.publish(referenceEvent);
    }
}
