package com.alikaracor.learning.flightservice.service;

import com.alikaracor.learning.flightservice.dto.FlightCsvImportResponse;
import com.alikaracor.learning.flightservice.dto.FlightResponse;
import jakarta.validation.Validator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.alikaracor.learning.flightservice.dto.FlightCreateRequest;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.ConstraintViolation;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class FlightCsvImportService {

    private final FlightService flightService;
    private final Validator validator;

    public FlightCsvImportService(FlightService flightService, Validator validator) {
        this.flightService = flightService;
        this.validator = validator;
    }

    public FlightCsvImportResponse importFlights(MultipartFile file, Long actorUserId, String clientIpAddress) {

        if (file.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Empty file");

        }

        List<FlightResponse> successfulFlights = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        int totalRowCount = 0;

        try (

                Reader reader = new InputStreamReader(
                        file.getInputStream(),
                        StandardCharsets.UTF_8
                );

                CSVParser csvParser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setTrim(true)
                        .get()
                        .parse(reader)

        ) {

            for (CSVRecord csvRecord : csvParser) {

                totalRowCount++;

                try {
                    FlightCreateRequest flightCreateRequest =
                            toFlightCreateRequest(csvRecord);

                    Set<ConstraintViolation<FlightCreateRequest>> violations =
                            validator.validate(flightCreateRequest);

                    if (!violations.isEmpty()) {

                        String validationMessage = violations.stream()
                                .map(violation ->
                                        violation.getPropertyPath()
                                                + ": "
                                                + violation.getMessage()
                                )
                                .collect(Collectors.joining(", "));

                        throw new IllegalArgumentException(validationMessage);
                    }

                    FlightResponse savedFlight = flightService.addFlight(
                            flightCreateRequest,
                            actorUserId,
                            clientIpAddress
                    );

                    successfulFlights.add(savedFlight);

                } catch (RuntimeException exception) {

                    String failureReason = exception.getMessage();

                    if (exception instanceof ResponseStatusException responseStatusException
                            && responseStatusException.getReason() != null) {

                        failureReason = responseStatusException.getReason();
                    }

                    errors.add(
                            "CSV satırı "
                                    + (csvRecord.getRecordNumber() + 1)
                                    + ": "
                                    + failureReason
                    );
                }
            }

        } catch (IOException exception) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Error reading from file",
                    exception
            );

        }


        FlightCsvImportResponse response = new FlightCsvImportResponse();

        response.setTotalRowCount(totalRowCount);
        response.setSuccessfulRowCount(successfulFlights.size());
        response.setFailedRowCount(errors.size());
        response.setSuccessfulFlights(successfulFlights);
        response.setErrors(errors);

        return response;


    }

    private FlightCreateRequest toFlightCreateRequest(CSVRecord csvRecord) {

        FlightCreateRequest request = new FlightCreateRequest();

        request.setFlightNumber(csvRecord.get("flightNumber"));
        request.setAirlineId(Long.valueOf(csvRecord.get("airlineId")));

        String aircraftId = csvRecord.get("aircraftId");
        if (!aircraftId.isBlank()) {
            request.setAircraftId(Long.valueOf(aircraftId));
        }

        request.setAircraftTypeId(
                Long.valueOf(csvRecord.get("aircraftTypeId"))
        );

        request.setOriginAirportId(
                Long.valueOf(csvRecord.get("originAirportId"))
        );

        request.setDestinationAirportId(
                Long.valueOf(csvRecord.get("destinationAirportId"))
        );

        request.setFlightTypeId(
                Long.valueOf(csvRecord.get("flightTypeId"))
        );

        request.setFlightDate(
                LocalDate.parse(csvRecord.get("flightDate"))
        );

        request.setScheduledDepartureTime(
                LocalTime.parse(csvRecord.get("scheduledDepartureTime"))
        );

        request.setScheduledArrivalTime(
                LocalTime.parse(csvRecord.get("scheduledArrivalTime"))
        );

        return request;
    }


}
