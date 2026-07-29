package com.alikaracor.learning.referencemanager.controller;

import com.alikaracor.learning.referencemanager.dto.FlightTypeRequest;
import com.alikaracor.learning.referencemanager.dto.FlightTypeResponse;
import com.alikaracor.learning.referencemanager.service.FlightTypeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flight-types")
public class FlightTypeController {

    private final FlightTypeService flightTypeService;

    public FlightTypeController(FlightTypeService flightTypeService) {
        this.flightTypeService = flightTypeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FlightTypeResponse addFlightType(@Valid @RequestBody FlightTypeRequest flightTypeRequest) {
        return flightTypeService.addFlightType(flightTypeRequest);
    }

    @GetMapping
    public List<FlightTypeResponse> getAllFlightTypes() {
        return flightTypeService.getAllFlightTypes();
    }

    @GetMapping("/active")
    public List<FlightTypeResponse> getActiveFlightTypes() {
        return flightTypeService.getActiveFlightTypes();
    }

    @GetMapping("/{flightTypeId}")
    public FlightTypeResponse getFlightTypeById(@PathVariable Long flightTypeId) {
        return flightTypeService.getFlightTypeById(flightTypeId);
    }

    @PutMapping("/{flightTypeId}")
    public FlightTypeResponse updateFlightTypeById(
            @PathVariable Long flightTypeId,
            @Valid @RequestBody FlightTypeRequest flightTypeRequest
    ) {
        return flightTypeService.updateFlightTypeById(flightTypeId, flightTypeRequest);
    }

    @DeleteMapping("/{flightTypeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateFlightTypeById(@PathVariable Long flightTypeId) {
        flightTypeService.deactivateFlightTypeById(flightTypeId);
    }
}
