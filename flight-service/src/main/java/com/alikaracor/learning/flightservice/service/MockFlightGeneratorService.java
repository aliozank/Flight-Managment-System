package com.alikaracor.learning.flightservice.service;

import com.alikaracor.learning.flightservice.client.ReferenceManagerClient;
import com.alikaracor.learning.flightservice.client.dto.*;
import com.alikaracor.learning.flightservice.dto.FlightCreateRequest;
import com.alikaracor.learning.flightservice.dto.FlightResponse;
import com.alikaracor.learning.flightservice.dto.MockFlightGenerationRequest;
import com.alikaracor.learning.flightservice.dto.MockFlightGenerationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MockFlightGeneratorService {

    private final ReferenceManagerClient referenceManagerClient;

    private final FlightService flightService;


    public MockFlightGeneratorService(ReferenceManagerClient referenceManagerClient, FlightService flightService) {

        this.referenceManagerClient = referenceManagerClient;
        this.flightService = flightService;

    }


    public MockFlightGenerationResponse generateFlights(MockFlightGenerationRequest request, Long performedByUserId, String clientIpAddress) {

        List<AircraftReferenceResponse> activeAircrafts = referenceManagerClient.getAllAircrafts()
                .stream()
                .filter(aircraftReferenceResponse -> "ACTIVE".equalsIgnoreCase(aircraftReferenceResponse.getAircraftStatus()))
                .filter(aircraft -> aircraft.getOperatorAirlineId() != null)
                .filter(aircraft -> aircraft.getAircraftTypeId() != null)
                .toList();

        List<RouteReferenceResponse> activeRoutes = referenceManagerClient.getAllRoutes()
                .stream()
                .filter(routeReferenceResponse -> "ACTIVE".equalsIgnoreCase(routeReferenceResponse.getRouteStatus()))
                .toList();

        List<FlightTypeReferenceResponse> activeFlightTypes = referenceManagerClient.getAllFlightTypes()
                .stream()
                .filter(flightTypeReference -> "ACTIVE".equalsIgnoreCase(flightTypeReference.getFlightTypeStatus()))
                .toList();


        if (activeAircrafts.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mock flight oluşturmak için aktif aircraft bulunamadı"
            );
        }

        if (activeRoutes.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mock flight oluşturmak için aktif route bulunamadı"
            );
        }

        if (activeFlightTypes.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mock flight oluşturmak için aktif flight type bulunamadı"
            );
        }

        List<FlightResponse> generatedFlights = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i=0 ; i<request.getFlightCount(); i++) {

            try {
                AircraftReferenceResponse selectedAircraft = activeAircrafts.get(ThreadLocalRandom.current().nextInt(0, activeAircrafts.size()));

                RouteReferenceResponse selectedRoute = activeRoutes.get(ThreadLocalRandom.current().nextInt(0, activeRoutes.size()));

                FlightTypeReferenceResponse selectedFlightType = activeFlightTypes.get(ThreadLocalRandom.current().nextInt(0, activeFlightTypes.size()));

                AirlineReferenceResponse selectedAirline = referenceManagerClient.getAirlineById(selectedAircraft.getOperatorAirlineId());

                if (!"ACTIVE".equalsIgnoreCase(selectedAirline.getAirlineStatus())){

                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Seçilen aircraft aktif bir airline ye bağlı değil");

                }

                String iataCode = selectedAirline.getAirlineIataCode().toUpperCase(Locale.ROOT);

                String randomNumber = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));

                String flightNumber = iataCode + randomNumber;

                long randomFutureDay = ThreadLocalRandom.current().nextLong(1, request.getMaximumFutureDays()+1L);

                LocalDate flightDate = LocalDate.now().plusDays(randomFutureDay);

                int departureHour = ThreadLocalRandom.current().nextInt(19);    //TARİH PROBLEMİ NOT : 01
                int departureMinute = ThreadLocalRandom.current().nextInt(12) * 5;
                LocalTime departureTime = LocalTime.of(departureHour, departureMinute);

                long durationMinutes = ThreadLocalRandom.current().nextLong(12, 61) * 5;

                AirportReferenceResponse originAirport = referenceManagerClient.getAirportById(selectedRoute.getOriginAirportId());

                AirportReferenceResponse destinationAirport = referenceManagerClient.getAirportById(selectedRoute.getDestinationAirportId());

                ZoneId originZone = ZoneId.of(originAirport.getAirportTimezone());

                ZoneId destinationZone = ZoneId.of(destinationAirport.getAirportTimezone());

                ZonedDateTime departureDateTime = ZonedDateTime.of(flightDate, departureTime, originZone);

                ZonedDateTime arrivalDateTime = departureDateTime
                        .plusMinutes(durationMinutes)
                        .withZoneSameInstant(destinationZone);

                LocalDate arrivalDate = arrivalDateTime.toLocalDate();
                LocalTime arrivalTime = arrivalDateTime.toLocalTime();


                FlightCreateRequest  flightCreateRequest = new FlightCreateRequest();

                flightCreateRequest.setFlightNumber(flightNumber);
                flightCreateRequest.setAirlineId(selectedAircraft.getOperatorAirlineId());
                flightCreateRequest.setAircraftId(selectedAircraft.getAircraftId());
                flightCreateRequest.setAircraftTypeId(selectedAircraft.getAircraftTypeId());
                flightCreateRequest.setOriginAirportId(selectedRoute.getOriginAirportId());
                flightCreateRequest.setDestinationAirportId(selectedRoute.getDestinationAirportId());
                flightCreateRequest.setFlightTypeId(selectedFlightType.getFlightTypeId());
                flightCreateRequest.setFlightDate(flightDate);
                flightCreateRequest.setScheduledDepartureTime(departureTime);
                flightCreateRequest.setScheduledArrivalDate(arrivalDate);
                flightCreateRequest.setScheduledArrivalTime(arrivalTime);

                FlightResponse generatedFlight = flightService.addFlight(flightCreateRequest,performedByUserId,clientIpAddress);

                generatedFlights.add(generatedFlight);

            } catch (RuntimeException exception) {

                String failureReason = "Mock flight generation failed";

                if (exception instanceof ResponseStatusException responseStatusException
                        && responseStatusException.getReason() != null) {

                    failureReason = responseStatusException.getReason();

                } else if (exception.getMessage() != null && !exception.getMessage().isBlank()) {

                    failureReason = exception.getMessage();
                }

                errors.add("Mock uçuş denemesi " + (i + 1) + ": " + failureReason);
            }

        }

        MockFlightGenerationResponse response = new MockFlightGenerationResponse();

        response.setRequestedCount(request.getFlightCount());
        response.setSuccessfulCount(generatedFlights.size());
        response.setFailedCount(errors.size());
        response.setSuccessfulFlights(generatedFlights);
        response.setErrors(errors);

        return response;

    }


}
