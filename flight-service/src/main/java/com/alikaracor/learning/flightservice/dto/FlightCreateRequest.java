package com.alikaracor.learning.flightservice.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Setter
@Getter
@NoArgsConstructor
public class FlightCreateRequest {

    @NotBlank
    @Pattern(
            regexp = "^[A-Z0-9]{2}\\d{4}$",
            message = "Flight number 2 adet büyük harf ve 4 adet rakamdan olmuşmalıdır"
    )
    private String flightNumber;

    @NotNull
    @Positive
    private Long airlineId;

    @Positive
    private Long aircraftId;

    @NotNull
    @Positive
    private Long aircraftTypeId;

    @NotNull
    @Positive
    private Long originAirportId;

    @NotNull
    @Positive
    private Long destinationAirportId;

    @NotNull
    @Positive
    private Long flightTypeId;

    @NotNull
    private LocalDate flightDate;

    @NotNull
    private LocalTime scheduledDepartureTime;

    @NotNull
    private LocalTime scheduledArrivalTime;

    @NotNull
    private LocalDate scheduledArrivalDate;

}
