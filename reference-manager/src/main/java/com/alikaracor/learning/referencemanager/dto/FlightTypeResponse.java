package com.alikaracor.learning.referencemanager.dto;

import com.alikaracor.learning.referencemanager.model.FlightTypeStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class FlightTypeResponse {

    private Long flightTypeId;
    private String flightTypeName;
    private String flightTypeCode;
    private FlightTypeStatus flightTypeStatus;
    private Instant flightTypeCreatedAt;
    private Instant flightTypeUpdatedAt;
}
