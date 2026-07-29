package com.alikaracor.learning.referencemanager.controller;

import com.alikaracor.learning.referencemanager.dto.AircraftRequest;
import com.alikaracor.learning.referencemanager.dto.AircraftResponse;
import com.alikaracor.learning.referencemanager.service.AircraftService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aircrafts")
public class AircraftController {

    private final AircraftService aircraftService;

    public AircraftController(AircraftService aircraftService) {
        this.aircraftService = aircraftService;
    }

    @GetMapping
    public List<AircraftResponse> getAllAircrafts() {
        return aircraftService.getAllAircrafts();
    }

    @GetMapping("/{aircraftId}")
    public AircraftResponse getAircraftById(@PathVariable Long aircraftId) {
        return aircraftService.getAircraftById(aircraftId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AircraftResponse addAircraft(@RequestBody @Valid AircraftRequest aircraftRequest) {
        return aircraftService.addAircraft(aircraftRequest);
    }

    @PutMapping("/{aircraftId}")
    public AircraftResponse updateAircraftById(@RequestBody @Valid AircraftRequest aircraftRequest,@PathVariable Long aircraftId) {
        return aircraftService.updateAircraftById(aircraftId, aircraftRequest);
    }

    @DeleteMapping("/{aircraftId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactiveAircraftById(@PathVariable Long aircraftId) {
        aircraftService.deactiveAircraftById(aircraftId);
    }




}
