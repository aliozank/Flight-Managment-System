package com.alikaracor.learning.referencemanager.mapper;

import com.alikaracor.learning.referencemanager.dto.RouteRequest;
import com.alikaracor.learning.referencemanager.dto.RouteResponse;
import com.alikaracor.learning.referencemanager.model.Route;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface RouteMapper {

    @Mapping(target = "originAirport", ignore = true)
    @Mapping(target = "destinationAirport", ignore = true)
    Route toRoute(RouteRequest routeRequest);

    @Mapping(source = "originAirport.airportId",
            target = "originAirportId")
    @Mapping(source = "destinationAirport.airportId",
            target = "destinationAirportId")
    RouteResponse toRouteResponse(Route route);
}
