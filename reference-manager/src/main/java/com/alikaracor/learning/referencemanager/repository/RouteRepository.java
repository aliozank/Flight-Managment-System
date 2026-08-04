package com.alikaracor.learning.referencemanager.repository;

import com.alikaracor.learning.referencemanager.model.Route;
import com.alikaracor.learning.referencemanager.model.RouteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route,Long> {

    boolean existsByOriginAirport_AirportIdAndDestinationAirport_AirportId(
            Long originAirportId,
            Long destinationAirportId
    );

    boolean existsByOriginAirport_AirportIdAndDestinationAirport_AirportIdAndRouteIdNot(
            Long originAirportId,
            Long destinationAirportId,
            Long routeId
    );

    Optional<Route> findByOriginAirport_AirportIdAndDestinationAirport_AirportIdAndRouteStatus(
            Long originAirportId,
            Long destinationAirportId,
            RouteStatus routeStatus
    );

}
