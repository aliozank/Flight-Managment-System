package com.alikaracor.learning.referencemanager.repository;

import com.alikaracor.learning.referencemanager.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;

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

}
