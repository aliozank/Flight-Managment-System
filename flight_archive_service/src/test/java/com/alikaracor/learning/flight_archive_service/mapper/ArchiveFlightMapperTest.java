package com.alikaracor.learning.flight_archive_service.mapper;

import com.alikaracor.learning.flight_archive_service.dto.ArchivedFlightResponse;
import com.alikaracor.learning.flight_archive_service.model.ArchivedFlight;
import com.alikaracor.learning.flight_archive_service.model.FlightStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ArchiveFlightMapperTest {

    private final ArchiveFlightMapper mapper = Mappers.getMapper(ArchiveFlightMapper.class);

    @Test
    @DisplayName("toArchivedFlightResponse - ArchivedFlight alanlarını ArchivedFlightResponse nesnesine eksiksiz dönüştürmelidir")
    void toArchivedFlightResponse_shouldMapAllFieldsCorrectly() {
        ArchivedFlight archivedFlight = new ArchivedFlight();
        archivedFlight.setArchiveId(1L);
        archivedFlight.setEventId(UUID.randomUUID());
        archivedFlight.setFlightId(100L);
        archivedFlight.setFlightNumber("TK1234");
        archivedFlight.setAirlineId(10L);
        archivedFlight.setAircraftId(20L);
        archivedFlight.setAircraftTypeId(30L);
        archivedFlight.setOriginAirportId(1L);
        archivedFlight.setDestinationAirportId(2L);
        archivedFlight.setFlightTypeId(5L);
        archivedFlight.setFlightDate(LocalDate.of(2026, 10, 1));
        archivedFlight.setScheduledDepartureTime(LocalTime.of(10, 0));
        archivedFlight.setScheduledArrivalTime(LocalTime.of(12, 0));
        archivedFlight.setFlightStatus(FlightStatus.ARRIVED);
        archivedFlight.setFlightVersion(2);
        archivedFlight.setChangedByUserId(99L);
        archivedFlight.setEventOccurredAt(Instant.now());
        archivedFlight.setArchivedAt(Instant.now());

        ArchivedFlightResponse response = mapper.toArchivedFlightResponse(archivedFlight);

        assertThat(response).isNotNull();
        assertThat(response.getArchiveId()).isEqualTo(archivedFlight.getArchiveId());
        assertThat(response.getEventId()).isEqualTo(archivedFlight.getEventId());
        assertThat(response.getFlightId()).isEqualTo(archivedFlight.getFlightId());
        assertThat(response.getFlightNumber()).isEqualTo(archivedFlight.getFlightNumber());
        assertThat(response.getAirlineId()).isEqualTo(archivedFlight.getAirlineId());
        assertThat(response.getAircraftId()).isEqualTo(archivedFlight.getAircraftId());
        assertThat(response.getAircraftTypeId()).isEqualTo(archivedFlight.getAircraftTypeId());
        assertThat(response.getOriginAirportId()).isEqualTo(archivedFlight.getOriginAirportId());
        assertThat(response.getDestinationAirportId()).isEqualTo(archivedFlight.getDestinationAirportId());
        assertThat(response.getFlightTypeId()).isEqualTo(archivedFlight.getFlightTypeId());
        assertThat(response.getFlightDate()).isEqualTo(archivedFlight.getFlightDate());
        assertThat(response.getScheduledDepartureTime()).isEqualTo(archivedFlight.getScheduledDepartureTime());
        assertThat(response.getScheduledArrivalTime()).isEqualTo(archivedFlight.getScheduledArrivalTime());
        assertThat(response.getFlightStatus()).isEqualTo(archivedFlight.getFlightStatus());
        assertThat(response.getFlightVersion()).isEqualTo(archivedFlight.getFlightVersion());
        assertThat(response.getChangedByUserId()).isEqualTo(archivedFlight.getChangedByUserId());
        assertThat(response.getEventOccurredAt()).isEqualTo(archivedFlight.getEventOccurredAt());
        assertThat(response.getArchivedAt()).isEqualTo(archivedFlight.getArchivedAt());
    }
}
