package com.alikaracor.learning.referencemanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Table(name = "airports")
@Getter
@Setter
@NoArgsConstructor
@Entity
public class Airport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long airportId;

    @Column(nullable = false, length = 150)
    private String airportName;

    @Column(nullable = false, length = 100)
    private String airportCity;

    @Column(nullable = false, length = 60)
    private String airportCountry;

    @Column(unique = true, nullable = false, length = 3)
    private String airportIataCode;

    @Column(unique = true, nullable = false, length = 4)
    private String airportIcaoCode;

    @Column(nullable = false, length = 60)
    private String airportTimezone;

    @Column(nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private AirportStatus airportStatus;

    @Column(nullable = false)
    @CreationTimestamp
    private Instant airportCreatedAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private Instant airportUpdatedAt;
}
