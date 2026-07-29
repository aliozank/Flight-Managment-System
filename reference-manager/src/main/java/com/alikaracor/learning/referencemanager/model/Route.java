package com.alikaracor.learning.referencemanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Table(
        name = "routes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uc_routes_origin_destination",
                        columnNames = {"origin_airport_id", "destination_airport_id"}
                )
        }
)
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long routeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_airport_id", nullable = false)
    private Airport originAirport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_airport_id", nullable = false)
    private Airport destinationAirport;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RouteStatus routeStatus;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant routeCreatedAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant routeUpdatedAt;

}
