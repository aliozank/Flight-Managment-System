package com.alikaracor.learning.flightservice.service;

import com.alikaracor.learning.flightservice.client.dto.AirportReferenceResponse;
import com.alikaracor.learning.flightservice.dto.FlightCreateRequest;
import com.alikaracor.learning.flightservice.dto.FlightResponse;
import com.alikaracor.learning.flightservice.dto.FlightStatusUpdateRequest;
import com.alikaracor.learning.flightservice.dto.FlightUpdateRequest;
import com.alikaracor.learning.flightservice.event.FlightEventType;
import com.alikaracor.learning.flightservice.mapper.FlightMapper;
import com.alikaracor.learning.flightservice.model.Flight;
import com.alikaracor.learning.flightservice.model.FlightChangeType;
import com.alikaracor.learning.flightservice.model.FlightStatus;
import com.alikaracor.learning.flightservice.model.FlightVersion;
import com.alikaracor.learning.flightservice.publisher.FlightEventPublisher;
import com.alikaracor.learning.flightservice.publisher.FlightWebSocketPublisher;
import com.alikaracor.learning.flightservice.repository.FlightRepository;
import com.alikaracor.learning.flightservice.repository.FlightVersionRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;


@Service
public class FlightService {

    private final FlightRepository flightRepository;
    private final FlightVersionRepository flightVersionRepository;
    private final FlightMapper flightMapper;
    private final FlightReferenceValidationService flightReferenceValidationService;
    private final AircraftScheduleConflictValidationService aircraftScheduleConflictValidationService;
    private final ActivityLogService activityLogService;
    private final FlightEventPublisher flightEventPublisher;
    private final FlightWebSocketPublisher flightWebSocketPublisher;


    public FlightService(FlightRepository flightRepository, FlightVersionRepository flightVersionRepository, FlightMapper flightMapper, FlightReferenceValidationService flightReferenceValidationService, AircraftScheduleConflictValidationService aircraftScheduleConflictValidationService, ActivityLogService activityLogService, FlightEventPublisher flightEventPublisher, FlightWebSocketPublisher flightWebSocketPublisher) {
        this.flightRepository = flightRepository;
        this.flightVersionRepository = flightVersionRepository;
        this.flightMapper = flightMapper;
        this.flightReferenceValidationService = flightReferenceValidationService;
        this.aircraftScheduleConflictValidationService = aircraftScheduleConflictValidationService;
        this.activityLogService = activityLogService;
        this.flightEventPublisher = flightEventPublisher;
        this.flightWebSocketPublisher = flightWebSocketPublisher;
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

            applyScheduleInstants(newFlight);

            aircraftScheduleConflictValidationService.validateAircraftScheduleForCreate(
                    newFlight.getAircraftId(),
                    newFlight.getScheduledDepartureAt(),
                    newFlight.getScheduledArrivalAt()
            );

            Flight savedFlight = flightRepository.save(newFlight);

            FlightVersion newFlightVersion = flightMapper.toFlightVersion(savedFlight);

            newFlightVersion.setFlightChangeType(FlightChangeType.CREATED);
            newFlightVersion.setChangedByUserId(actorUserId);

            flightVersionRepository.save(newFlightVersion);

            activityLogService.logFlightCreated(actorUserId, savedFlight.getFlightId(), ipAddress);

            flightEventPublisher.publish(savedFlight, FlightEventType.CREATED, actorUserId);

            FlightResponse flightResponse = flightMapper.toFlightResponse(savedFlight);

            flightWebSocketPublisher.publish(flightResponse);

            return flightResponse;

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
    public FlightResponse updateFlightStatusAutomatically(Long flightId, FlightStatus targetStatus) {

        FlightStatusUpdateRequest request = new FlightStatusUpdateRequest();
        request.setFlightStatus(targetStatus);

        return updateFlightStatus(flightId, request, null, null);

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

        if (updateFlight.getFlightStatus() != FlightStatus.SCHEDULED && updateFlight.getFlightStatus() != FlightStatus.DELAYED) {

            activityLogService.logFlightUpdateFailure(actorUserId, flightId, "Flight status is not suitable for update", ipAddress);

            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Flight status is not suitable for update");

        }

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

            applyScheduleInstants(updateFlight);

            aircraftScheduleConflictValidationService.validateAircraftScheduleForUpdate(
                    flightId,
                    updateFlight.getAircraftId(),
                    updateFlight.getScheduledDepartureAt(),
                    updateFlight.getScheduledArrivalAt()
            );

            flightRepository.save(updateFlight);

            FlightVersion updatedFlightVersion = flightMapper.toFlightVersion(updateFlight);

            updatedFlightVersion.setFlightChangeType(FlightChangeType.UPDATED);
            updatedFlightVersion.setChangedByUserId(actorUserId);
            flightVersionRepository.save(updatedFlightVersion);

            activityLogService.logFlightUpdated(actorUserId, updateFlight.getFlightId(), ipAddress);

            flightEventPublisher.publish(updateFlight, FlightEventType.UPDATED, actorUserId);

            FlightResponse flightResponse = flightMapper.toFlightResponse(updateFlight);

            flightWebSocketPublisher.publish(flightResponse);

            return flightResponse;

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

            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Bu uçuş zaten iptal edilmiş");
        }

        if (cancelFlight.getFlightStatus().equals(FlightStatus.DEPARTED)) {

            activityLogService.logFlightCancelFailure(actorUserId, flightId, "Bu uçuş zaten kalkmış neyi iptal ediyon", ipAddress);

            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Bu uçuş zaten kalkmış iptal edilemez");

        }

        if (cancelFlight.getFlightStatus().equals(FlightStatus.ARRIVED)) {
            activityLogService.logFlightCancelFailure(actorUserId, flightId, "Bu uçuş zaten gerçeklmiş neyi iptal ediyon", ipAddress);

            throw new ResponseStatusException(HttpStatus.CONFLICT,
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

            FlightResponse flightResponse = flightMapper.toFlightResponse(cancelFlight);

            flightWebSocketPublisher.publish(flightResponse);

        } catch (RuntimeException exception) {
            String failureReason = "Flight cancellation failed";

            if (exception instanceof ResponseStatusException responseStatusException && responseStatusException.getReason() != null) {
                failureReason = responseStatusException.getReason();
            }
            activityLogService.logFlightCancelFailure(actorUserId, cancelFlight.getFlightId(), failureReason, ipAddress);
            throw exception;
        }

    }


    private boolean isStatusTransitionAllowed(FlightStatus currentStatus, FlightStatus targetStatus) {

        return switch (currentStatus) {

            case SCHEDULED -> targetStatus == FlightStatus.DELAYED || targetStatus == FlightStatus.DEPARTED;

            case DELAYED -> targetStatus == FlightStatus.SCHEDULED || targetStatus == FlightStatus.DEPARTED;

            case DEPARTED -> targetStatus == FlightStatus.ARRIVED;

            case ARRIVED, CANCELLED -> false;
        };

    }


    @Transactional
    public FlightResponse updateFlightStatus(Long flightId, FlightStatusUpdateRequest flightUpdateRequestStatus, Long actorUserId, String ipAddress) {

        boolean automaticUpdate = actorUserId == null;

        Flight oldFlight = flightRepository.findById(flightId)
                .orElseThrow(() -> {

                            if (automaticUpdate) {

                                activityLogService.logAutomaticFlightStatusUpdateFailure(flightId, "Flight not found");

                            } else {

                                activityLogService.logFlightUpdateFailure(actorUserId, flightId, "Flight not found", ipAddress);

                            }

                            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight with id " + flightId + " not found.");

                        }
                );

        FlightStatus targetStatus = flightUpdateRequestStatus.getFlightStatus();

        FlightStatus oldStatus = oldFlight.getFlightStatus();

        if (oldStatus.equals(targetStatus)) {

            return flightMapper.toFlightResponse(oldFlight);

        }


        try {

            if (targetStatus == FlightStatus.CANCELLED) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Flight cancellation must use the cancellation endpoint"
                );
            }

            if (!isStatusTransitionAllowed(oldStatus, targetStatus)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Flight status cannot transition from " + oldStatus + " to " + targetStatus);
            }

            oldFlight.setFlightStatus(targetStatus);
            oldFlight.setFlightUpdatedAt(Instant.now());
            oldFlight.setFlightVersion(oldFlight.getFlightVersion() + 1);

            flightRepository.save(oldFlight);

            FlightVersion updatedFlightVersion = flightMapper.toFlightVersion(oldFlight);

            updatedFlightVersion.setFlightChangeType(FlightChangeType.UPDATED);
            updatedFlightVersion.setChangedByUserId(actorUserId);

            flightVersionRepository.save(updatedFlightVersion);

            if (automaticUpdate) {

                activityLogService.logAutomaticFlightStatusUpdated(oldFlight.getFlightId());

            } else {

                activityLogService.logFlightUpdated(actorUserId, oldFlight.getFlightId(), ipAddress);

            }


            flightEventPublisher.publish(oldFlight, FlightEventType.UPDATED, actorUserId);

            FlightResponse flightResponse = flightMapper.toFlightResponse(oldFlight);

            flightWebSocketPublisher.publish(flightResponse);

            return flightResponse;

        } catch (RuntimeException exception) {

            String failureReason = "Flight status update failed";

            if (exception instanceof ResponseStatusException responseStatusException && responseStatusException.getReason() != null) {

                failureReason = responseStatusException.getReason();
            }

            if (automaticUpdate) {

                activityLogService.logAutomaticFlightStatusUpdateFailure(oldFlight.getFlightId(), failureReason);

            }

            else {

                activityLogService.logFlightUpdateFailure(actorUserId, oldFlight.getFlightId(), failureReason, ipAddress);

            }



            throw exception;
        }

    }

    private void applyScheduleInstants(Flight flight) {

        AirportReferenceResponse originAirport = flightReferenceValidationService.validateAirport(flight.getOriginAirportId());

        AirportReferenceResponse destinationAirport = flightReferenceValidationService.validateAirport(flight.getDestinationAirportId());

        try {
            ZoneId originZone = ZoneId.of(originAirport.getAirportTimezone());

            ZoneId destinationZone = ZoneId.of(destinationAirport.getAirportTimezone());

            LocalDateTime departureLocal = LocalDateTime.of(
                    flight.getFlightDate(),
                    flight.getScheduledDepartureTime()
            );

            LocalDateTime arrivalLocal = LocalDateTime.of(
                    flight.getScheduledArrivalDate(),
                    flight.getScheduledArrivalTime()
            );

            Instant departureAt = departureLocal.atZone(originZone).toInstant();

            Instant arrivalAt = arrivalLocal.atZone(destinationZone).toInstant();

            if (!arrivalAt.isAfter(departureAt)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Scheduled arrival must be after scheduled departure"
                );
            }

            flight.setScheduledDepartureAt(departureAt);
            flight.setScheduledArrivalAt(arrivalAt);

        } catch (DateTimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Airport timezone is invalid",
                    exception
            );
        }
    }
}
