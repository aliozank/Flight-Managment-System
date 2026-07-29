package com.alikaracor.learning.referencemanager.dto;

import com.alikaracor.learning.referencemanager.model.AircraftCategory;
import com.alikaracor.learning.referencemanager.model.AircraftTypeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AircraftTypeRequest {

    @NotBlank
    @Size(max = 100)
    private String aircraftTypeManufacturer;

    @NotBlank
    @Size(max = 100)
    private String aircraftTypeModel;

    @NotBlank
    @Pattern(
            regexp = "^[A-Z0-9]{2,4}$",
            message = "ICAO type code 2-4 karakter arasında ve yalnızca büyük harf/rakam olmalıdır"
    )
    private String aircraftTypeIcaoCode;

    @NotNull
    private AircraftCategory aircraftTypeCategory;

    @NotNull
    private AircraftTypeStatus aircraftTypeStatus;

}
