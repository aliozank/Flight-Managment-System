package com.alikaracor.learning.flight_archive_service.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.alikaracor.learning.flight_archive_service.model.FlightStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class FlightEvent {

    private UUID eventId;
    private FlightEventType eventType;
    private Instant occurredAt;

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

}
