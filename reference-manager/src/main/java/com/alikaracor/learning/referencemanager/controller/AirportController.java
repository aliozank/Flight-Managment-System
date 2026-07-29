package com.alikaracor.learning.referencemanager.controller;

import com.alikaracor.learning.referencemanager.dto.AirportRequest;
import com.alikaracor.learning.referencemanager.dto.AirportResponse;
import com.alikaracor.learning.referencemanager.service.AirportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/airports")
public class AirportController {

    private final AirportService airportService;

    public AirportController(AirportService airportService) {
        this.airportService = airportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AirportResponse addAirport(@Valid @RequestBody AirportRequest airportRequest) {

        return airportService.addAirport(airportRequest);
    }

    @GetMapping
    public List<AirportResponse> getAllAirports() {

        return airportService.getAllAirports();
    }

    @GetMapping("/{airportId}")
    public AirportResponse getAirportById(@PathVariable Long airportId) {

        return airportService.getAirportById(airportId);
    }

    @GetMapping("/iata/{airportIataCode}")
    public AirportResponse getAirportByIataCode(@PathVariable String airportIataCode) {

        return airportService.getAirportByIataCode(airportIataCode);
    }

    @GetMapping("/icao/{airportIcaoCode}")
    public AirportResponse getAirportByIcaoCode(@PathVariable String airportIcaoCode) {

        return airportService.getAirportByIcaoCode(airportIcaoCode);
    }


    @PutMapping("/{airportId}")
    public AirportResponse updateAirportById(
            @PathVariable Long airportId,
            @Valid @RequestBody AirportRequest airportRequest
    ) {

        return airportService.updateAirport(airportId, airportRequest);
    }

    @DeleteMapping("/{airportId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateAirportById(@PathVariable Long airportId) {

        airportService.deactivateAirport(airportId);
    }
}
