package com.alikaracor.learning.flightservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MockFlightGenerationRequest {

    @Min(1)
    @Max(100)
    @NotNull
    private Integer flightCount;

    @NotNull
    @Min(1)
    @Max(365)
    private Integer maximumFutureDays;

}
