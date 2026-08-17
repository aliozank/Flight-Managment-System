package com.alikaracor.learning.flightservice.service;

import com.alikaracor.learning.flightservice.client.ReferenceManagerClient;
import com.alikaracor.learning.flightservice.client.dto.*;
import com.alikaracor.learning.flightservice.dto.FlightCreateRequest;
import com.alikaracor.learning.flightservice.dto.FlightUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightReferenceValidationServiceTest {

    @Mock
    private ReferenceManagerClient referenceManagerClient;

    @InjectMocks
    private FlightReferenceValidationService validationService;

    // ==================== validateAirline Tests ====================

    @Test
    @DisplayName("validateAirline - Havayolu statüsü ACTIVE olduğunda hata fırlatmamalıdır")
    void validateAirline_shouldPass_whenAirlineIsActive() {
        AirlineReferenceResponse response = new AirlineReferenceResponse();
        response.setAirlineId(10L);
        response.setAirlineStatus("ACTIVE");

        when(referenceManagerClient.getAirlineById(10L)).thenReturn(response);

        assertThatCode(() -> validationService.validateAirline(10L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateAirline - Havayolu statüsü ACTIVE olmadığında 400 BAD_REQUEST fırlatmalıdır")
    void validateAirline_shouldThrowException_whenAirlineIsNotActive() {
        AirlineReferenceResponse response = new AirlineReferenceResponse();
        response.setAirlineId(10L);
        response.setAirlineStatus("INACTIVE");

        when(referenceManagerClient.getAirlineById(10L)).thenReturn(response);

        assertThatThrownBy(() -> validationService.validateAirline(10L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("validateAirline - ReferenceManagerClient istisna fırlattığında hatayı olduğu gibi iletmelidir")
    void validateAirline_shouldPropagateException_whenReferenceManagerClientFails() {
        when(referenceManagerClient.getAirlineById(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Airline not found"));

        assertThatThrownBy(() -> validationService.validateAirline(99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ==================== validateAircraftType Tests ====================

    @Test
    @DisplayName("validateAircraftType - Uçak tipi ACTIVE olduğunda hata fırlatmamalıdır")
    void validateAircraftType_shouldPass_whenAircraftTypeIsActive() {
        AircraftTypeReferenceResponse response = new AircraftTypeReferenceResponse();
        response.setAircraftTypeId(20L);
        response.setAircraftTypeStatus("ACTIVE");

        when(referenceManagerClient.getAircraftTypeById(20L)).thenReturn(response);

        assertThatCode(() -> validationService.validateAircraftType(20L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateAircraftType - Uçak tipi ACTIVE olmadığında 400 BAD_REQUEST fırlatmalıdır")
    void validateAircraftType_shouldThrowException_whenAircraftTypeIsNotActive() {
        AircraftTypeReferenceResponse response = new AircraftTypeReferenceResponse();
        response.setAircraftTypeId(20L);
        response.setAircraftTypeStatus("DISABLED");

        when(referenceManagerClient.getAircraftTypeById(20L)).thenReturn(response);

        assertThatThrownBy(() -> validationService.validateAircraftType(20L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ==================== validateAirport Tests ====================

    @Test
    @DisplayName("validateAirport - Havalimanı OPERATIONAL olduğunda hata fırlatmamalıdır")
    void validateAirport_shouldPass_whenAirportIsOperational() {
        AirportReferenceResponse response = new AirportReferenceResponse();
        response.setAirportId(1L);
        response.setAirportStatus("OPERATIONAL");

        when(referenceManagerClient.getAirportById(1L)).thenReturn(response);

        assertThatCode(() -> validationService.validateAirport(1L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateAirport - Havalimanı OPERATIONAL olmadığında 400 BAD_REQUEST fırlatmalıdır")
    void validateAirport_shouldThrowException_whenAirportIsNotOperational() {
        AirportReferenceResponse response = new AirportReferenceResponse();
        response.setAirportId(1L);
        response.setAirportStatus("CLOSED");

        when(referenceManagerClient.getAirportById(1L)).thenReturn(response);

        assertThatThrownBy(() -> validationService.validateAirport(1L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ==================== validateFlightType Tests ====================

    @Test
    @DisplayName("validateFlightType - Uçuş tipi ACTIVE olduğunda hata fırlatmamalıdır")
    void validateFlightType_shouldPass_whenFlightTypeIsActive() {
        FlightTypeReferenceResponse response = new FlightTypeReferenceResponse();
        response.setFlightTypeId(5L);
        response.setFlightTypeStatus("ACTIVE");

        when(referenceManagerClient.getFlightTypeById(5L)).thenReturn(response);

        assertThatCode(() -> validationService.validateFlightType(5L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateFlightType - Uçuş tipi ACTIVE olmadığında 400 BAD_REQUEST fırlatmalıdır")
    void validateFlightType_shouldThrowException_whenFlightTypeIsNotActive() {
        FlightTypeReferenceResponse response = new FlightTypeReferenceResponse();
        response.setFlightTypeId(5L);
        response.setFlightTypeStatus("INACTIVE");

        when(referenceManagerClient.getFlightTypeById(5L)).thenReturn(response);

        assertThatThrownBy(() -> validationService.validateFlightType(5L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ==================== validateAircraft Tests ====================

    @Test
    @DisplayName("validateAircraft - aircraftId null olduğunda pas geçmelidir")
    void validateAircraft_shouldDoNothing_whenAircraftIdIsNull() {
        assertThatCode(() -> validationService.validateAircraft(null, 20L, 10L))
                .doesNotThrowAnyException();

        verifyNoInteractions(referenceManagerClient);
    }

    @Test
    @DisplayName("validateAircraft - Uçak ACTIVE ve seçilen tipe ait olduğunda geçmelidir")
    void validateAircraft_shouldPass_whenAircraftIsActiveAndMatchesType() {
        AircraftReferenceResponse response = new AircraftReferenceResponse();
        response.setAircraftId(100L);
        response.setAircraftTypeId(20L);
        response.setOperatorAirlineId(10L);
        response.setAircraftStatus("ACTIVE");

        when(referenceManagerClient.getAircraftById(100L)).thenReturn(response);

        assertThatCode(() -> validationService.validateAircraft(100L, 20L, 10L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateAircraft - Uçak ACTIVE değilse 400 BAD_REQUEST fırlatmalıdır")
    void validateAircraft_shouldThrowException_whenAircraftIsNotActive() {
        AircraftReferenceResponse response = new AircraftReferenceResponse();
        response.setAircraftId(100L);
        response.setAircraftTypeId(20L);
        response.setAircraftStatus("MAINTENANCE");

        when(referenceManagerClient.getAircraftById(100L)).thenReturn(response);

        assertThatThrownBy(() -> validationService.validateAircraft(100L, 20L, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("validateAircraft - Uçak tipi eşleşmiyorsa 400 BAD_REQUEST fırlatmalıdır")
    void validateAircraft_shouldThrowException_whenAircraftTypeMismatches() {
        AircraftReferenceResponse response = new AircraftReferenceResponse();
        response.setAircraftId(100L);
        response.setAircraftTypeId(999L); // Farklı tip
        response.setAircraftStatus("ACTIVE");

        when(referenceManagerClient.getAircraftById(100L)).thenReturn(response);

        assertThatThrownBy(() -> validationService.validateAircraft(100L, 20L, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ==================== validateRoute Tests ====================

    @Test
    @DisplayName("validateRoute - Kalkış ve varış aynı havalimanı ise 400 BAD_REQUEST fırlatmalıdır")
    void validateRoute_shouldThrowException_whenOriginAndDestinationAreSame() {
        assertThatThrownBy(() -> validationService.validateRoute(1L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(referenceManagerClient);
    }

    @Test
    @DisplayName("validateRoute - Rota ACTIVE olduğunda hata fırlatmamalıdır")
    void validateRoute_shouldPass_whenRouteIsActive() {
        RouteReferenceResponse response = new RouteReferenceResponse();
        response.setRouteId(50L);
        response.setRouteStatus("ACTIVE");

        when(referenceManagerClient.getActiveRoute(1L, 2L)).thenReturn(response);

        assertThatCode(() -> validationService.validateRoute(1L, 2L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateRoute - Rota ACTIVE olmadığında 400 BAD_REQUEST fırlatmalıdır")
    void validateRoute_shouldThrowException_whenRouteIsNotActive() {
        RouteReferenceResponse response = new RouteReferenceResponse();
        response.setRouteId(50L);
        response.setRouteStatus("INACTIVE");

        when(referenceManagerClient.getActiveRoute(1L, 2L)).thenReturn(response);

        assertThatThrownBy(() -> validationService.validateRoute(1L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ==================== validateCreateRequest / validateUpdateRequest Tests ====================

    @Test
    @DisplayName("validateCreateRequest - Tüm doğrulamalar başarılı olduğunda geçmelidir")
    void validateCreateRequest_shouldValidateAllFieldsSuccessfully() {
        FlightCreateRequest request = new FlightCreateRequest();
        request.setAirlineId(10L);
        request.setFlightNumber("TK1234");
        request.setAircraftTypeId(20L);
        request.setFlightTypeId(5L);
        request.setOriginAirportId(1L);
        request.setDestinationAirportId(2L);
        request.setAircraftId(100L);

        AirlineReferenceResponse airline = new AirlineReferenceResponse(); airline.setAirlineStatus("ACTIVE"); airline.setAirlineIataCode("TK");
        AircraftTypeReferenceResponse type = new AircraftTypeReferenceResponse(); type.setAircraftTypeStatus("ACTIVE");
        FlightTypeReferenceResponse flightType = new FlightTypeReferenceResponse(); flightType.setFlightTypeStatus("ACTIVE");
        AirportReferenceResponse origin = new AirportReferenceResponse(); origin.setAirportStatus("OPERATIONAL");
        AirportReferenceResponse dest = new AirportReferenceResponse(); dest.setAirportStatus("OPERATIONAL");
        RouteReferenceResponse route = new RouteReferenceResponse(); route.setRouteStatus("ACTIVE");
        AircraftReferenceResponse aircraft = new AircraftReferenceResponse(); aircraft.setAircraftStatus("ACTIVE"); aircraft.setAircraftTypeId(20L); aircraft.setOperatorAirlineId(10L);

        when(referenceManagerClient.getAirlineById(10L)).thenReturn(airline);
        when(referenceManagerClient.getAircraftTypeById(20L)).thenReturn(type);
        when(referenceManagerClient.getFlightTypeById(5L)).thenReturn(flightType);
        when(referenceManagerClient.getAirportById(1L)).thenReturn(origin);
        when(referenceManagerClient.getAirportById(2L)).thenReturn(dest);
        when(referenceManagerClient.getActiveRoute(1L, 2L)).thenReturn(route);
        when(referenceManagerClient.getAircraftById(100L)).thenReturn(aircraft);

        assertThatCode(() -> validationService.validateCreateRequest(request))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateUpdateRequest - Tüm doğrulamalar başarılı olduğunda geçmelidir")
    void validateUpdateRequest_shouldValidateAllFieldsSuccessfully() {
        FlightUpdateRequest request = new FlightUpdateRequest();
        request.setAirlineId(10L);
        request.setFlightNumber("TK1234");
        request.setAircraftTypeId(20L);
        request.setFlightTypeId(5L);
        request.setOriginAirportId(1L);
        request.setDestinationAirportId(2L);
        request.setAircraftId(null); // Optional aircraft

        AirlineReferenceResponse airline = new AirlineReferenceResponse(); airline.setAirlineStatus("ACTIVE"); airline.setAirlineIataCode("TK");
        AircraftTypeReferenceResponse type = new AircraftTypeReferenceResponse(); type.setAircraftTypeStatus("ACTIVE");
        FlightTypeReferenceResponse flightType = new FlightTypeReferenceResponse(); flightType.setFlightTypeStatus("ACTIVE");
        AirportReferenceResponse origin = new AirportReferenceResponse(); origin.setAirportStatus("OPERATIONAL");
        AirportReferenceResponse dest = new AirportReferenceResponse(); dest.setAirportStatus("OPERATIONAL");
        RouteReferenceResponse route = new RouteReferenceResponse(); route.setRouteStatus("ACTIVE");

        when(referenceManagerClient.getAirlineById(10L)).thenReturn(airline);
        when(referenceManagerClient.getAircraftTypeById(20L)).thenReturn(type);
        when(referenceManagerClient.getFlightTypeById(5L)).thenReturn(flightType);
        when(referenceManagerClient.getAirportById(1L)).thenReturn(origin);
        when(referenceManagerClient.getAirportById(2L)).thenReturn(dest);
        when(referenceManagerClient.getActiveRoute(1L, 2L)).thenReturn(route);

        assertThatCode(() -> validationService.validateUpdateRequest(request))
                .doesNotThrowAnyException();
    }
}
