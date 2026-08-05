package com.alikaracor.learning.flight_archive_service.service;

import com.alikaracor.learning.flight_archive_service.dto.ArchivedFlightResponse;
import com.alikaracor.learning.flight_archive_service.event.FlightEvent;
import com.alikaracor.learning.flight_archive_service.mapper.ArchiveFlightMapper;
import com.alikaracor.learning.flight_archive_service.model.ArchivedFlight;
import com.alikaracor.learning.flight_archive_service.model.FlightStatus;
import com.alikaracor.learning.flight_archive_service.repository.FlightArchiveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightArchiveServiceTest {

    @Mock
    private FlightArchiveRepository flightArchiveRepository;

    @Mock
    private ArchiveFlightMapper archiveFlightMapper;

    @InjectMocks
    private FlightArchiveService flightArchiveService;

    private FlightEvent sampleEvent;
    private ArchivedFlight sampleArchivedFlight;
    private ArchivedFlightResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleEvent = new FlightEvent();
        sampleEvent.setEventId(UUID.randomUUID());
        sampleEvent.setFlightId(100L);
        sampleEvent.setFlightNumber("TK1234");
        sampleEvent.setAirlineId(10L);
        sampleEvent.setAircraftId(20L);
        sampleEvent.setAircraftTypeId(30L);
        sampleEvent.setOriginAirportId(1L);
        sampleEvent.setDestinationAirportId(2L);
        sampleEvent.setFlightTypeId(5L);
        sampleEvent.setFlightDate(LocalDate.of(2026, 10, 1));
        sampleEvent.setScheduledDepartureTime(LocalTime.of(10, 0));
        sampleEvent.setScheduledArrivalTime(LocalTime.of(12, 0));
        sampleEvent.setFlightStatus(FlightStatus.ARRIVED);
        sampleEvent.setFlightVersion(2);
        sampleEvent.setChangedByUserId(99L);
        sampleEvent.setOccurredAt(Instant.now());

        sampleArchivedFlight = new ArchivedFlight();
        sampleArchivedFlight.setArchiveId(1L);
        sampleArchivedFlight.setFlightId(100L);
        sampleArchivedFlight.setFlightVersion(2);

        sampleResponse = new ArchivedFlightResponse();
        sampleResponse.setArchiveId(1L);
        sampleResponse.setFlightId(100L);
    }

    @Test
    @DisplayName("archiveFlight - ARRIVED statüsündeki event yeni ArchivedFlight olarak kaydedilmelidir")
    void archiveFlight_shouldSaveNewArchivedFlight_whenEventStatusIsArrived() {
        when(flightArchiveRepository.existsByEventId(sampleEvent.getEventId())).thenReturn(false);
        when(flightArchiveRepository.findByFlightId(100L)).thenReturn(Optional.empty());

        flightArchiveService.archiveFlight(sampleEvent);

        ArgumentCaptor<ArchivedFlight> captor = ArgumentCaptor.forClass(ArchivedFlight.class);
        verify(flightArchiveRepository).save(captor.capture());

        ArchivedFlight saved = captor.getValue();
        assertThat(saved.getEventId()).isEqualTo(sampleEvent.getEventId());
        assertThat(saved.getFlightId()).isEqualTo(100L);
        assertThat(saved.getFlightStatus()).isEqualTo(FlightStatus.ARRIVED);
    }

    @Test
    @DisplayName("archiveFlight - CANCELLED statüsündeki event kaydedilmelidir")
    void archiveFlight_shouldSaveNewArchivedFlight_whenEventStatusIsCancelled() {
        sampleEvent.setFlightStatus(FlightStatus.CANCELLED);
        when(flightArchiveRepository.existsByEventId(sampleEvent.getEventId())).thenReturn(false);
        when(flightArchiveRepository.findByFlightId(100L)).thenReturn(Optional.empty());

        flightArchiveService.archiveFlight(sampleEvent);

        verify(flightArchiveRepository).save(any(ArchivedFlight.class));
    }

    @ParameterizedTest
    @EnumSource(value = FlightStatus.class, names = {"SCHEDULED", "BOARDING", "DEPARTED"})
    @DisplayName("archiveFlight - Final olmayan statüler (SCHEDULED, BOARDING, DEPARTED) kaydedilmemelidir")
    void archiveFlight_shouldNotSave_whenEventStatusIsNotFinal(FlightStatus nonFinalStatus) {
        sampleEvent.setFlightStatus(nonFinalStatus);
        when(flightArchiveRepository.existsByEventId(sampleEvent.getEventId())).thenReturn(false);

        flightArchiveService.archiveFlight(sampleEvent);

        verify(flightArchiveRepository, never()).save(any());
    }

    @Test
    @DisplayName("archiveFlight - Aynı eventId zaten varsa işlem yapılmamalıdır")
    void archiveFlight_shouldNotSave_whenEventIdAlreadyExists() {
        when(flightArchiveRepository.existsByEventId(sampleEvent.getEventId())).thenReturn(true);

        flightArchiveService.archiveFlight(sampleEvent);

        verify(flightArchiveRepository, never()).findByFlightId(any());
        verify(flightArchiveRepository, never()).save(any());
    }

    @Test
    @DisplayName("archiveFlight - Aynı flightId için daha yeni version geldiğinde kayıt güncellenmelidir")
    void archiveFlight_shouldUpdateRecord_whenNewerVersionArrives() {
        ArchivedFlight existingFlight = new ArchivedFlight();
        existingFlight.setArchiveId(1L);
        existingFlight.setFlightId(100L);
        existingFlight.setFlightVersion(1);

        when(flightArchiveRepository.existsByEventId(sampleEvent.getEventId())).thenReturn(false);
        when(flightArchiveRepository.findByFlightId(100L)).thenReturn(Optional.of(existingFlight));

        flightArchiveService.archiveFlight(sampleEvent);

        verify(flightArchiveRepository).save(existingFlight);
        assertThat(existingFlight.getFlightVersion()).isEqualTo(2);
        assertThat(existingFlight.getEventId()).isEqualTo(sampleEvent.getEventId());
    }

    @Test
    @DisplayName("archiveFlight - Aynı veya daha eski version geldiğinde kayıt güncellenmemelidir")
    void archiveFlight_shouldNotUpdateRecord_whenSameOrOlderVersionArrives() {
        ArchivedFlight existingFlight = new ArchivedFlight();
        existingFlight.setArchiveId(1L);
        existingFlight.setFlightId(100L);
        existingFlight.setFlightVersion(2); // Aynı version

        when(flightArchiveRepository.existsByEventId(sampleEvent.getEventId())).thenReturn(false);
        when(flightArchiveRepository.findByFlightId(100L)).thenReturn(Optional.of(existingFlight));

        flightArchiveService.archiveFlight(sampleEvent);

        verify(flightArchiveRepository, never()).save(any());
    }

    @Test
    @DisplayName("archiveFlight - Event alanları ArchivedFlight nesnesine eksiksiz aktarılmalıdır")
    void archiveFlight_shouldMapAllFieldsCorrectly() {
        when(flightArchiveRepository.existsByEventId(sampleEvent.getEventId())).thenReturn(false);
        when(flightArchiveRepository.findByFlightId(100L)).thenReturn(Optional.empty());

        flightArchiveService.archiveFlight(sampleEvent);

        ArgumentCaptor<ArchivedFlight> captor = ArgumentCaptor.forClass(ArchivedFlight.class);
        verify(flightArchiveRepository).save(captor.capture());

        ArchivedFlight saved = captor.getValue();
        assertThat(saved.getEventId()).isEqualTo(sampleEvent.getEventId());
        assertThat(saved.getFlightId()).isEqualTo(sampleEvent.getFlightId());
        assertThat(saved.getFlightNumber()).isEqualTo(sampleEvent.getFlightNumber());
        assertThat(saved.getAirlineId()).isEqualTo(sampleEvent.getAirlineId());
        assertThat(saved.getAircraftId()).isEqualTo(sampleEvent.getAircraftId());
        assertThat(saved.getAircraftTypeId()).isEqualTo(sampleEvent.getAircraftTypeId());
        assertThat(saved.getOriginAirportId()).isEqualTo(sampleEvent.getOriginAirportId());
        assertThat(saved.getDestinationAirportId()).isEqualTo(sampleEvent.getDestinationAirportId());
        assertThat(saved.getFlightTypeId()).isEqualTo(sampleEvent.getFlightTypeId());
        assertThat(saved.getFlightDate()).isEqualTo(sampleEvent.getFlightDate());
        assertThat(saved.getScheduledDepartureTime()).isEqualTo(sampleEvent.getScheduledDepartureTime());
        assertThat(saved.getScheduledArrivalTime()).isEqualTo(sampleEvent.getScheduledArrivalTime());
        assertThat(saved.getFlightStatus()).isEqualTo(sampleEvent.getFlightStatus());
        assertThat(saved.getFlightVersion()).isEqualTo(sampleEvent.getFlightVersion());
        assertThat(saved.getChangedByUserId()).isEqualTo(sampleEvent.getChangedByUserId());
        assertThat(saved.getEventOccurredAt()).isEqualTo(sampleEvent.getOccurredAt());
    }

    @Test
    @DisplayName("getAllArchivedFlights - Tüm arşivleşmiş uçuş listesini dönmelidir")
    void getAllArchivedFlights_shouldReturnMappedList() {
        when(flightArchiveRepository.findAll()).thenReturn(List.of(sampleArchivedFlight));
        when(archiveFlightMapper.toArchivedFlightResponse(sampleArchivedFlight)).thenReturn(sampleResponse);

        List<ArchivedFlightResponse> result = flightArchiveService.getAllArchivedFlights();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getArchiveId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getArchivedFlightByArchiveId - Başarılı archiveId sorgulamasında yanıt dönmelidir")
    void getArchivedFlightByArchiveId_shouldReturnResponse_whenFound() {
        when(flightArchiveRepository.findById(1L)).thenReturn(Optional.of(sampleArchivedFlight));
        when(archiveFlightMapper.toArchivedFlightResponse(sampleArchivedFlight)).thenReturn(sampleResponse);

        ArchivedFlightResponse result = flightArchiveService.getArchivedFlightByArchiveId(1L);

        assertThat(result).isNotNull();
        assertThat(result.getArchiveId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getArchivedFlightByArchiveId - Bulunamadığında 404 NOT_FOUND fırlatmalıdır")
    void getArchivedFlightByArchiveId_shouldThrow404_whenNotFound() {
        when(flightArchiveRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flightArchiveService.getArchivedFlightByArchiveId(99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("getArchivedFlightByFlightId - Başarılı flightId sorgulamasında yanıt dönmelidir")
    void getArchivedFlightByFlightId_shouldReturnResponse_whenFound() {
        when(flightArchiveRepository.findByFlightId(100L)).thenReturn(Optional.of(sampleArchivedFlight));
        when(archiveFlightMapper.toArchivedFlightResponse(sampleArchivedFlight)).thenReturn(sampleResponse);

        ArchivedFlightResponse result = flightArchiveService.getArchivedFlightByFlightId(100L);

        assertThat(result).isNotNull();
        assertThat(result.getFlightId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("getArchivedFlightByFlightId - Bulunamadığında 404 NOT_FOUND fırlatmalıdır")
    void getArchivedFlightByFlightId_shouldThrow404_whenNotFound() {
        when(flightArchiveRepository.findByFlightId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flightArchiveService.getArchivedFlightByFlightId(999L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
