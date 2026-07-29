package com.alikaracor.learning.referencemanager.mapper;

import com.alikaracor.learning.referencemanager.dto.AircraftRequest;
import com.alikaracor.learning.referencemanager.dto.AircraftResponse;
import com.alikaracor.learning.referencemanager.model.Aircraft;
import com.alikaracor.learning.referencemanager.model.AircraftStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AircraftMapper {


    @Mapping(target = "operatorAirline", ignore = true)
    @Mapping(target = "aircraftType", ignore = true)
    Aircraft toAircraft(AircraftRequest aircraftRequest);


    @Mapping(source = "operatorAirline.airlineId",
            target = "operatorAirlineId")
    @Mapping(source = "aircraftType.aircraftTypeId",
            target = "aircraftTypeId")
    AircraftResponse toAircraftResponse(Aircraft aircraft);

}
