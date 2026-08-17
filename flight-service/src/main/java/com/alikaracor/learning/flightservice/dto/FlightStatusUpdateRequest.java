package com.alikaracor.learning.flightservice.dto;

import com.alikaracor.learning.flightservice.model.FlightStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FlightStatusUpdateRequest {

    @NotNull
    private FlightStatus flightStatus;

}
