package com.alikaracor.learning.flight_archive_service.controller;

import com.alikaracor.learning.flight_archive_service.dto.ArchivedFlightResponse;
import com.alikaracor.learning.flight_archive_service.service.FlightArchiveService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/archived-flights")
public class ArchivedFlightController {

    private final FlightArchiveService flightArchiveService;

    public ArchivedFlightController(FlightArchiveService flightArchiveService) {
        this.flightArchiveService = flightArchiveService;
    }


    @GetMapping
    public List<ArchivedFlightResponse> getArchivedFlights() {

        return flightArchiveService.getAllArchivedFlights();

    }

    @GetMapping("/by-flight/{flightId}")
    public ArchivedFlightResponse getArchivedFlightById(@PathVariable Long flightId) {

        return flightArchiveService.getArchivedFlightByFlightId(flightId);

    }

    @GetMapping("/{archiveId}")
    public ArchivedFlightResponse getArchivedFlightByArchiveId(@PathVariable Long archiveId) {

        return flightArchiveService.getArchivedFlightByArchiveId(archiveId);

    }




}
