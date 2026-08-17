package com.alikaracor.learning.flightservice.event;

import com.alikaracor.learning.flightservice.model.FlightStatus;
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
public class FlightEvent {

    private UUID eventId;
    private FlightEventType eventType;
    private Instant occurredAt;

    private Long flightId;
    private Integer flightVersion;

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

    private Long changedByUserId;

    private LocalDate scheduledArrivalDate;

    private Instant scheduledDepartureAt;

    private Instant scheduledArrivalAt;

}
