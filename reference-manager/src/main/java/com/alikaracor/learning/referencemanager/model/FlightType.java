package com.alikaracor.learning.referencemanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "flight_types")
@Getter
@Setter
@NoArgsConstructor
public class FlightType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long flightTypeId;

    @Column(nullable = false, unique = true, length = 40)
    private String flightTypeName;

    @Column(nullable = false, unique = true, length = 40)
    private String flightTypeCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private FlightTypeStatus flightTypeStatus;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant flightTypeCreatedAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant flightTypeUpdatedAt;
}
