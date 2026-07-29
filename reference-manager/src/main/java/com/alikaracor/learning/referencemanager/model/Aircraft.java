package com.alikaracor.learning.referencemanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "aircrafts")
public class Aircraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long aircraftId;

    @Column(nullable = false, unique = true, length = 20)
    private String aircraftRegistrationNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="operator_airline_id")
    private Airline operatorAirline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="aircraft_type_id", nullable = false)
    private AircraftType aircraftType;

    @Column(nullable = false)
    private Integer aircraftCapacity;

    @Column(nullable = false)
    private Integer aircraftManufactureYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AircraftStatus aircraftStatus;

    @Column(nullable = false)
    @CreationTimestamp
    private Instant aircraftCreatedAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private Instant aircraftUpdatedAt;
}
