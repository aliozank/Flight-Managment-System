package com.alikaracor.learning.referencemanager.dto;

import com.alikaracor.learning.referencemanager.model.AircraftStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class AircraftResponse {


    private Long aircraftId;
    private String aircraftRegistrationNumber;
    private Long operatorAirlineId;
    private Long aircraftTypeId;
    private Integer aircraftCapacity;
    private Integer aircraftManufactureYear;
    private AircraftStatus aircraftStatus;
    private Instant aircraftCreatedAt;
    private Instant aircraftUpdatedAt;
}
