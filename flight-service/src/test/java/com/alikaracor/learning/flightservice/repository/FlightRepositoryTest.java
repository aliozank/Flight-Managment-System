package com.alikaracor.learning.flightservice.repository;

import com.alikaracor.learning.flightservice.model.Flight;
import com.alikaracor.learning.flightservice.model.FlightStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FlightRepositoryTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    @Autowired
    private FlightRepository flightRepository;

    private Flight createSampleFlight(String flightNumber, Long aircraftId, Instant departureAt, Instant arrivalAt, FlightStatus status) {
        Flight flight = new Flight();
        flight.setFlightNumber(flightNumber);
        flight.setAirlineId(10L);
        flight.setAircraftId(aircraftId);
        flight.setAircraftTypeId(20L);
        flight.setOriginAirportId(1L);
        flight.setDestinationAirportId(2L);
        flight.setFlightTypeId(5L);
        flight.setFlightDate(LocalDate.of(2026, 10, 1));
        flight.setScheduledDepartureTime(LocalTime.of(10, 0));
        flight.setScheduledArrivalTime(LocalTime.of(12, 0));
        flight.setScheduledArrivalDate(LocalDate.of(2026, 10, 1));
        flight.setScheduledDepartureAt(departureAt);
        flight.setScheduledArrivalAt(arrivalAt);
        flight.setFlightStatus(status);
        flight.setFlightVersion(1);
        return flight;
    }

    @Test
    @DisplayName("existsByAircraftId...ForCreate - Çakışan aktif uçuş olduğunda true dönmelidir")
    void existsByAircraftIdForCreate_shouldReturnTrue_whenActiveFlightOverlaps() {
        Instant dep1 = Instant.parse("2026-10-01T10:00:00Z");
        Instant arr1 = Instant.parse("2026-10-01T14:00:00Z");
        Flight existing = createSampleFlight("TK1001", 100L, dep1, arr1, FlightStatus.SCHEDULED);
        flightRepository.save(existing);

        Instant queryDep = Instant.parse("2026-10-01T12:00:00Z");
        Instant queryArr = Instant.parse("2026-10-01T16:00:00Z");

        boolean exists = flightRepository.existsByAircraftIdAndFlightStatusNotAndScheduledDepartureAtLessThanAndScheduledArrivalAtGreaterThan(
                100L, FlightStatus.CANCELLED, queryArr, queryDep
        );

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByAircraftId...ForCreate - Çakışan uçuş CANCELLED ise false dönmelidir")
    void existsByAircraftIdForCreate_shouldReturnFalse_whenOverlappingFlightIsCancelled() {
        Instant dep1 = Instant.parse("2026-10-01T10:00:00Z");
        Instant arr1 = Instant.parse("2026-10-01T14:00:00Z");
        Flight existing = createSampleFlight("TK1002", 100L, dep1, arr1, FlightStatus.CANCELLED);
        flightRepository.save(existing);

        Instant queryDep = Instant.parse("2026-10-01T12:00:00Z");
        Instant queryArr = Instant.parse("2026-10-01T16:00:00Z");

        boolean exists = flightRepository.existsByAircraftIdAndFlightStatusNotAndScheduledDepartureAtLessThanAndScheduledArrivalAtGreaterThan(
                100L, FlightStatus.CANCELLED, queryArr, queryDep
        );

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByAircraftId...ForCreate - Zaman penceresi çakışmıyorsa false dönmelidir")
    void existsByAircraftIdForCreate_shouldReturnFalse_whenTimeWindowDoesNotOverlap() {
        Instant dep1 = Instant.parse("2026-10-01T10:00:00Z");
        Instant arr1 = Instant.parse("2026-10-01T12:00:00Z");
        Flight existing = createSampleFlight("TK1003", 100L, dep1, arr1, FlightStatus.SCHEDULED);
        flightRepository.save(existing);

        Instant queryDep = Instant.parse("2026-10-01T13:00:00Z");
        Instant queryArr = Instant.parse("2026-10-01T15:00:00Z");

        boolean exists = flightRepository.existsByAircraftIdAndFlightStatusNotAndScheduledDepartureAtLessThanAndScheduledArrivalAtGreaterThan(
                100L, FlightStatus.CANCELLED, queryArr, queryDep
        );

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByAircraftId...ForUpdate - Kendisi hariç başka çakışan aktif uçuş yoksa false dönmelidir")
    void existsByAircraftIdForUpdate_shouldReturnFalse_whenOnlyCurrentFlightMatches() {
        Instant dep1 = Instant.parse("2026-10-01T10:00:00Z");
        Instant arr1 = Instant.parse("2026-10-01T14:00:00Z");
        Flight existing = createSampleFlight("TK1004", 100L, dep1, arr1, FlightStatus.SCHEDULED);
        Flight saved = flightRepository.save(existing);

        boolean exists = flightRepository.existsByAircraftIdAndFlightIdNotAndFlightStatusNotAndScheduledDepartureAtLessThanAndScheduledArrivalAtGreaterThan(
                100L, saved.getFlightId(), FlightStatus.CANCELLED, arr1, dep1
        );

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findAllByFlightStatusInAndScheduledDepartureAtLessThanEqual - Sadece belirtilen statülerdeki (SCHEDULED, DELAYED) uçuşları bulmalı, DEPARTED'ı bulmamalıdır")
    void findAllByFlightStatusIn_shouldFindScheduledAndDelayed_notDeparted() {
        Instant past = Instant.parse("2026-10-01T10:00:00Z");
        Instant now = Instant.parse("2026-10-01T12:00:00Z");

        Flight scheduled = createSampleFlight("TK2001", 101L, past, now.plusSeconds(3600), FlightStatus.SCHEDULED);
        Flight delayed = createSampleFlight("TK2002", 102L, past, now.plusSeconds(3600), FlightStatus.DELAYED);
        Flight departed = createSampleFlight("TK2003", 103L, past, now.plusSeconds(3600), FlightStatus.DEPARTED);

        flightRepository.save(scheduled);
        flightRepository.save(delayed);
        flightRepository.save(departed);

        java.util.List<Flight> results = flightRepository.findAllByFlightStatusInAndScheduledDepartureAtLessThanEqual(
                java.util.List.of(FlightStatus.SCHEDULED, FlightStatus.DELAYED), now
        );

        assertThat(results).hasSize(2);
        assertThat(results).extracting(Flight::getFlightStatus)
                .containsExactlyInAnyOrder(FlightStatus.SCHEDULED, FlightStatus.DELAYED)
                .doesNotContain(FlightStatus.DEPARTED);
    }
}
