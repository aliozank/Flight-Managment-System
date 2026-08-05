package com.alikaracor.learning.flightservice.service;

import com.alikaracor.learning.flightservice.dto.FlightCreateRequest;
import com.alikaracor.learning.flightservice.dto.FlightCsvImportResponse;
import com.alikaracor.learning.flightservice.dto.FlightResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightCsvImportServiceTest {

    @Mock
    private FlightService flightService;

    @Mock
    private Validator validator;

    @InjectMocks
    private FlightCsvImportService flightCsvImportService;

    private Long actorUserId;
    private String clientIpAddress;
    private String csvHeader;

    @BeforeEach
    void setUp() {
        actorUserId = 100L;
        clientIpAddress = "127.0.0.1";
        csvHeader = "flightNumber,airlineId,aircraftId,aircraftTypeId,originAirportId,destinationAirportId,flightTypeId,flightDate,scheduledDepartureTime,scheduledArrivalTime\n";
    }

    @Test
    @DisplayName("importFlights - Dosya tamamen boş olduğunda 400 BAD_REQUEST fırlatmalıdır")
    void importFlights_shouldThrowResponseStatusException_whenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> flightCsvImportService.importFlights(emptyFile, actorUserId, clientIpAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(flightService, validator);
    }

    @Test
    @DisplayName("importFlights - Header dışında veri içermeyen CSV yüklendiğinde sayaçlar 0 olmalıdır")
    void importFlights_shouldReturnZeroCounts_whenHeaderOnlyCsvUploaded() {
        MockMultipartFile file = new MockMultipartFile("file", "header_only.csv", "text/csv", csvHeader.getBytes(StandardCharsets.UTF_8));

        FlightCsvImportResponse response = flightCsvImportService.importFlights(file, actorUserId, clientIpAddress);

        assertThat(response).isNotNull();
        assertThat(response.getTotalRowCount()).isEqualTo(0);
        assertThat(response.getSuccessfulRowCount()).isEqualTo(0);
        assertThat(response.getFailedRowCount()).isEqualTo(0);
        assertThat(response.getErrors()).isEmpty();

        verifyNoInteractions(flightService, validator);
    }

    @Test
    @DisplayName("importFlights - Geçerli CSV yüklendiğinde tüm uçuşlar başarıyla eklenmelidir")
    void importFlights_shouldImportFlightsSuccessfully_whenCsvIsValid() {
        String csvContent = csvHeader
                + "TK1234,10,100,20,1,2,5,2026-10-01,10:00,12:00\n"
                + "TK5678,10,,20,1,3,5,2026-10-02,14:00,16:00";

        MockMultipartFile file = new MockMultipartFile("file", "flights.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(validator.validate(any(FlightCreateRequest.class))).thenReturn(Collections.emptySet());

        FlightResponse mockResponse1 = new FlightResponse();
        mockResponse1.setFlightId(1L);
        mockResponse1.setFlightNumber("TK1234");

        FlightResponse mockResponse2 = new FlightResponse();
        mockResponse2.setFlightId(2L);
        mockResponse2.setFlightNumber("TK5678");

        when(flightService.addFlight(any(FlightCreateRequest.class), eq(actorUserId), eq(clientIpAddress)))
                .thenReturn(mockResponse1)
                .thenReturn(mockResponse2);

        FlightCsvImportResponse response = flightCsvImportService.importFlights(file, actorUserId, clientIpAddress);

        assertThat(response).isNotNull();
        assertThat(response.getTotalRowCount()).isEqualTo(2);
        assertThat(response.getSuccessfulRowCount()).isEqualTo(2);
        assertThat(response.getFailedRowCount()).isEqualTo(0);
        assertThat(response.getSuccessfulFlights()).hasSize(2);
        assertThat(response.getErrors()).isEmpty();

        verify(flightService, times(2)).addFlight(any(FlightCreateRequest.class), eq(actorUserId), eq(clientIpAddress));
    }

    @Test
    @DisplayName("importFlights - Satırlardan biri hata aldığında kısmi başarı raporlanmalıdır")
    void importFlights_shouldHandlePartialErrors_whenSomeRowsFailValidationOrService() {
        String csvContent = csvHeader
                + "TK1234,10,100,20,1,2,5,2026-10-01,10:00,12:00\n"
                + "TK5678,10,101,20,1,3,5,2026-10-02,14:00,16:00";

        MockMultipartFile file = new MockMultipartFile("file", "flights.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(validator.validate(any(FlightCreateRequest.class))).thenReturn(Collections.emptySet());

        FlightResponse mockResponse1 = new FlightResponse();
        mockResponse1.setFlightId(1L);

        when(flightService.addFlight(any(FlightCreateRequest.class), eq(actorUserId), eq(clientIpAddress)))
                .thenReturn(mockResponse1)
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Flight already exists"));

        FlightCsvImportResponse response = flightCsvImportService.importFlights(file, actorUserId, clientIpAddress);

        assertThat(response.getTotalRowCount()).isEqualTo(2);
        assertThat(response.getSuccessfulRowCount()).isEqualTo(1);
        assertThat(response.getFailedRowCount()).isEqualTo(1);
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().get(0)).contains("Flight already exists");
    }

    @Test
    @DisplayName("importFlights - Tarih/saat parse hatası olduğunda hata listesine eklenmeli ve işleme devam etmelidir")
    void importFlights_shouldRecordParseError_whenDateFieldIsInvalid() {
        String csvContent = csvHeader + "TK1234,10,100,20,1,2,5,INVALID_DATE,10:00,12:00";

        MockMultipartFile file = new MockMultipartFile("file", "flights.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        FlightCsvImportResponse response = flightCsvImportService.importFlights(file, actorUserId, clientIpAddress);

        assertThat(response.getTotalRowCount()).isEqualTo(1);
        assertThat(response.getSuccessfulRowCount()).isEqualTo(0);
        assertThat(response.getFailedRowCount()).isEqualTo(1);
        assertThat(response.getErrors().get(0)).contains("satırı 2");

        verify(flightService, never()).addFlight(any(), any(), any());
    }

    @Test
    @DisplayName("importFlights - Bean Validation ihlali olduğunda hata kaydı yapılmalıdır")
    void importFlights_shouldRecordValidationError_whenValidatorReturnsViolations() {
        String csvContent = csvHeader + "INVALID,10,100,20,1,2,5,2026-10-01,10:00,12:00";

        MockMultipartFile file = new MockMultipartFile("file", "flights.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        @SuppressWarnings("unchecked")
        ConstraintViolation<FlightCreateRequest> violation = mock(ConstraintViolation.class);
        Path propertyPath = mock(Path.class);
        when(propertyPath.toString()).thenReturn("flightNumber");
        when(violation.getPropertyPath()).thenReturn(propertyPath);
        when(violation.getMessage()).thenReturn("format is invalid");

        when(validator.validate(any(FlightCreateRequest.class))).thenReturn(Set.of(violation));

        FlightCsvImportResponse response = flightCsvImportService.importFlights(file, actorUserId, clientIpAddress);

        assertThat(response.getTotalRowCount()).isEqualTo(1);
        assertThat(response.getSuccessfulRowCount()).isEqualTo(0);
        assertThat(response.getFailedRowCount()).isEqualTo(1);
        assertThat(response.getErrors().get(0)).contains("flightNumber: format is invalid");

        verify(flightService, never()).addFlight(any(), any(), any());
    }
}
