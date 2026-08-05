package com.alikaracor.learning.flight_archive_service.dto;


import com.alikaracor.learning.flight_archive_service.model.FlightStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ArchivedFlightResponse {

    private Long archiveId;
    private UUID eventId;

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
    private Long changedByUserId;

    private Instant eventOccurredAt;
    private Instant archivedAt;
}


