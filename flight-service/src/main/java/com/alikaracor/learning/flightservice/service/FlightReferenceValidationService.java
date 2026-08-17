package com.alikaracor.learning.flightservice.service;

import com.alikaracor.learning.flightservice.client.ReferenceManagerClient;
import com.alikaracor.learning.flightservice.client.dto.*;
import com.alikaracor.learning.flightservice.dto.FlightCreateRequest;
import com.alikaracor.learning.flightservice.dto.FlightUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FlightReferenceValidationService {

    private final ReferenceManagerClient referenceManagerClient;

    public FlightReferenceValidationService(ReferenceManagerClient referenceManagerClient) {
        this.referenceManagerClient = referenceManagerClient;
    }

    public AirlineReferenceResponse validateAirline(Long airlineId) {

        AirlineReferenceResponse airline = referenceManagerClient.getAirlineById(airlineId);

        if (!"ACTIVE".equalsIgnoreCase(airline.getAirlineStatus())) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Airline status is not ACTIVE");

        }

        return airline;

    }

    public void validateAircraftType(Long aircraftTypeId) {

        AircraftTypeReferenceResponse aircraftType = referenceManagerClient.getAircraftTypeById(aircraftTypeId);

        if (!"ACTIVE".equalsIgnoreCase(aircraftType.getAircraftTypeStatus())) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Aircraft type status is not ACTIVE");
        }

    }

    public AirportReferenceResponse validateAirport(Long airportId) {
        AirportReferenceResponse airport = referenceManagerClient.getAirportById(airportId);

        if (!"OPERATIONAL".equalsIgnoreCase(airport.getAirportStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Airport status is not OPERATIONAL");
        }

        return airport;

    }

    public void validateFlightType(Long flightTypeId) {
        FlightTypeReferenceResponse flightType = referenceManagerClient.getFlightTypeById(flightTypeId);

        if (!"ACTIVE".equalsIgnoreCase(flightType.getFlightTypeStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Flight type status is not ACTIVE");
        }
    }

    public void validateAircraft(Long aircraftId, Long selectedAircraftTypeId, Long selectedAirlineId) {

        if (aircraftId == null) {
            return;
        }

        AircraftReferenceResponse aircraft = referenceManagerClient.getAircraftById(aircraftId);

        if (!"ACTIVE".equalsIgnoreCase(aircraft.getAircraftStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Aircraft status is not ACTIVE"
            );
        }

        if (!aircraft.getAircraftTypeId().equals(selectedAircraftTypeId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected aircraft does not belong to selected aircraft type"
            );
        }

        if (aircraft.getOperatorAirlineId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected aircraft is not assigned to an airline"
            );
        }

        if (!aircraft.getOperatorAirlineId().equals(selectedAirlineId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected aircraft does not belong to selected airline"
            );
        }

    }

    public void validateRoute(Long originAirportId, Long destinationAirportId) {

        if (originAirportId.equals(destinationAirportId)) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Varış ve kalkış havalimanı aynı olamaz ");
        }

        RouteReferenceResponse route = referenceManagerClient.getActiveRoute(originAirportId, destinationAirportId);

        if (!"ACTIVE".equalsIgnoreCase(route.getRouteStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Route status is not ACTIVE");
        }
    }

    public void validateCreateRequest(FlightCreateRequest request) {

        AirlineReferenceResponse airline = validateAirline(request.getAirlineId());

        validateFlightNumberMatchesAirline(request.getFlightNumber(), airline);

        validateAircraftType(request.getAircraftTypeId());

        validateFlightType(request.getFlightTypeId());

        validateAirport(request.getOriginAirportId());

        validateAirport(request.getDestinationAirportId());

        validateRoute(request.getOriginAirportId(), request.getDestinationAirportId());

        validateAircraft(request.getAircraftId(), request.getAircraftTypeId(), request.getAirlineId());

    }


    public void validateUpdateRequest(FlightUpdateRequest request) {

        AirlineReferenceResponse airline = validateAirline(request.getAirlineId());

        validateFlightNumberMatchesAirline(request.getFlightNumber(), airline);

        validateAircraftType(request.getAircraftTypeId());

        validateFlightType(request.getFlightTypeId());

        validateAirport(request.getOriginAirportId());

        validateAirport(request.getDestinationAirportId());

        validateRoute(request.getOriginAirportId(), request.getDestinationAirportId());

        validateAircraft(request.getAircraftId(), request.getAircraftTypeId(), request.getAirlineId());

    }


    private void validateFlightNumberMatchesAirline(String flightNumber, AirlineReferenceResponse airline) {

        if (flightNumber == null || flightNumber.length() < 2 || airline.getAirlineIataCode() == null || !flightNumber.substring(0, 2)
                .equalsIgnoreCase(airline.getAirlineIataCode())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Flight number must start with the selected airline's IATA code"

            );
        }
    }



}

