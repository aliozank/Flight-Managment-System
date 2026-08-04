package com.alikaracor.learning.flightservice.mapper;

import com.alikaracor.learning.flightservice.dto.FlightCreateRequest;
import com.alikaracor.learning.flightservice.dto.FlightResponse;
import com.alikaracor.learning.flightservice.dto.FlightUpdateRequest;
import com.alikaracor.learning.flightservice.event.FlightEvent;
import com.alikaracor.learning.flightservice.model.Flight;
import com.alikaracor.learning.flightservice.model.FlightVersion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface FlightMapper {

    @Mapping(target = "flightId", ignore = true)
    @Mapping(target = "flightStatus", ignore = true)
    @Mapping(target = "flightVersion", ignore = true)
    @Mapping(target = "flightCreatedAt", ignore = true)
    @Mapping(target = "flightUpdatedAt", ignore = true)
    Flight toFlight(FlightCreateRequest flightCreateRequest);

    FlightResponse toFlightResponse(Flight flight);

    @Mapping(target = "flightId", ignore = true)
    @Mapping(target = "flightVersion", ignore = true)
    @Mapping(target = "flightCreatedAt", ignore = true)
    @Mapping(target = "flightUpdatedAt", ignore = true)
    void updateFlight(FlightUpdateRequest flightUpdateRequest, @MappingTarget Flight flight);

    @Mapping(target = "flightVersionId", ignore = true)
    @Mapping(target = "flight",
            expression = "java(flight)"
    )
    @Mapping(target = "flightVersionNumber",
            source = "flightVersion"
    )
    @Mapping(target = "flightChangeType", ignore = true)
    @Mapping(target = "changedByUserId", ignore = true)
    @Mapping(target = "flightVersionCreatedAt", ignore = true)
    FlightVersion toFlightVersion(Flight flight);

    @Mapping(target = "eventId" , ignore = true)
    @Mapping(target = "eventType" , ignore = true)
    @Mapping(target = "occurredAt" , ignore = true)
    @Mapping(target = "changedByUserId", ignore = true)
    FlightEvent toFlightEvent(Flight flight);

}
