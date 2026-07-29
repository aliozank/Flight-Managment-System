package com.alikaracor.learning.referencemanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "aircraft_types")
@Getter
@Setter
@NoArgsConstructor
public class AircraftType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long aircraftTypeId;

    @Column(nullable = false, length = 100)
    private String aircraftTypeManufacturer;

    @Column(nullable = false, length = 100)
    private String aircraftTypeModel;

    @Column(unique = true, nullable = false, length = 4)
    private String aircraftTypeIcaoCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AircraftCategory aircraftTypeCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AircraftTypeStatus aircraftTypeStatus;

    @CreationTimestamp
    @Column(nullable = false)
    private Instant aircraftTypeCreatedAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant aircraftTypeUpdatedAt;
}