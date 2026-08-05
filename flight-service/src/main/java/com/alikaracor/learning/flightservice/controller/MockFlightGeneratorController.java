package com.alikaracor.learning.flightservice.controller;

import com.alikaracor.learning.flightservice.dto.FlightResponse;
import com.alikaracor.learning.flightservice.dto.MockFlightGenerationRequest;
import com.alikaracor.learning.flightservice.service.MockFlightGeneratorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/flights/mock")
@RestController
public class MockFlightGeneratorController {

    private final MockFlightGeneratorService mockFlightGeneratorService;


    public MockFlightGeneratorController(MockFlightGeneratorService mockFlightGeneratorService) {
        this.mockFlightGeneratorService = mockFlightGeneratorService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<FlightResponse> generateFlights(@Valid @RequestBody MockFlightGenerationRequest request, @AuthenticationPrincipal Jwt jwt, HttpServletRequest httpServletRequest){

        Long performedByUser = Long.valueOf(jwt.getSubject());
        String clientIpAdress =  httpServletRequest.getRemoteAddr();

        return mockFlightGeneratorService.generateFlights(request,performedByUser,clientIpAdress);

    }








}
