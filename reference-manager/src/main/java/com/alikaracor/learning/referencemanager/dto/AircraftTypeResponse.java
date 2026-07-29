package com.alikaracor.learning.referencemanager.dto;

import com.alikaracor.learning.referencemanager.model.AircraftCategory;
import com.alikaracor.learning.referencemanager.model.AircraftTypeStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class AircraftTypeResponse {

    private Long aircraftTypeId;
    private AircraftCategory aircraftTypeCategory;
    private String aircraftTypeManufacturer;
    private String aircraftTypeModel;
    private String aircraftTypeIcaoCode;
    private AircraftTypeStatus aircraftTypeStatus;
    private Instant aircraftTypeCreatedAt;
    private Instant aircraftTypeUpdatedAt;


}
