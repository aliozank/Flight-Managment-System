package com.alikaracor.learning.flightservice.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class AircraftReferenceResponse implements Serializable {

    private Long aircraftId;
    private Long operatorAirlineId;
    private Long aircraftTypeId;
    private String aircraftStatus;
}
