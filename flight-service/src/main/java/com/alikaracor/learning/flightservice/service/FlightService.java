package com.alikaracor.learning.flightservice.service;

import com.alikaracor.learning.flightservice.dto.FlightCreateRequest;
import com.alikaracor.learning.flightservice.dto.FlightResponse;
import com.alikaracor.learning.flightservice.dto.FlightUpdateRequest;
import com.alikaracor.learning.flightservice.event.FlightEventType;
import com.alikaracor.learning.flightservice.mapper.FlightMapper;
import com.alikaracor.learning.flightservice.model.Flight;
import com.alikaracor.learning.flightservice.model.FlightChangeType;
import com.alikaracor.learning.flightservice.model.FlightStatus;
import com.alikaracor.learning.flightservice.model.FlightVersion;
import com.alikaracor.learning.flightservice.publisher.FlightEventPublisher;
import com.alikaracor.learning.flightservice.repository.FlightRepository;
import com.alikaracor.learning.flightservice.repository.FlightVersionRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class FlightService {

    private final FlightRepository flightRepository;
    private final FlightVersionRepository flightVersionRepository;
    private final FlightMapper flightMapper;
    private final FlightReferenceValidationService flightReferenceValidationService;
    private final ActivityLogService activityLogService;
    private final FlightEventPublisher flightEventPublisher;


    public FlightService(FlightRepository flightRepository, FlightVersionRepository flightVersionRepository, FlightMapper flightMapper, FlightReferenceValidationService flightReferenceValidationService, ActivityLogService activityLogService, FlightEventPublisher flightEventPublisher) {
        this.flightRepository = flightRepository;
        this.flightVersionRepository = flightVersionRepository;
        this.flightMapper = flightMapper;
        this.flightReferenceValidationService = flightReferenceValidationService;
        this.activityLogService = activityLogService;
        this.flightEventPublisher = flightEventPublisher;
    }

    public List<FlightResponse> getAllFlights() {

        return flightRepository.findAll()
                .stream()
                .map(flightMapper::toFlightResponse)
                .toList();

    }

    public FlightResponse getFlightById(Long flightId) {

        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Flight with id " + flightId + " not found."
                ));

        return flightMapper.toFlightResponse(flight);

    }

    @Transactional
    public FlightResponse addFlight(FlightCreateRequest flightCreateRequest, Long actorUserId, String ipAddress) {


        if (flightRepository.existsByFlightNumberAndFlightDate(flightCreateRequest.getFlightNumber(), flightCreateRequest.getFlightDate())) {

            activityLogService.logFlightCreateFailure(actorUserId, "Flight already exists", ipAddress);

            throw new ResponseStatusException(HttpStatus.CONFLICT, "Flight already exists");

        }

        try {
            flightReferenceValidationService.validateCreateRequest(flightCreateRequest);

            Flight newFlight = flightMapper.toFlight(flightCreateRequest);

            newFlight.setFlightStatus(FlightStatus.SCHEDULED);
            newFlight.setFlightVersion(1);

            Flight savedFlight = flightRepository.save(newFlight);

            FlightVersion newFlightVersion = flightMapper.toFlightVersion(savedFlight);

            newFlightVersion.setFlightChangeType(FlightChangeType.CREATED);
            newFlightVersion.setChangedByUserId(actorUserId);

            flightVersionRepository.save(newFlightVersion);

            activityLogService.logFlightCreated(actorUserId, savedFlight.getFlightId(), ipAddress);

            flightEventPublisher.publish(savedFlight, FlightEventType.CREATED, actorUserId);

            return flightMapper.toFlightResponse(savedFlight);

        } catch (RuntimeException exception) {

            String failureReason = "Flight creation failed";

            if (exception instanceof ResponseStatusException responseStatusException
                    && responseStatusException.getReason() != null) {

                failureReason = responseStatusException.getReason();
            }

            activityLogService.logFlightCreateFailure(
                    actorUserId,
                    failureReason,
                    ipAddress
            );

            throw exception;
        }

    }

    @Transactional
    public FlightResponse updateFlight(Long flightId, FlightUpdateRequest flightUpdateRequest, Long actorUserId, String ipAddress) {

        Flight updateFlight = flightRepository.findById(flightId)
                .orElseThrow(() -> {
                            activityLogService.logFlightUpdateFailure(actorUserId, flightId, "Bu id ile uçuş mevcut değil", ipAddress);
                            return new ResponseStatusException(HttpStatus.NOT_FOUND,
                                    "Flight with id " + flightId + " not found.");
                        }
                );

        if (flightRepository.existsByFlightNumberAndFlightDateAndFlightIdNot(flightUpdateRequest.getFlightNumber(), flightUpdateRequest.getFlightDate(), flightId)) {

            activityLogService.logFlightUpdateFailure(actorUserId, flightId, "Flight already exists", ipAddress);

            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Flight already exists");

        }


        try {

            flightReferenceValidationService.validateUpdateRequest(flightUpdateRequest);

            flightMapper.updateFlight(flightUpdateRequest, updateFlight);

            updateFlight.setFlightUpdatedAt(Instant.now());

            updateFlight.setFlightVersion(updateFlight.getFlightVersion() + 1);

            flightRepository.save(updateFlight);

            FlightVersion updatedFlightVersion = flightMapper.toFlightVersion(updateFlight);

            updatedFlightVersion.setFlightChangeType(FlightChangeType.UPDATED);
            updatedFlightVersion.setChangedByUserId(actorUserId);
            flightVersionRepository.save(updatedFlightVersion);

            activityLogService.logFlightUpdated(actorUserId, updateFlight.getFlightId(), ipAddress);

            flightEventPublisher.publish(updateFlight, FlightEventType.UPDATED, actorUserId);

            return flightMapper.toFlightResponse(updateFlight);
        } catch (RuntimeException exception) {

            String failureReason = "Flight update failed";
            if (exception instanceof ResponseStatusException responseStatusException && responseStatusException.getReason() != null) {

                failureReason = responseStatusException.getReason();

            }

            activityLogService.logFlightUpdateFailure(
                    actorUserId,
                    updateFlight.getFlightId(),
                    failureReason,
                    ipAddress
            );

            throw exception;

        }

    }

    @Transactional
    public void cancelFlight(Long flightId, Long actorUserId, String ipAddress) {

        Flight cancelFlight = flightRepository.findById(flightId)
                .orElseThrow(() -> {
                            activityLogService.logFlightCancelFailure(actorUserId, flightId, "Bu id ile uçuş mevcut değil", ipAddress);
                            return new ResponseStatusException(HttpStatus.NOT_FOUND,
                                    "Bu id ile uçuş bulunamadı");
                        }
                );

        if (cancelFlight.getFlightStatus().equals(FlightStatus.CANCELLED)) {
            activityLogService.logFlightCancelFailure(actorUserId, flightId, "Bu uçuş zaten iptal edilmiş", ipAddress);

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Bu uçuş zaten iptal edilmiş");
        }

        if (cancelFlight.getFlightStatus().equals(FlightStatus.ARRIVED)) {
            activityLogService.logFlightCancelFailure(actorUserId,flightId,"Bu uçuş zaten gerçeklmiş neyi iptal ediyon", ipAddress);

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Bu uçuş zaten gerçeklmiş iptal edilemez");

        }

        try {

            cancelFlight.setFlightStatus(FlightStatus.CANCELLED);

            cancelFlight.setFlightVersion(cancelFlight.getFlightVersion() + 1);

            cancelFlight.setFlightUpdatedAt(Instant.now());

            flightRepository.save(cancelFlight);

            FlightVersion cancelFlightVersion = flightMapper.toFlightVersion(cancelFlight);

            cancelFlightVersion.setFlightChangeType(FlightChangeType.CANCELLED);
            cancelFlightVersion.setChangedByUserId(actorUserId);

            flightVersionRepository.save(cancelFlightVersion);

            activityLogService.logFlightCancel(actorUserId, cancelFlight.getFlightId(), ipAddress);

            flightEventPublisher.publish(cancelFlight, FlightEventType.CANCELLED, actorUserId);

        } catch (RuntimeException exception) {
            String failureReason = "Flight cancellation failed";

            if (exception instanceof ResponseStatusException responseStatusException && responseStatusException.getReason() != null) {
                failureReason = responseStatusException.getReason();
            }
            activityLogService.logFlightCancelFailure(actorUserId, cancelFlight.getFlightId(), failureReason, ipAddress);
            throw exception;
        }

    }


}


