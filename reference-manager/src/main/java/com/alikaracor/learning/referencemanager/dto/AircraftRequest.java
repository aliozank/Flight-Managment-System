package com.alikaracor.learning.referencemanager.dto;

import com.alikaracor.learning.referencemanager.model.AircraftStatus;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter
@Setter
@NoArgsConstructor
public class AircraftRequest {


    @NotBlank
    @Size(max = 20)
    @Pattern(
            regexp = "^[A-Z0-9-]{2,20}$",
            message = "Registration number yalnızca büyük harf, rakam ve tire içermelidir"
    )
    private String aircraftRegistrationNumber;

    private Long operatorAirlineId;

    @NotNull
    private Long aircraftTypeId;

    @NotNull
    @PositiveOrZero
    private Integer aircraftCapacity;

    @NotNull
    @Min(1923)
    private Integer aircraftManufactureYear;

    @NotNull
    private AircraftStatus aircraftStatus;


}
