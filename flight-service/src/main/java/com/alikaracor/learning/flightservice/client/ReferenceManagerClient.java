package com.alikaracor.learning.flightservice.client;

import com.alikaracor.learning.flightservice.client.dto.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ReferenceManagerClient {

    private final RestClient referenceManagerRestClient;

    public ReferenceManagerClient(RestClient referenceManagerRestClient) {
        this.referenceManagerRestClient = referenceManagerRestClient;
    }

    @Cacheable(cacheNames = "airlines", key = "#airlineId")
    public AirlineReferenceResponse getAirlineById(Long airlineId) {

        return referenceManagerRestClient
                .get()
                .uri("/api/airlines/{airlineId}", airlineId)
                .retrieve()
                .body(AirlineReferenceResponse.class);
    }

    @Cacheable(cacheNames = "aircrafts", key = "#aircraftId")
    public AircraftReferenceResponse getAircraftById(Long aircraftId) {

        return referenceManagerRestClient
                .get()
                .uri("/api/aircrafts/{aircraftId}", aircraftId)
                .retrieve()
                .body(AircraftReferenceResponse.class);

    }

    @Cacheable(cacheNames = "aircraftTypes", key = "#aircraftTypeId")
    public AircraftTypeReferenceResponse getAircraftTypeById(Long aircraftTypeId) {

        return referenceManagerRestClient
                .get()
                .uri("/api/aircraft-types/{aircraftTypeId}",  aircraftTypeId)
                .retrieve()
                .body(AircraftTypeReferenceResponse.class);
    }

    @Cacheable(cacheNames = "airports", key = "#airportId")
    public AirportReferenceResponse getAirportById(Long airportId) {

        return referenceManagerRestClient
                .get()
                .uri("/api/airports/{airportId}",  airportId)
                .retrieve()
                .body(AirportReferenceResponse.class);
    }

    @Cacheable(cacheNames = "flightTypes", key = "#flightTypeId")
    public FlightTypeReferenceResponse getFlightTypeById(Long flightTypeId) {

        return referenceManagerRestClient
                .get()
                .uri("/api/flight-types/{flightTypeId}", flightTypeId)
                .retrieve()
                .body(FlightTypeReferenceResponse.class);
    }

    @Cacheable(cacheNames = "routes" , key = "#originAirportId + ':' + #destinationAirportId" )
    public RouteReferenceResponse getActiveRoute(Long originAirportId, Long destinationAirportId) {

        return referenceManagerRestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/routes/by-airports")
                        .queryParam("originAirportId", originAirportId)
                        .queryParam("destinationAirportId", destinationAirportId)
                        .build())
                .retrieve()
                .body(RouteReferenceResponse.class);
    }

}
