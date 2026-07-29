package com.alikaracor.learning.referencemanager.dto;


import com.alikaracor.learning.referencemanager.model.RouteStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RouteRequest {

    @NotNull
    @Positive
    private Long originAirportId;

    @NotNull
    @Positive
    private Long destinationAirportId;

    @NotNull
    private RouteStatus routeStatus;

}
