package com.alikaracor.learning.flightservice.service;

import com.alikaracor.learning.flightservice.client.ReferenceManagerClient;
import com.alikaracor.learning.flightservice.client.dto.*;
import com.alikaracor.learning.flightservice.dto.FlightCreateRequest;
import com.alikaracor.learning.flightservice.dto.FlightResponse;
import com.alikaracor.learning.flightservice.dto.MockFlightGenerationRequest;
import com.alikaracor.learning.flightservice.dto.MockFlightGenerationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MockFlightGeneratorServiceTest {

    @Mock
    private ReferenceManagerClient referenceManagerClient;

    @Mock
    private FlightService flightService;

    @InjectMocks
    private MockFlightGeneratorService mockFlightGeneratorService;

    private MockFlightGenerationRequest generationRequest;
    private AircraftReferenceResponse activeAircraft;
    private RouteReferenceResponse activeRoute;
    private FlightTypeReferenceResponse activeFlightType;
    private AirlineReferenceResponse activeAirline;
    private Long performedByUserId;
    private String clientIpAddress;

    @BeforeEach
    void setUp() {
        performedByUserId = 100L;
        clientIpAddress = "127.0.0.1";

        generationRequest = new MockFlightGenerationRequest();
        generationRequest.setFlightCount(2);
        generationRequest.setMaximumFutureDays(7);

        activeAircraft = new AircraftReferenceResponse();
        activeAircraft.setAircraftId(100L);
        activeAircraft.setAircraftTypeId(20L);
        activeAircraft.setOperatorAirlineId(10L);
        activeAircraft.setAircraftStatus("ACTIVE");

        activeRoute = new RouteReferenceResponse();
        activeRoute.setRouteId(50L);
        activeRoute.setOriginAirportId(1L);
        activeRoute.setDestinationAirportId(2L);
        activeRoute.setRouteStatus("ACTIVE");

        activeFlightType = new FlightTypeReferenceResponse();
        activeFlightType.setFlightTypeId(5L);
        activeFlightType.setFlightTypeStatus("ACTIVE");

        activeAirline = new AirlineReferenceResponse();
        activeAirline.setAirlineId(10L);
        activeAirline.setAirlineIataCode("TK");
        activeAirline.setAirlineStatus("ACTIVE");
    }

    @Test
    @DisplayName("generateFlights - Aktif referans verileriyle istenen sayıda mock uçuş oluşturmalıdır")
    void generateFlights_shouldGenerateFlights_whenReferencesAreActive() {
        when(referenceManagerClient.getAllAircrafts()).thenReturn(List.of(activeAircraft));
        when(referenceManagerClient.getAllRoutes()).thenReturn(List.of(activeRoute));
        when(referenceManagerClient.getAllFlightTypes()).thenReturn(List.of(activeFlightType));
        when(referenceManagerClient.getAirlineById(10L)).thenReturn(activeAirline);

        AirportReferenceResponse mockAirport = new AirportReferenceResponse();
        mockAirport.setAirportId(1L);
        mockAirport.setAirportTimezone("Europe/Istanbul");
        when(referenceManagerClient.getAirportById(anyLong())).thenReturn(mockAirport);

        FlightResponse mockResponse = new FlightResponse();
        mockResponse.setFlightId(1L);
        when(flightService.addFlight(any(FlightCreateRequest.class), eq(performedByUserId), eq(clientIpAddress)))
                .thenReturn(mockResponse);

        MockFlightGenerationResponse result = mockFlightGeneratorService.generateFlights(generationRequest, performedByUserId, clientIpAddress);

        assertThat(result.getRequestedCount()).isEqualTo(2);
        assertThat(result.getSuccessfulCount()).isEqualTo(2);
        assertThat(result.getFailedCount()).isZero();
        assertThat(result.getSuccessfulFlights()).hasSize(2);
        assertThat(result.getErrors()).isEmpty();

        ArgumentCaptor<FlightCreateRequest> requestCaptor = ArgumentCaptor.forClass(FlightCreateRequest.class);
        verify(flightService, times(2)).addFlight(requestCaptor.capture(), eq(performedByUserId), eq(clientIpAddress));

        FlightCreateRequest captured = requestCaptor.getAllValues().get(0);
        assertThat(captured.getFlightNumber()).startsWith("TK");
        assertThat(captured.getAirlineId()).isEqualTo(10L);
        assertThat(captured.getAircraftId()).isEqualTo(100L);
        assertThat(captured.getAircraftTypeId()).isEqualTo(20L);
        assertThat(captured.getOriginAirportId()).isEqualTo(1L);
        assertThat(captured.getDestinationAirportId()).isEqualTo(2L);
        assertThat(captured.getFlightTypeId()).isEqualTo(5L);
        assertThat(captured.getFlightDate()).isAfterOrEqualTo(LocalDate.now().plusDays(1));
        assertThat(captured.getFlightDate()).isBeforeOrEqualTo(LocalDate.now().plusDays(7));
        assertThat(captured.getScheduledArrivalTime()).isAfter(captured.getScheduledDepartureTime());
    }

    @Test
    @DisplayName("generateFlights - Aktif aircraft bulunamadığında 409 CONFLICT fırlatmalıdır")
    void generateFlights_shouldThrowConflict_whenNoActiveAircraftsExist() {
        activeAircraft.setAircraftStatus("INACTIVE");
        when(referenceManagerClient.getAllAircrafts()).thenReturn(List.of(activeAircraft));

        assertThatThrownBy(() -> mockFlightGeneratorService.generateFlights(generationRequest, performedByUserId, clientIpAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verifyNoInteractions(flightService);
    }

    @Test
    @DisplayName("generateFlights - Aktif route bulunamadığında 409 CONFLICT fırlatmalıdır")
    void generateFlights_shouldThrowConflict_whenNoActiveRoutesExist() {
        when(referenceManagerClient.getAllAircrafts()).thenReturn(List.of(activeAircraft));
        activeRoute.setRouteStatus("INACTIVE");
        when(referenceManagerClient.getAllRoutes()).thenReturn(List.of(activeRoute));

        assertThatThrownBy(() -> mockFlightGeneratorService.generateFlights(generationRequest, performedByUserId, clientIpAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verifyNoInteractions(flightService);
    }

    @Test
    @DisplayName("generateFlights - Aktif flight type bulunamadığında 409 CONFLICT fırlatmalıdır")
    void generateFlights_shouldThrowConflict_whenNoActiveFlightTypesExist() {
        when(referenceManagerClient.getAllAircrafts()).thenReturn(List.of(activeAircraft));
        when(referenceManagerClient.getAllRoutes()).thenReturn(List.of(activeRoute));
        activeFlightType.setFlightTypeStatus("INACTIVE");
        when(referenceManagerClient.getAllFlightTypes()).thenReturn(List.of(activeFlightType));

        assertThatThrownBy(() -> mockFlightGeneratorService.generateFlights(generationRequest, performedByUserId, clientIpAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verifyNoInteractions(flightService);
    }

    @Test
    @DisplayName("generateFlights - Seçilen uçak pasif bir havayoluna bağlıysa hataları raporlamalıdır")
    void generateFlights_shouldReportFailures_whenSelectedAircraftHasInactiveOperatorAirline() {
        when(referenceManagerClient.getAllAircrafts()).thenReturn(List.of(activeAircraft));
        when(referenceManagerClient.getAllRoutes()).thenReturn(List.of(activeRoute));
        when(referenceManagerClient.getAllFlightTypes()).thenReturn(List.of(activeFlightType));

        activeAirline.setAirlineStatus("INACTIVE");
        when(referenceManagerClient.getAirlineById(10L)).thenReturn(activeAirline);

        MockFlightGenerationResponse result = mockFlightGeneratorService.generateFlights(
                generationRequest,
                performedByUserId,
                clientIpAddress
        );

        assertThat(result.getRequestedCount()).isEqualTo(2);
        assertThat(result.getSuccessfulCount()).isZero();
        assertThat(result.getFailedCount()).isEqualTo(2);
        assertThat(result.getSuccessfulFlights()).isEmpty();
        assertThat(result.getErrors()).hasSize(2);

        verifyNoInteractions(flightService);
    }
}
