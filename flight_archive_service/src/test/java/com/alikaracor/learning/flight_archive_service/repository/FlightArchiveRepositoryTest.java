package com.alikaracor.learning.flight_archive_service.repository;

import com.alikaracor.learning.flight_archive_service.model.ArchivedFlight;
import com.alikaracor.learning.flight_archive_service.model.FlightStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FlightArchiveRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private FlightArchiveRepository repository;

    private ArchivedFlight createSampleFlight(UUID eventId, Long flightId, String flightNumber) {
        ArchivedFlight flight = new ArchivedFlight();
        flight.setEventId(eventId);
        flight.setFlightId(flightId);
        flight.setFlightNumber(flightNumber);
        flight.setAirlineId(10L);
        flight.setAircraftTypeId(20L);
        flight.setOriginAirportId(1L);
        flight.setDestinationAirportId(2L);
        flight.setFlightTypeId(5L);
        flight.setFlightDate(LocalDate.of(2026, 10, 1));
        flight.setScheduledDepartureTime(LocalTime.of(10, 0));
        flight.setScheduledArrivalTime(LocalTime.of(12, 0));
        flight.setFlightStatus(FlightStatus.ARRIVED);
        flight.setFlightVersion(1);
        flight.setEventOccurredAt(Instant.now());
        return flight;
    }

    @Test
    @DisplayName("save & findByFlightId - Gerçek PostgreSQL veritabanına kaydetmeli ve flightId ile sorgulamalıdır")
    void saveAndFindByFlightId_shouldPersistAndRetrieveRecord() {
        UUID eventId = UUID.randomUUID();
        ArchivedFlight flight = createSampleFlight(eventId, 100L, "TK1234");

        ArchivedFlight saved = repository.save(flight);
        assertThat(saved.getArchiveId()).isNotNull();

        Optional<ArchivedFlight> found = repository.findByFlightId(100L);
        assertThat(found).isPresent();
        assertThat(found.get().getFlightNumber()).isEqualTo("TK1234");
    }

    @Test
    @DisplayName("existsByEventId - eventId varlığını PostgreSQL üzerinde doğru doğrulamalıdır")
    void existsByEventId_shouldReturnTrueWhenEventIdExists() {
        UUID eventId = UUID.randomUUID();
        ArchivedFlight flight = createSampleFlight(eventId, 200L, "TK5678");
        repository.save(flight);

        assertThat(repository.existsByEventId(eventId)).isTrue();
        assertThat(repository.existsByEventId(UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("eventId unique kısıtı - Aynı eventId ile iki kayıt eklendiğinde PostgreSQL DataIntegrityViolationException fırlatmalıdır")
    void save_shouldThrowException_whenEventIdIsDuplicate() {
        UUID eventId = UUID.randomUUID();
        ArchivedFlight flight1 = createSampleFlight(eventId, 301L, "TK111");
        repository.saveAndFlush(flight1);

        ArchivedFlight flight2 = createSampleFlight(eventId, 302L, "TK222");

        assertThatThrownBy(() -> repository.saveAndFlush(flight2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("flightId unique kısıtı - Aynı flightId ile iki kayıt eklendiğinde PostgreSQL DataIntegrityViolationException fırlatmalıdır")
    void save_shouldThrowException_whenFlightIdIsDuplicate() {
        ArchivedFlight flight1 = createSampleFlight(UUID.randomUUID(), 500L, "TK333");
        repository.saveAndFlush(flight1);

        ArchivedFlight flight2 = createSampleFlight(UUID.randomUUID(), 500L, "TK444");

        assertThatThrownBy(() -> repository.saveAndFlush(flight2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
