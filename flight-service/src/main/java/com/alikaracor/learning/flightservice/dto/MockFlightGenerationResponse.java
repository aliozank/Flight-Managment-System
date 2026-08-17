package com.alikaracor.learning.flightservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class MockFlightGenerationResponse {

    private int requestedCount;

    private int successfulCount;

    private int failedCount;

    private List<FlightResponse> successfulFlights;

    private List<String> errors;
}
