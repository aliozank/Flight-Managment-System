package com.alikaracor.learning.flightservice.service;

import com.alikaracor.learning.flightservice.model.FlightStatus;
import com.alikaracor.learning.flightservice.repository.FlightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AircraftScheduleConflictValidationService Tests")
class AircraftScheduleConflictValidationServiceTest {

    @Mock
    private FlightRepository flightRepository;

    private AircraftScheduleConflictValidationService validationService;

    private Instant departureAt;
    private Instant arrivalAt;

    @BeforeEach
    void setUp() {
        validationService = new AircraftScheduleConflictValidationService(flightRepository);
        departureAt = Instant.parse("2026-10-01T10:00:00Z");
        arrivalAt = Instant.parse("2026-10-01T12:00:00Z");
    }

    // ==================== validateAircraftScheduleForCreate Tests ====================

    @Test
    @DisplayName("validateAircraftScheduleForCreate - aircraftId null olduğunda doğrulama atlanmalıdır")
    void validateAircraftScheduleForCreate_shouldSkipValidation_whenAircraftIdIsNull() {
        assertDoesNotThrow(() ->
            validationService.validateAircraftScheduleForCreate(
                null,
                departureAt,
                arrivalAt
            )
        );
    }

    @Test
    @DisplayName("validateAircraftScheduleForCreate - Zaman çakışması olmadığında doğrulama geçmelidir")
    void validateAircraftScheduleForCreate_shouldPass_whenNoConflictExists() {
        Long aircraftId = 1L;

        when(flightRepository.existsByAircraftIdAndFlightStatusNotAndScheduledDepartureAtLessThanAndScheduledArrivalAtGreaterThan(
                aircraftId, FlightStatus.CANCELLED, arrivalAt, departureAt))
                .thenReturn(false);

        assertDoesNotThrow(() ->
            validationService.validateAircraftScheduleForCreate(
                aircraftId,
                departureAt,
                arrivalAt
            )
        );
    }

    @Test
    @DisplayName("validateAircraftScheduleForCreate - Zaman çakışması olduğunda 409 CONFLICT fırlatılmalıdır")
    void validateAircraftScheduleForCreate_shouldThrowConflict_whenScheduleConflicts() {
        Long aircraftId = 1L;

        when(flightRepository.existsByAircraftIdAndFlightStatusNotAndScheduledDepartureAtLessThanAndScheduledArrivalAtGreaterThan(
                aircraftId, FlightStatus.CANCELLED, arrivalAt, departureAt))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            validationService.validateAircraftScheduleForCreate(
                aircraftId,
                departureAt,
                arrivalAt
            )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("occupied"));
    }

    // ==================== validateAircraftScheduleForUpdate Tests ====================

    @Test
    @DisplayName("validateAircraftScheduleForUpdate - aircraftId null olduğunda doğrulama atlanmalıdır")
    void validateAircraftScheduleForUpdate_shouldSkipValidation_whenAircraftIdIsNull() {
        assertDoesNotThrow(() ->
            validationService.validateAircraftScheduleForUpdate(
                1L,
                null,
                departureAt,
                arrivalAt
            )
        );
    }

    @Test
    @DisplayName("validateAircraftScheduleForUpdate - Başka uçuşlarla çakışma olmadığında doğrulama geçmelidir")
    void validateAircraftScheduleForUpdate_shouldPass_whenNoConflictWithOtherFlights() {
        Long flightId = 1L;
        Long aircraftId = 1L;

        when(flightRepository.existsByAircraftIdAndFlightIdNotAndFlightStatusNotAndScheduledDepartureAtLessThanAndScheduledArrivalAtGreaterThan(
                aircraftId, flightId, FlightStatus.CANCELLED, arrivalAt, departureAt))
                .thenReturn(false);

        assertDoesNotThrow(() ->
            validationService.validateAircraftScheduleForUpdate(
                flightId,
                aircraftId,
                departureAt,
                arrivalAt
            )
        );
    }

    @Test
    @DisplayName("validateAircraftScheduleForUpdate - Başka uçuşla çakışma olduğunda 409 CONFLICT fırlatılmalıdır")
    void validateAircraftScheduleForUpdate_shouldThrowConflict_whenConflictsWithDifferentFlight() {
        Long flightId = 1L;
        Long aircraftId = 1L;

        when(flightRepository.existsByAircraftIdAndFlightIdNotAndFlightStatusNotAndScheduledDepartureAtLessThanAndScheduledArrivalAtGreaterThan(
                aircraftId, flightId, FlightStatus.CANCELLED, arrivalAt, departureAt))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            validationService.validateAircraftScheduleForUpdate(
                flightId,
                aircraftId,
                departureAt,
                arrivalAt
            )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("occupied"));
    }
}
