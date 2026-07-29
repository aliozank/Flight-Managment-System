package com.alikaracor.learning.referencemanager.controller;


import com.alikaracor.learning.referencemanager.dto.AirlineRequest;
import com.alikaracor.learning.referencemanager.dto.AirlineResponse;
import com.alikaracor.learning.referencemanager.service.AirlineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/airlines")
public class AirlineController {

    private final AirlineService airlineService;

    public AirlineController(AirlineService airlineService) {
        this.airlineService = airlineService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AirlineResponse addAirline(@Valid @RequestBody AirlineRequest airlineRequest) {

        return airlineService.addAirline(airlineRequest);

    }

    @GetMapping
    public List<AirlineResponse> findAllAirlines() {

        return airlineService.getAllAirlines();

    }

    @GetMapping("/{airlineId}")
    public AirlineResponse findById(@PathVariable Long airlineId) {

        return airlineService.getAirlineById(airlineId);

    }

    @PutMapping("/{airlineId}")
    @ResponseStatus(HttpStatus.OK)
    public AirlineResponse updateAirlineById(@PathVariable Long airlineId, @RequestBody @Valid AirlineRequest airlineRequest) {

        return airlineService.updateAirline(airlineId, airlineRequest);

    }

    @DeleteMapping("/{airlineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactiveById(@PathVariable Long airlineId) {

        airlineService.deactivateAirline(airlineId);

    }

}
