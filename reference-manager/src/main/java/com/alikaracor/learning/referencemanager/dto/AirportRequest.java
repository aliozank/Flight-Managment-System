package com.alikaracor.learning.referencemanager.dto;

import com.alikaracor.learning.referencemanager.model.AirportStatus;
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
public class AirportRequest {

    @Size(max = 150)
    @NotBlank
    private String airportName;

    @Size(max = 100)
    @NotBlank
    private String airportCity;

    @Size(max = 60)
    @NotBlank
    private String airportCountry;

    @NotBlank
    @Pattern(
            regexp = "^[A-Z]{3}$",
            message = "IATA kodu 3 büyük harften oluşmalıdır"
    )
    private String airportIataCode;

    @NotBlank
    @Pattern(
            regexp = "^[A-Z]{4}$",
            message = "ICAO kodu 4 büyük harften oluşmalıdır"
    )
    private String airportIcaoCode;

    @Size(max = 60)
    @NotBlank
    private String airportTimezone;

    @NotNull
    private AirportStatus airportStatus;
}
