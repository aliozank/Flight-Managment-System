package com.alikaracor.learning.flight_archive_service.mapper;

import com.alikaracor.learning.flight_archive_service.dto.ArchivedFlightResponse;
import com.alikaracor.learning.flight_archive_service.model.ArchivedFlight;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArchiveFlightMapper {


    ArchivedFlightResponse toArchivedFlightResponse(ArchivedFlight archivedFlight);




}
