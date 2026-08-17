package com.alikaracor.learning.flightservice.scheduler;

import com.alikaracor.learning.flightservice.model.Flight;
import com.alikaracor.learning.flightservice.model.FlightStatus;
import com.alikaracor.learning.flightservice.repository.FlightRepository;
import com.alikaracor.learning.flightservice.service.FlightService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightStatusSchedulerTest {

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private FlightService flightService;

    @InjectMocks
    private FlightStatusScheduler flightStatusScheduler;

    @Test
    @DisplayName("SCHEDULED and DELAYED flights past departure should be updated to DEPARTED")
    void shouldUpdateScheduledAndDelayedToDeparted() {
        Flight f1 = new Flight();
        f1.setFlightId(1L);
        f1.setFlightStatus(FlightStatus.SCHEDULED);

        Flight f2 = new Flight();
        f2.setFlightId(2L);
        f2.setFlightStatus(FlightStatus.DELAYED);

        when(flightRepository.findAllByFlightStatusInAndScheduledDepartureAtLessThanEqual(
                eq(List.of(FlightStatus.SCHEDULED, FlightStatus.DELAYED)), any(Instant.class)))
                .thenReturn(List.of(f1, f2));

        when(flightRepository.findAllByFlightStatusAndScheduledArrivalAtLessThanEqual(
                eq(FlightStatus.DEPARTED), any(Instant.class)))
                .thenReturn(List.of());

        flightStatusScheduler.updateFlightStatuses();

        verify(flightService).updateFlightStatusAutomatically(1L, FlightStatus.DEPARTED);
        verify(flightService).updateFlightStatusAutomatically(2L, FlightStatus.DEPARTED);
    }

    @Test
    @DisplayName("DEPARTED flights past arrival should be updated to ARRIVED")
    void shouldUpdateDepartedToArrived() {
        Flight f3 = new Flight();
        f3.setFlightId(3L);
        f3.setFlightStatus(FlightStatus.DEPARTED);

        when(flightRepository.findAllByFlightStatusInAndScheduledDepartureAtLessThanEqual(
                eq(List.of(FlightStatus.SCHEDULED, FlightStatus.DELAYED)), any(Instant.class)))
                .thenReturn(List.of());

        when(flightRepository.findAllByFlightStatusAndScheduledArrivalAtLessThanEqual(
                eq(FlightStatus.DEPARTED), any(Instant.class)))
                .thenReturn(List.of(f3));

        flightStatusScheduler.updateFlightStatuses();

        verify(flightService).updateFlightStatusAutomatically(3L, FlightStatus.ARRIVED);
    }

    @Test
    @DisplayName("Failed update -> RuntimeException caught, loop continues")
    void shouldCatchRuntimeExceptionAndContinueLoop() {
        Flight f1 = new Flight();
        f1.setFlightId(1L);
        Flight f2 = new Flight();
        f2.setFlightId(2L);

        when(flightRepository.findAllByFlightStatusInAndScheduledDepartureAtLessThanEqual(
                eq(List.of(FlightStatus.SCHEDULED, FlightStatus.DELAYED)), any(Instant.class)))
                .thenReturn(List.of(f1, f2));

        when(flightRepository.findAllByFlightStatusAndScheduledArrivalAtLessThanEqual(
                eq(FlightStatus.DEPARTED), any(Instant.class)))
                .thenReturn(List.of());

        doThrow(new RuntimeException("Test Exception"))
                .when(flightService).updateFlightStatusAutomatically(1L, FlightStatus.DEPARTED);

        flightStatusScheduler.updateFlightStatuses();

        verify(flightService).updateFlightStatusAutomatically(1L, FlightStatus.DEPARTED);
        verify(flightService).updateFlightStatusAutomatically(2L, FlightStatus.DEPARTED); // Loop continued
    }
}
