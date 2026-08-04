package com.alikaracor.learning.flightservice.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
public class FlightCsvImportResponse {

    private int totalRowCount;
    private int successfulRowCount;
    private int failedRowCount;
    private List<FlightResponse> successfulFlights;
    private List<String> errors;

}
