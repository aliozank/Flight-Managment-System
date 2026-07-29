package com.alikaracor.learning.referencemanager.mapper;

import com.alikaracor.learning.referencemanager.dto.AircraftTypeRequest;
import com.alikaracor.learning.referencemanager.dto.AircraftTypeResponse;
import com.alikaracor.learning.referencemanager.model.AircraftType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AircraftTypeMapper {

    AircraftType toAircraftType(AircraftTypeRequest aircraftTypeRequest);

    AircraftTypeResponse toAircraftTypeResponse(AircraftType aircraftType);

}
