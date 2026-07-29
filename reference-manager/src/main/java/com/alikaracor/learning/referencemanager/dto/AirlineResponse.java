package com.alikaracor.learning.referencemanager.dto;

import com.alikaracor.learning.referencemanager.model.AirlineStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class AirlineResponse {

    private String airlineIataCode;
    private String airlineCountry;
    private String airlineName;
    private String airlineIcaoCode;
    private Long airlineId;
    private AirlineStatus airlineStatus;
    private Instant airlineUpdatedAt;
    private Instant airlineCreatedAt;
}
