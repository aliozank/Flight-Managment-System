package com.alikaracor.learning.referencemanager.dto;

import com.alikaracor.learning.referencemanager.model.AirportStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class AirportResponse {

    private Long airportId;
    private String airportName;
    private String airportCity;
    private String airportCountry;
    private String airportIataCode;
    private String airportIcaoCode;
    private String airportTimezone;
    private AirportStatus airportStatus;
    private Instant airportCreatedAt;
    private Instant airportUpdatedAt;
}
