package com.alikaracor.learning.flightservice.service;

import com.alikaracor.learning.flightservice.model.Flight;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AircraftScheduleConflictValidationService Tests")
class AircraftScheduleConflictValidationServiceTest {

    @Mock
    private FlightRepository flightRepository;

    private AircraftScheduleConflictValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new AircraftScheduleConflictValidationService(flightRepository);
    }

    // ==================== validateAircraftScheduleForCreate Tests ====================

    @Test
    @DisplayName("validateAircraftScheduleForCreate - aircraftId null olduğunda doğrulama atlanmalıdır")
    void validateAircraftScheduleForCreate_shouldSkipValidation_whenAircraftIdIsNull() {
        assertDoesNotThrow(() ->
            validationService.validateAircraftScheduleForCreate(
                null,
                LocalDate.of(2026, 10, 1),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
            )
        );
    }

    @Test
    @DisplayName("validateAircraftScheduleForCreate - Aynı tarihte aktif uçuş olmadığında doğrulama geçmelidir")
    void validateAircraftScheduleForCreate_shouldPass_whenNoActiveFlightsOnDate() {
        Long aircraftId = 1L;
        LocalDate flightDate = LocalDate.of(2026, 10, 1);

        when(flightRepository.findActiveFlightsByAircraftIdAndFlightDate(aircraftId, flightDate, FlightStatus.CANCELLED))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() ->
            validationService.validateAircraftScheduleForCreate(
                aircraftId,
                flightDate,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
            )
        );
    }

    @Test
    @DisplayName("validateAircraftScheduleForCreate - Kısmi çakışma (yeni uçuş mevcut aralığın ortasında) exception fırlatmalıdır")
    void validateAircraftScheduleForCreate_shouldThrowConflict_whenNewFlightPartiallyOverlapsExisting() {
        Long aircraftId = 1L;
        LocalDate flightDate = LocalDate.of(2026, 10, 1);

        Flight existingFlight = new Flight();
        existingFlight.setFlightId(1L);
        existingFlight.setScheduledDepartureTime(LocalTime.of(10, 0));
        existingFlight.setScheduledArrivalTime(LocalTime.of(12, 0));
        existingFlight.setFlightStatus(FlightStatus.SCHEDULED);

        when(flightRepository.findActiveFlightsByAircraftIdAndFlightDate(aircraftId, flightDate, FlightStatus.CANCELLED))
                .thenReturn(List.of(existingFlight));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            validationService.validateAircraftScheduleForCreate(
                aircraftId,
                flightDate,
                LocalTime.of(11, 30),
                LocalTime.of(13, 0)
            )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("occupied"));
    }

    @Test
    @DisplayName("validateAircraftScheduleForCreate - Yeni aralık mevcut aralığı tamamen kaplamalı exception fırlatmalıdır")
    void validateAircraftScheduleForCreate_shouldThrowConflict_whenNewFlightCompletelyCoversExisting() {
        Long aircraftId = 1L;
        LocalDate flightDate = LocalDate.of(2026, 10, 1);

        Flight existingFlight = new Flight();
        existingFlight.setFlightId(1L);
        existingFlight.setScheduledDepartureTime(LocalTime.of(11, 0));
        existingFlight.setScheduledArrivalTime(LocalTime.of(12, 0));
        existingFlight.setFlightStatus(FlightStatus.SCHEDULED);

        when(flightRepository.findActiveFlightsByAircraftIdAndFlightDate(aircraftId, flightDate, FlightStatus.CANCELLED))
                .thenReturn(List.of(existingFlight));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            validationService.validateAircraftScheduleForCreate(
                aircraftId,
                flightDate,
                LocalTime.of(10, 0),
                LocalTime.of(13, 0)
            )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    @DisplayName("validateAircraftScheduleForCreate - Mevcut aralık yeni aralığı tamamen kaplamalı exception fırlatmalıdır")
    void validateAircraftScheduleForCreate_shouldThrowConflict_whenExistingFlightCompletelyCoversNew() {
        Long aircraftId = 1L;
        LocalDate flightDate = LocalDate.of(2026, 10, 1);

        Flight existingFlight = new Flight();
        existingFlight.setFlightId(1L);
        existingFlight.setScheduledDepartureTime(LocalTime.of(10, 0));
        existingFlight.setScheduledArrivalTime(LocalTime.of(14, 0));
        existingFlight.setFlightStatus(FlightStatus.SCHEDULED);

        when(flightRepository.findActiveFlightsByAircraftIdAndFlightDate(aircraftId, flightDate, FlightStatus.CANCELLED))
                .thenReturn(List.of(existingFlight));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            validationService.validateAircraftScheduleForCreate(
                aircraftId,
                flightDate,
                LocalTime.of(11, 0),
                LocalTime.of(13, 0)
            )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    @DisplayName("validateAircraftScheduleForCreate - Yeni uçuş tam mevcut uçuşun bitişinde başlıyorsa çakışma olmaz (12:00-14:00 ve 10:00-12:00)")
    void validateAircraftScheduleForCreate_shouldPass_whenNewFlightStartsExactlyWhenExistingEnds() {
        Long aircraftId = 1L;
        LocalDate flightDate = LocalDate.of(2026, 10, 1);

        Flight existingFlight = new Flight();
        existingFlight.setFlightId(1L);
        existingFlight.setScheduledDepartureTime(LocalTime.of(10, 0));
        existingFlight.setScheduledArrivalTime(LocalTime.of(12, 0));
        existingFlight.setFlightStatus(FlightStatus.SCHEDULED);

        when(flightRepository.findActiveFlightsByAircraftIdAndFlightDate(aircraftId, flightDate, FlightStatus.CANCELLED))
                .thenReturn(List.of(existingFlight));

        assertDoesNotThrow(() ->
            validationService.validateAircraftScheduleForCreate(
                aircraftId,
                flightDate,
                LocalTime.of(12, 0),
                LocalTime.of(14, 0)
            )
        );
    }

    @Test
    @DisplayName("validateAircraftScheduleForCreate - Yeni uçuş tam mevcut uçuşun bitişinden sonra başlıyorsa çakışma olmaz")
    void validateAircraftScheduleForCreate_shouldPass_whenNewFlightStartsAfterExistingEnds() {
        Long aircraftId = 1L;
        LocalDate flightDate = LocalDate.of(2026, 10, 1);

        Flight existingFlight = new Flight();
        existingFlight.setFlightId(1L);
        existingFlight.setScheduledDepartureTime(LocalTime.of(10, 0));
        existingFlight.setScheduledArrivalTime(LocalTime.of(12, 0));
        existingFlight.setFlightStatus(FlightStatus.SCHEDULED);

        when(flightRepository.findActiveFlightsByAircraftIdAndFlightDate(aircraftId, flightDate, FlightStatus.CANCELLED))
                .thenReturn(List.of(existingFlight));

        assertDoesNotThrow(() ->
            validationService.validateAircraftScheduleForCreate(
                aircraftId,
                flightDate,
                LocalTime.of(12, 1),
                LocalTime.of(14, 0)
            )
        );
    }

    @Test
    @DisplayName("validateAircraftScheduleForCreate - Yeni uçuş tam mevcut uçuşun başlangıcında bitiyorsa çakışma olmaz")
    void validateAircraftScheduleForCreate_shouldPass_whenNewFlightEndsExactlyWhenExistingStarts() {
        Long aircraftId = 1L;
        LocalDate flightDate = LocalDate.of(2026, 10, 1);

        Flight existingFlight = new Flight();
        existingFlight.setFlightId(1L);
        existingFlight.setScheduledDepartureTime(LocalTime.of(12, 0));
        existingFlight.setScheduledArrivalTime(LocalTime.of(14, 0));
        existingFlight.setFlightStatus(FlightStatus.SCHEDULED);

        when(flightRepository.findActiveFlightsByAircraftIdAndFlightDate(aircraftId, flightDate, FlightStatus.CANCELLED))
                .thenReturn(List.of(existingFlight));

        assertDoesNotThrow(() ->
            validationService.validateAircraftScheduleForCreate(
                aircraftId,
                flightDate,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
            )
        );
    }

    @Test
    @DisplayName("validateAircraftScheduleForCreate - CANCELLED uçuş engel olmamalı")
    void validateAircraftScheduleForCreate_shouldPass_whenExistingFlightIsCancelled() {
        Long aircraftId = 1L;
        LocalDate flightDate = LocalDate.of(2026, 10, 1);

        when(flightRepository.findActiveFlightsByAircraftIdAndFlightDate(aircraftId, flightDate, FlightStatus.CANCELLED))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() ->
            validationService.validateAircraftScheduleForCreate(
                aircraftId,
                flightDate,
                LocalTime.of(11, 0),
                LocalTime.of(13, 0)
            )
        );
    }

    @Test
    @DisplayName("validateAircraftScheduleForCreate - Farklı uçak aynı zaman aralığı engel olmamalı")
    void validateAircraftScheduleForCreate_shouldPass_whenDifferentAircraftHasConflictingTime() {
        Long aircraftId = 1L;
        LocalDate flightDate = LocalDate.of(2026, 10, 1);

        when(flightRepository.findActiveFlightsByAircraftIdAndFlightDate(aircraftId, flightDate, FlightStatus.CANCELLED))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() ->
            validationService.validateAircraftScheduleForCreate(
                aircraftId,
                flightDate,
                LocalTime.of(11, 0),
                LocalTime.of(13, 0)
            )
        );
    }

    @Test
    @DisplayName("validateAircraftScheduleForCreate - Farklı tarih aynı uçak aynı zaman engel olmamalı")
    void validateAircraftScheduleForCreate_shouldPass_whenDifferentDateHasConflictingTime() {
        Long aircraftId = 1L;
        LocalDate flightDate = LocalDate.of(2026, 10, 1);

        when(flightRepository.findActiveFlightsByAircraftIdAndFlightDate(aircraftId, flightDate, FlightStatus.CANCELLED))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() ->
            validationService.validateAircraftScheduleForCreate(
                aircraftId,
                flightDate,
                LocalTime.of(11, 0),
                LocalTime.of(13, 0)
            )
        );
    }

    // ==================== validateAircraftScheduleForUpdate Tests ====================

    @Test
    @DisplayName("validateAircraftScheduleForUpdate - aircraftId null olduğunda doğrulama atlanmalıdır")
    void validateAircraftScheduleForUpdate_shouldSkipValidation_whenAircraftIdIsNull() {
        assertDoesNotThrow(() ->
            validationService.validateAircraftScheduleForUpdate(
                1L,
                null,
                LocalDate.of(2026, 10, 1),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
            )
        );
    }

    @Test
    @DisplayName("validateAircraftScheduleForUpdate - Güncellenen uçuş kendisiyle karşılaştırılmamalı")
    void validateAircraftScheduleForUpdate_shouldExcludeCurrentFlight_whenFlightIdMatches() {
        Long flightId = 1L;
        Long aircraftId = 1L;
        LocalDate flightDate = LocalDate.of(2026, 10, 1);

        Flight sameFlight = new Flight();
        sameFlight.setFlightId(flightId);
        sameFlight.setScheduledDepartureTime(LocalTime.of(10, 0));
        sameFlight.setScheduledArrivalTime(LocalTime.of(12, 0));
        sameFlight.setFlightStatus(FlightStatus.SCHEDULED);

        when(flightRepository.findActiveFlightsByAircraftIdAndFlightDate(aircraftId, flightDate, FlightStatus.CANCELLED))
                .thenReturn(List.of(sameFlight));

        assertDoesNotThrow(() ->
            validationService.validateAircraftScheduleForUpdate(
                flightId,
                aircraftId,
                flightDate,
                LocalTime.of(11, 0),
                LocalTime.of(13, 0)
            )
        );
    }

    @Test
    @DisplayName("validateAircraftScheduleForUpdate - Farklı uçuşla çakışma engellenmeli")
    void validateAircraftScheduleForUpdate_shouldThrowConflict_whenConflictsWithDifferentFlight() {
        Long currentFlightId = 1L;
        Long aircraftId = 1L;
        LocalDate flightDate = LocalDate.of(2026, 10, 1);

        Flight otherFlight = new Flight();
        otherFlight.setFlightId(2L);
        otherFlight.setScheduledDepartureTime(LocalTime.of(10, 0));
        otherFlight.setScheduledArrivalTime(LocalTime.of(12, 0));
        otherFlight.setFlightStatus(FlightStatus.SCHEDULED);

        when(flightRepository.findActiveFlightsByAircraftIdAndFlightDate(aircraftId, flightDate, FlightStatus.CANCELLED))
                .thenReturn(List.of(otherFlight));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            validationService.validateAircraftScheduleForUpdate(
                currentFlightId,
                aircraftId,
                flightDate,
                LocalTime.of(11, 0),
                LocalTime.of(13, 0)
            )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    @DisplayName("validateAircraftScheduleForUpdate - Güncellemede CANCELLED uçuş engel olmamalı")
    void validateAircraftScheduleForUpdate_shouldPass_whenOnlyActiveCancelledFlightExists() {
        Long currentFlightId = 1L;
        Long aircraftId = 1L;
        LocalDate flightDate = LocalDate.of(2026, 10, 1);

        when(flightRepository.findActiveFlightsByAircraftIdAndFlightDate(aircraftId, flightDate, FlightStatus.CANCELLED))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() ->
            validationService.validateAircraftScheduleForUpdate(
                currentFlightId,
                aircraftId,
                flightDate,
                LocalTime.of(11, 0),
                LocalTime.of(13, 0)
            )
        );
    }

}
