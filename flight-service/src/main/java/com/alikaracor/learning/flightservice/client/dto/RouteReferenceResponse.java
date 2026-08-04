package com.alikaracor.learning.flightservice.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class RouteReferenceResponse implements Serializable {

    private Long routeId;
    private Long originAirportId;
    private Long destinationAirportId;
    private String routeStatus;

}
