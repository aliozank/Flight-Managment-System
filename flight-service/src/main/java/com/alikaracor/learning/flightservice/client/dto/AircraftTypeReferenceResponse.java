package com.alikaracor.learning.flightservice.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class AircraftTypeReferenceResponse implements Serializable {

    private Long aircraftTypeId;
    private String aircraftTypeStatus;

}
