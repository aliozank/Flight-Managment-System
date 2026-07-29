package com.alikaracor.learning.referencemanager.controller;

import com.alikaracor.learning.referencemanager.dto.AircraftTypeRequest;
import com.alikaracor.learning.referencemanager.dto.AircraftTypeResponse;
import com.alikaracor.learning.referencemanager.service.AircraftTypeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aircraft-types")
public class AircraftTypeController {

    private final AircraftTypeService aircraftTypeService;

    public AircraftTypeController(AircraftTypeService aircraftTypeService) {
        this.aircraftTypeService = aircraftTypeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AircraftTypeResponse addAircraftType(@Valid @RequestBody AircraftTypeRequest aircraftTypeRequest) {

        return aircraftTypeService.addAircraftType(aircraftTypeRequest);
    }

    @GetMapping
    public List<AircraftTypeResponse> getAllAircraftTypes() {

        return aircraftTypeService.getAllAircraftTypes();
    }

    @GetMapping("/{aircraftTypeId}")
    public AircraftTypeResponse getAircraftTypeById(@PathVariable Long aircraftTypeId) {

        return aircraftTypeService.getAircraftTypeById(aircraftTypeId);
    }

    @PutMapping("/{aircraftTypeId}")
    public AircraftTypeResponse updateAircraftType(@PathVariable Long aircraftTypeId, @Valid @RequestBody AircraftTypeRequest aircraftTypeRequest
    ) {

        return aircraftTypeService.updateAircraftType(
                aircraftTypeId,
                aircraftTypeRequest
        );
    }

    @DeleteMapping("/{aircraftTypeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateAircraftType(@PathVariable Long aircraftTypeId) {

        aircraftTypeService.deactivateAircraftType(aircraftTypeId);
    }
}
