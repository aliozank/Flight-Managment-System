package com.alikaracor.learning.flight_archive_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "archived_flights")
public class ArchivedFlight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "archive_id", nullable = false)
    private Long archiveId;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "flight_id", nullable = false, unique = true)
    private Long flightId;

    @Column(name = "flight_number" , nullable = false , length = 10)
    private String flightNumber;

    @Column(name = "airline_id", nullable = false)
    private Long airlineId;

    @Column(name = "aircraft_id")
    private Long aircraftId;

    @Column(name = "aircraft_type_id" , nullable = false)
    private Long aircraftTypeId;

    @Column(name = "origin_airport_id", nullable = false)
    private Long originAirportId;

    @Column(name = "destination_airport_id", nullable = false)
    private Long destinationAirportId;

    @Column(name = "flight_type_id", nullable = false)
    private Long flightTypeId;

    @Column(name = "flight_date", nullable = false)
    private LocalDate flightDate;

    @Column(name = "scheduled_departure_time", nullable = false)
    private LocalTime scheduledDepartureTime;

    @Column(name = "scheduled_arrival_time", nullable = false)
    private LocalTime scheduledArrivalTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "flight_status", nullable = false, length = 30)
    private FlightStatus flightStatus;

    @Column(name = "flight_version", nullable = false)
    private Integer flightVersion;

    @Column(name = "changed_by_user_id")
    private Long changedByUserId;

    @Column(name = "event_occurred_at", nullable = false)
    private Instant eventOccurredAt;

    @CreationTimestamp
    @Column(name = "archived_at", nullable = false, updatable = false)
    private Instant archivedAt;

    @Column(name = "scheduled_arrival_date", nullable = false)
    private LocalDate scheduledArrivalDate;

    @Column(name = "scheduled_departure_at", nullable = false)
    private Instant scheduledDepartureAt;

    @Column(name = "scheduled_arrival_at", nullable = false)
    private Instant scheduledArrivalAt;


}
