package com.alikaracor.learning.flightservice.service;

import com.alikaracor.learning.flightservice.client.ReferenceManagerClient;
import com.alikaracor.learning.flightservice.client.dto.AircraftReferenceResponse;
import com.alikaracor.learning.flightservice.client.dto.AirlineReferenceResponse;
import com.alikaracor.learning.flightservice.client.dto.FlightTypeReferenceResponse;
import com.alikaracor.learning.flightservice.client.dto.RouteReferenceResponse;
import com.alikaracor.learning.flightservice.dto.FlightCreateRequest;
import com.alikaracor.learning.flightservice.dto.FlightResponse;
import com.alikaracor.learning.flightservice.dto.MockFlightGenerationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
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


    public List<FlightResponse> generateFlights(MockFlightGenerationRequest request, Long performedByUserId, String clientIpAddress) {

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

        for (int i=0 ; i<request.getFlightCount(); i++) {

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

            LocalTime arrivalTime = departureTime.plusMinutes(durationMinutes);


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
            flightCreateRequest.setScheduledArrivalTime(arrivalTime);

            FlightResponse generatedFlight = flightService.addFlight(flightCreateRequest,performedByUserId,clientIpAddress);

            generatedFlights.add(generatedFlight);

        }

        return generatedFlights;

    }


}
