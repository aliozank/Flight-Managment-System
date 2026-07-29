package com.alikaracor.learning.referencemanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Table(name = "airlines")
@Getter
@Setter
@NoArgsConstructor
@Entity
public class Airline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long airlineId;

    @Column(unique = true, nullable = false, length = 150)
    private String airlineName;

    @Column(unique = true, nullable = false ,length = 2)
    private String airlineIataCode;

    @Column(unique = true, nullable = false ,length = 3)
    private String airlineIcaoCode;

    @Column(nullable = false ,length = 60)
    private String airlineCountry;

    @Column(nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private AirlineStatus airlineStatus;

    @Column(nullable = false)
    @CreationTimestamp
    private Instant airlineCreatedAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private Instant airlineUpdatedAt;


}