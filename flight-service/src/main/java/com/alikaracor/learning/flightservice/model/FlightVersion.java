package com.alikaracor.learning.flightservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Entity
@Setter
@NoArgsConstructor
@Table(name = "flight_versions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uc_flight_versions_flight_version",
                        columnNames = {
                                "flight_id",
                                "flight_version_number"
                        }
                ) } )
public class FlightVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flight_version_id",nullable = false)
    private Long flightVersionId;

    @JoinColumn(name = "flight_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_flight_versions_flight"))
    @ManyToOne(fetch = FetchType.LAZY)
    private Flight flight;


    @Column(name = "flight_version_number", nullable = false)
    private Integer flightVersionNumber;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "flight_status", nullable = false, length = 30)
    private FlightStatus flightStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "flight_change_type", nullable = false, length = 30)
    private FlightChangeType flightChangeType;

    @Column(name = "changed_by_user_id")
    private Long changedByUserId;

    @CreationTimestamp
    @Column(name = "flight_version_created_at",
            nullable = false,
            updatable = false)
    private Instant flightVersionCreatedAt;




}
