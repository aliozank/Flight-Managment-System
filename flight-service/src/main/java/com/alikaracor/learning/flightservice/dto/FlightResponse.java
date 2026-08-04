package com.alikaracor.learning.flightservice.dto;

import com.alikaracor.learning.flightservice.model.FlightStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
public class FlightResponse {

    private Long flightId;

    private String flightNumber;

    private Long airlineId;

    private Long aircraftId;

    private Long aircraftTypeId;

    private Long originAirportId;

    private Long destinationAirportId;

    private Long flightTypeId;

    private LocalDate flightDate;

    private LocalTime scheduledDepartureTime;

    private LocalTime scheduledArrivalTime;

    private FlightStatus flightStatus;

    private Integer flightVersion;

    private Instant flightCreatedAt;

    private Instant flightUpdatedAt;
}