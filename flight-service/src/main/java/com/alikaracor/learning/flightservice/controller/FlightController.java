package com.alikaracor.learning.flightservice.controller;

import com.alikaracor.learning.flightservice.dto.*;
import com.alikaracor.learning.flightservice.service.FlightCsvImportService;
import com.alikaracor.learning.flightservice.service.FlightService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private final FlightService flightService;
    private final FlightCsvImportService flightCsvImportService;

    public FlightController(FlightService flightService, FlightCsvImportService flightCsvImportService) {
        this.flightService = flightService;
        this.flightCsvImportService = flightCsvImportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FlightResponse createFlight(@RequestBody @Valid FlightCreateRequest flightCreateRequest, @AuthenticationPrincipal Jwt jwt, HttpServletRequest httpServletRequest) {

        Long performedByUserId = Long.valueOf(jwt.getSubject());

        String clientIpAddress = httpServletRequest.getRemoteAddr();

        return flightService.addFlight(flightCreateRequest, performedByUserId, clientIpAddress);

    }

    @GetMapping
    public List<FlightResponse> getFlights() {

        return flightService.getAllFlights();

    }

    @GetMapping("/{flightId}")
    public FlightResponse getFlightById(@PathVariable Long flightId) {

        return flightService.getFlightById(flightId);

    }

    @PutMapping("/{flightId}")
    public FlightResponse updateFlightById(@PathVariable Long flightId, @RequestBody @Valid FlightUpdateRequest flightUpdateRequest,@AuthenticationPrincipal Jwt jwt, HttpServletRequest httpServletRequest) {

        Long performedByUserId = Long.valueOf(jwt.getSubject());

        String clientIpAddress = httpServletRequest.getRemoteAddr();

        return flightService.updateFlight(flightId, flightUpdateRequest, performedByUserId, clientIpAddress);

    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{flightId}")
    public void cancelFlight(@PathVariable Long flightId, @AuthenticationPrincipal Jwt jwt, HttpServletRequest httpServletRequest) {

        Long performedByUserId = Long.valueOf(jwt.getSubject());
        String clientIpAddress = httpServletRequest.getRemoteAddr();

        flightService.cancelFlight(flightId, performedByUserId, clientIpAddress);

    }

    @PostMapping(value = "/csv/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public FlightCsvImportResponse uploadFlightCsv(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal Jwt jwt, HttpServletRequest httpServletRequest) {

        Long performedByUserId = Long.valueOf(jwt.getSubject());
        String clientIpAddress = httpServletRequest.getRemoteAddr();

        return flightCsvImportService.importFlights(file, performedByUserId, clientIpAddress);
    }

    @PatchMapping("/{flightId}/status")
    public FlightResponse updateFlightStatus(@PathVariable Long flightId, @Valid @RequestBody FlightStatusUpdateRequest flightStatusUpdateRequest, @AuthenticationPrincipal Jwt jwt, HttpServletRequest httpServletRequest) {

        Long performedByUserId = Long.valueOf(jwt.getSubject());
        String clientIpAddress = httpServletRequest.getRemoteAddr();

        return flightService.updateFlightStatus(flightId, flightStatusUpdateRequest, performedByUserId, clientIpAddress);

    }

}
