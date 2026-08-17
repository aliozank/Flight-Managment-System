package com.alikaracor.learning.flightservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "flights",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uc_flights_number_date",
                        columnNames = {"flight_number", "flight_date"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flight_id", nullable = false)
    private Long flightId;

    @Column(name = "flight_number", nullable = false, length = 6)
    private String flightNumber;

    @Column(name = "airline_id", nullable = false)
    private Long airlineId;

    @Column(name = "aircraft_id")
    private Long aircraftId;

    @Column(name = "aircraft_type_id", nullable = false)
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

    @Column(name = "scheduled_arrival_date", nullable = false)
    private LocalDate scheduledArrivalDate;

    @Column(name = "scheduled_departure_at", nullable = false)
    private Instant scheduledDepartureAt;

    @Column(name = "scheduled_arrival_at", nullable = false)
    private Instant scheduledArrivalAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "flight_status", nullable = false, length = 30)
    private FlightStatus flightStatus = FlightStatus.SCHEDULED;

    @Column(name = "flight_version", nullable = false)
    private Integer flightVersion = 1;

    @CreationTimestamp
    @Column(name = "flight_created_at", nullable = false, updatable = false)
    private Instant flightCreatedAt;

    @UpdateTimestamp
    @Column(name = "flight_updated_at", nullable = false)
    private Instant flightUpdatedAt;
}