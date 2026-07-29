package com.alikaracor.learning.referencemanager.dto;


import com.alikaracor.learning.referencemanager.model.RouteStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@NoArgsConstructor
public class RouteResponse {

    private Long routeId;

    private Long originAirportId;

    private Long destinationAirportId;

    private RouteStatus routeStatus;

    private Instant routeCreatedAt;

    private Instant routeUpdatedAt;
}
