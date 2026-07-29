package com.alikaracor.learning.referencemanager.mapper;

import com.alikaracor.learning.referencemanager.dto.FlightTypeRequest;
import com.alikaracor.learning.referencemanager.dto.FlightTypeResponse;
import com.alikaracor.learning.referencemanager.model.FlightType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FlightTypeMapper {

    FlightType toFlightType(FlightTypeRequest flightTypeRequest);

    FlightTypeResponse toFlightTypeResponse(FlightType flightType);
}
