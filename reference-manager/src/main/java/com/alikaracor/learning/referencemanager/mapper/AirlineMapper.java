package com.alikaracor.learning.referencemanager.mapper;

import com.alikaracor.learning.referencemanager.dto.AirlineRequest;
import com.alikaracor.learning.referencemanager.dto.AirlineResponse;
import com.alikaracor.learning.referencemanager.model.Airline;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AirlineMapper {


AirlineResponse toAirlineResponse(Airline airline);

Airline toAirline(AirlineRequest airlinerRequest);


}
