package com.alikaracor.learning.referencemanager.dto;

import com.alikaracor.learning.referencemanager.model.AirlineStatus;
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
public class AirlineRequest {

    @Size(max = 150)
    @NotBlank
    private String airlineName;

    @NotBlank
    @Pattern(
            regexp = "^[A-Z]{3}$",
            message = "ICAO kodu 3 büyük harften oluşmalıdır"
    )
    private String airlineIcaoCode;


    @NotBlank
    @Pattern(
            regexp = "^[A-Z0-9]{2}$",
            message = "IATA kodu 2 büyük harf/rakamdan oluşmalıdır"
    )
    private String airlineIataCode;

    @Size(max = 60)
    @NotBlank
    private String airlineCountry;

    @NotNull
    private AirlineStatus airlineStatus;

}
