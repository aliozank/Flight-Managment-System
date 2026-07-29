package com.alikaracor.learning.referencemanager.mapper;

import com.alikaracor.learning.referencemanager.dto.AirportRequest;
import com.alikaracor.learning.referencemanager.dto.AirportResponse;
import com.alikaracor.learning.referencemanager.model.Airport;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AirportMapper {

    AirportResponse toAirportResponse(Airport airport);

    Airport toAirport(AirportRequest airportRequest);
}
