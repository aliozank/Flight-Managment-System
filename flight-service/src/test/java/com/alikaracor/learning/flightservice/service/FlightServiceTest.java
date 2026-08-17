package com.alikaracor.learning.flightservice.service;

import com.alikaracor.learning.flightservice.client.dto.AirportReferenceResponse;
import com.alikaracor.learning.flightservice.dto.FlightCreateRequest;
import com.alikaracor.learning.flightservice.dto.FlightResponse;
import com.alikaracor.learning.flightservice.dto.FlightStatusUpdateRequest;
import com.alikaracor.learning.flightservice.dto.FlightUpdateRequest;
import com.alikaracor.learning.flightservice.event.FlightEventType;
import com.alikaracor.learning.flightservice.mapper.FlightMapper;
import com.alikaracor.learning.flightservice.model.Flight;
import com.alikaracor.learning.flightservice.model.FlightChangeType;
import com.alikaracor.learning.flightservice.model.FlightStatus;
import com.alikaracor.learning.flightservice.model.FlightVersion;
import com.alikaracor.learning.flightservice.publisher.FlightEventPublisher;
import com.alikaracor.learning.flightservice.publisher.FlightWebSocketPublisher;
import com.alikaracor.learning.flightservice.repository.FlightRepository;
import com.alikaracor.learning.flightservice.repository.FlightVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightServiceTest {

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private FlightVersionRepository flightVersionRepository;

    @Mock
    private FlightMapper flightMapper;

    @Mock
    private FlightReferenceValidationService flightReferenceValidationService;

    @Mock
    private AircraftScheduleConflictValidationService aircraftScheduleConflictValidationService;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private FlightEventPublisher flightEventPublisher;

    @Mock
    private FlightWebSocketPublisher flightWebSocketPublisher;

    @InjectMocks
    private FlightService flightService;

    private Flight sampleFlight;
    private FlightCreateRequest createRequest;
    private FlightUpdateRequest updateRequest;
    private FlightResponse sampleResponse;
    private Long actorUserId;
    private String ipAddress;

    @BeforeEach
    void setUp() {
        actorUserId = 100L;
        ipAddress = "192.168.1.1";

        sampleFlight = new Flight();
        sampleFlight.setFlightId(1L);
        sampleFlight.setFlightNumber("TK1234");
        sampleFlight.setAirlineId(10L);
        sampleFlight.setAircraftTypeId(20L);
        sampleFlight.setOriginAirportId(1L);
        sampleFlight.setDestinationAirportId(2L);
        sampleFlight.setFlightTypeId(5L);
        sampleFlight.setFlightDate(LocalDate.of(2026, 10, 1));
        sampleFlight.setScheduledDepartureTime(LocalTime.of(10, 0));
        sampleFlight.setScheduledArrivalTime(LocalTime.of(12, 0));
        sampleFlight.setScheduledArrivalDate(LocalDate.of(2026, 10, 1));
        sampleFlight.setScheduledDepartureAt(Instant.parse("2026-10-01T10:00:00Z"));
        sampleFlight.setScheduledArrivalAt(Instant.parse("2026-10-01T12:00:00Z"));
        sampleFlight.setFlightStatus(FlightStatus.SCHEDULED);
        sampleFlight.setFlightVersion(1);
        sampleFlight.setFlightCreatedAt(Instant.now());
        sampleFlight.setFlightUpdatedAt(Instant.now());

        createRequest = new FlightCreateRequest();
        createRequest.setFlightNumber("TK1234");
        createRequest.setAirlineId(10L);
        createRequest.setAircraftTypeId(20L);
        createRequest.setOriginAirportId(1L);
        createRequest.setDestinationAirportId(2L);
        createRequest.setFlightTypeId(5L);
        createRequest.setFlightDate(LocalDate.of(2026, 10, 1));
        createRequest.setScheduledDepartureTime(LocalTime.of(10, 0));
        createRequest.setScheduledArrivalTime(LocalTime.of(12, 0));
        createRequest.setScheduledArrivalDate(LocalDate.of(2026, 10, 1));

        updateRequest = new FlightUpdateRequest();
        updateRequest.setFlightNumber("TK1234");
        updateRequest.setAirlineId(10L);
        updateRequest.setAircraftTypeId(20L);
        updateRequest.setOriginAirportId(1L);
        updateRequest.setDestinationAirportId(2L);
        updateRequest.setFlightTypeId(5L);
        updateRequest.setFlightDate(LocalDate.of(2026, 10, 1));
        updateRequest.setScheduledDepartureTime(LocalTime.of(11, 0));
        updateRequest.setScheduledArrivalTime(LocalTime.of(13, 0));
        updateRequest.setScheduledArrivalDate(LocalDate.of(2026, 10, 1));

        sampleResponse = new FlightResponse();
        sampleResponse.setFlightId(1L);
        sampleResponse.setFlightNumber("TK1234");

        AirportReferenceResponse dummyAirport = new AirportReferenceResponse();
        dummyAirport.setAirportId(1L);
        dummyAirport.setAirportTimezone("UTC");
        lenient().when(flightReferenceValidationService.validateAirport(any())).thenReturn(dummyAirport);
    }

    // ==================== getAllFlights Tests ====================

    @Test
    @DisplayName("getAllFlights - Uçuşlar mevcut olduğunda tüm uçuş listesini dönmelidir")
    void getAllFlights_shouldReturnListOfFlightResponse_whenFlightsExist() {
        when(flightRepository.findAll()).thenReturn(List.of(sampleFlight));
        when(flightMapper.toFlightResponse(sampleFlight)).thenReturn(sampleResponse);

        List<FlightResponse> result = flightService.getAllFlights();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFlightId()).isEqualTo(1L);
        verify(flightRepository, times(1)).findAll();
        verify(flightMapper, times(1)).toFlightResponse(sampleFlight);
    }

    @Test
    @DisplayName("getAllFlights - Uçuş bulunmadığında boş liste dönmelidir")
    void getAllFlights_shouldReturnEmptyList_whenNoFlightsExist() {
        when(flightRepository.findAll()).thenReturn(Collections.emptyList());

        List<FlightResponse> result = flightService.getAllFlights();

        assertThat(result).isEmpty();
        verify(flightRepository, times(1)).findAll();
        verifyNoInteractions(flightMapper);
    }

    // ==================== getFlightById Tests ====================

    @Test
    @DisplayName("getFlightById - Geçerli flightId ile uçuş bulunduğunda FlightResponse dönmelidir")
    void getFlightById_shouldReturnFlightResponse_whenFlightExists() {
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));
        when(flightMapper.toFlightResponse(sampleFlight)).thenReturn(sampleResponse);

        FlightResponse result = flightService.getFlightById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getFlightId()).isEqualTo(1L);
        verify(flightRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getFlightById - Var olmayan flightId istendiğinde 404 NOT_FOUND fırlatmalıdır")
    void getFlightById_shouldThrowResponseStatusException_whenFlightDoesNotExist() {
        when(flightRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flightService.getFlightById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(flightRepository, times(1)).findById(99L);
        verifyNoInteractions(flightMapper);
    }

    // ==================== addFlight Tests ====================

    @Test
    @DisplayName("addFlight - İstek geçerli olduğunda uçuş oluşturulmalı, versiyon kaydedilmeli, log yazılmalı, event ve WebSocket publish edilmelidir")
    void addFlight_shouldCreateAndReturnFlightResponse_whenRequestIsValid() {
        FlightVersion mockVersion = new FlightVersion();

        when(flightRepository.existsByFlightNumberAndFlightDate(createRequest.getFlightNumber(), createRequest.getFlightDate()))
                .thenReturn(false);
        doNothing().when(flightReferenceValidationService).validateCreateRequest(createRequest);
        doNothing().when(aircraftScheduleConflictValidationService).validateAircraftScheduleForCreate(
                any(),
                any(),
                any()
        );
        when(flightMapper.toFlight(createRequest)).thenReturn(sampleFlight);
        when(flightRepository.save(any(Flight.class))).thenReturn(sampleFlight);
        when(flightMapper.toFlightVersion(sampleFlight)).thenReturn(mockVersion);
        when(flightVersionRepository.save(any(FlightVersion.class))).thenReturn(mockVersion);
        when(flightMapper.toFlightResponse(sampleFlight)).thenReturn(sampleResponse);

        FlightResponse response = flightService.addFlight(createRequest, actorUserId, ipAddress);

        assertThat(response).isNotNull();
        assertThat(response.getFlightId()).isEqualTo(1L);

        verify(flightReferenceValidationService).validateCreateRequest(createRequest);
        verify(aircraftScheduleConflictValidationService).validateAircraftScheduleForCreate(
                any(),
                any(),
                any()
        );
        verify(flightRepository).save(sampleFlight);
        verify(flightVersionRepository).save(mockVersion);
        verify(activityLogService).logFlightCreated(actorUserId, 1L, ipAddress);
        verify(flightEventPublisher).publish(sampleFlight, FlightEventType.CREATED, actorUserId);
        verify(flightWebSocketPublisher).publish(sampleResponse);
    }

    @Test
    @DisplayName("addFlight - Aynı numaralı ve tarihli uçuş zaten varsa 409 CONFLICT fırlatmalı, log tutmalı ve WebSocket publish çağrılmamalıdır")
    void addFlight_shouldThrowConflictExceptionAndLogFailure_whenFlightAlreadyExists() {
        when(flightRepository.existsByFlightNumberAndFlightDate(createRequest.getFlightNumber(), createRequest.getFlightDate()))
                .thenReturn(true);

        assertThatThrownBy(() -> flightService.addFlight(createRequest, actorUserId, ipAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(activityLogService).logFlightCreateFailure(actorUserId, "Flight already exists", ipAddress);
        verifyNoMoreInteractions(flightReferenceValidationService, flightRepository, flightEventPublisher, flightWebSocketPublisher);
    }

    @Test
    @DisplayName("addFlight - Referans doğrulamasında hata alınırsa hata logu tutulup exception tekrar fırlatılmalı ve WebSocket publish çağrılmamalıdır")
    void addFlight_shouldLogFailureAndRethrow_whenValidationThrowsException() {
        when(flightRepository.existsByFlightNumberAndFlightDate(createRequest.getFlightNumber(), createRequest.getFlightDate()))
                .thenReturn(false);
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Airline status is not ACTIVE"))
                .when(flightReferenceValidationService).validateCreateRequest(createRequest);

        assertThatThrownBy(() -> flightService.addFlight(createRequest, actorUserId, ipAddress))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Airline status is not ACTIVE");

        verify(activityLogService).logFlightCreateFailure(actorUserId, "Airline status is not ACTIVE", ipAddress);
        verify(flightRepository, never()).save(any());
        verify(flightEventPublisher, never()).publish(any(), any(), any());
        verify(flightWebSocketPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("addFlight - Aircraft zamanlaması çakışması olduğunda 409 fırlatılmalı, save/event/WebSocket yapılmamalıdır")
    void addFlight_shouldThrowConflictAndNotSave_whenAircraftScheduleConflicts() {
        when(flightRepository.existsByFlightNumberAndFlightDate(createRequest.getFlightNumber(), createRequest.getFlightDate()))
                .thenReturn(false);
        doNothing().when(flightReferenceValidationService).validateCreateRequest(createRequest);
        when(flightMapper.toFlight(createRequest)).thenReturn(sampleFlight);
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Aircraft is occupied during this time window"))
                .when(aircraftScheduleConflictValidationService).validateAircraftScheduleForCreate(
                        any(),
                        any(),
                        any()
                );

        assertThatThrownBy(() -> flightService.addFlight(createRequest, actorUserId, ipAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(activityLogService).logFlightCreateFailure(actorUserId, "Aircraft is occupied during this time window", ipAddress);
        verify(flightRepository, never()).save(any());
        verify(flightEventPublisher, never()).publish(any(), any(), any());
        verify(flightWebSocketPublisher, never()).publish(any());
    }

    // ==================== updateFlight Tests ====================

    @Test
    @DisplayName("updateFlight - İstek geçerli olduğunda uçuş güncellenmeli, versiyon artırılmalı, log, event ve WebSocket publish tetiklenmelidir")
    void updateFlight_shouldUpdateAndReturnFlightResponse_whenRequestIsValid() {
        FlightVersion mockVersion = new FlightVersion();

        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));
        when(flightRepository.existsByFlightNumberAndFlightDateAndFlightIdNot("TK1234", LocalDate.of(2026, 10, 1), 1L))
                .thenReturn(false);
        doNothing().when(flightReferenceValidationService).validateUpdateRequest(updateRequest);
        doNothing().when(aircraftScheduleConflictValidationService).validateAircraftScheduleForUpdate(
                any(),
                any(),
                any(),
                any()
        );
        doNothing().when(flightMapper).updateFlight(updateRequest, sampleFlight);
        when(flightRepository.save(sampleFlight)).thenReturn(sampleFlight);
        when(flightMapper.toFlightVersion(sampleFlight)).thenReturn(mockVersion);
        when(flightVersionRepository.save(mockVersion)).thenReturn(mockVersion);
        when(flightMapper.toFlightResponse(sampleFlight)).thenReturn(sampleResponse);

        FlightResponse response = flightService.updateFlight(1L, updateRequest, actorUserId, ipAddress);

        assertThat(response).isNotNull();
        assertThat(sampleFlight.getFlightVersion()).isEqualTo(2);

        verify(flightReferenceValidationService).validateUpdateRequest(updateRequest);
        verify(aircraftScheduleConflictValidationService).validateAircraftScheduleForUpdate(
                any(),
                any(),
                any(),
                any()
        );
        verify(flightRepository).save(sampleFlight);
        verify(flightVersionRepository).save(mockVersion);
        verify(activityLogService).logFlightUpdated(actorUserId, 1L, ipAddress);
        verify(flightEventPublisher).publish(sampleFlight, FlightEventType.UPDATED, actorUserId);
        verify(flightWebSocketPublisher).publish(sampleResponse);
    }

    @Test
    @DisplayName("updateFlight - Var olmayan uçuş ID'si verildiğinde 404 NOT_FOUND üretmeli ve WebSocket publish çağrılmamalıdır")
    void updateFlight_shouldThrowNotFoundExceptionAndLogFailure_whenFlightDoesNotExist() {
        when(flightRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flightService.updateFlight(99L, updateRequest, actorUserId, ipAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(activityLogService).logFlightUpdateFailure(actorUserId, 99L, "Bu id ile uçuş mevcut değil", ipAddress);
        verify(flightRepository, never()).save(any());
        verify(flightWebSocketPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("updateFlight - Başka bir uçuş aynı numara ve tarihe sahipse 409 CONFLICT fırlatmalı ve WebSocket publish çağrılmamalıdır")
    void updateFlight_shouldThrowConflictExceptionAndLogFailure_whenAnotherFlightWithSameNumberAndDateExists() {
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));
        when(flightRepository.existsByFlightNumberAndFlightDateAndFlightIdNot("TK1234", LocalDate.of(2026, 10, 1), 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> flightService.updateFlight(1L, updateRequest, actorUserId, ipAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(activityLogService).logFlightUpdateFailure(actorUserId, 1L, "Flight already exists", ipAddress);
        verify(flightReferenceValidationService, never()).validateUpdateRequest(any());
        verify(flightWebSocketPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("updateFlight - Güncelleme esnasında exception oluşursa hata logu tutup rethrow etmeli ve WebSocket publish çağrılmamalıdır")
    void updateFlight_shouldLogFailureAndRethrow_whenValidationThrowsException() {
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));
        when(flightRepository.existsByFlightNumberAndFlightDateAndFlightIdNot("TK1234", LocalDate.of(2026, 10, 1), 1L))
                .thenReturn(false);
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Route status is not ACTIVE"))
                .when(flightReferenceValidationService).validateUpdateRequest(updateRequest);

        assertThatThrownBy(() -> flightService.updateFlight(1L, updateRequest, actorUserId, ipAddress))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Route status is not ACTIVE");

        verify(activityLogService).logFlightUpdateFailure(actorUserId, 1L, "Route status is not ACTIVE", ipAddress);
        verify(flightRepository, never()).save(any());
        verify(flightWebSocketPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("updateFlight - Aircraft zamanlaması çakışması olduğunda 409 fırlatılmalı, save/event/WebSocket yapılmamalıdır")
    void updateFlight_shouldThrowConflictAndNotSave_whenAircraftScheduleConflicts() {
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));
        when(flightRepository.existsByFlightNumberAndFlightDateAndFlightIdNot("TK1234", LocalDate.of(2026, 10, 1), 1L))
                .thenReturn(false);
        doNothing().when(flightReferenceValidationService).validateUpdateRequest(updateRequest);
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Aircraft is occupied during this time window"))
                .when(aircraftScheduleConflictValidationService).validateAircraftScheduleForUpdate(
                        any(),
                        any(),
                        any(),
                        any()
                );

        assertThatThrownBy(() -> flightService.updateFlight(1L, updateRequest, actorUserId, ipAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(activityLogService).logFlightUpdateFailure(actorUserId, 1L, "Aircraft is occupied during this time window", ipAddress);
        verify(flightRepository, never()).save(any());
        verify(flightEventPublisher, never()).publish(any(), any(), any());
        verify(flightWebSocketPublisher, never()).publish(any());
    }

    // ==================== cancelFlight Tests ====================

    @Test
    @DisplayName("cancelFlight - SCHEDULED uçuş başarıyla CANCELLED durumuna geçmeli, versiyon artmalı, log, event ve WebSocket publish tetiklenmelidir")
    void cancelFlight_shouldCancelFlight_whenFlightIsScheduled() {
        FlightVersion mockVersion = new FlightVersion();

        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));
        when(flightRepository.save(sampleFlight)).thenReturn(sampleFlight);
        when(flightMapper.toFlightVersion(sampleFlight)).thenReturn(mockVersion);
        when(flightVersionRepository.save(mockVersion)).thenReturn(mockVersion);
        when(flightMapper.toFlightResponse(sampleFlight)).thenReturn(sampleResponse);

        flightService.cancelFlight(1L, actorUserId, ipAddress);

        assertThat(sampleFlight.getFlightStatus()).isEqualTo(FlightStatus.CANCELLED);
        assertThat(sampleFlight.getFlightVersion()).isEqualTo(2);

        verify(flightRepository).save(sampleFlight);
        verify(flightVersionRepository).save(mockVersion);
        verify(activityLogService).logFlightCancel(actorUserId, 1L, ipAddress);
        verify(flightEventPublisher).publish(sampleFlight, FlightEventType.CANCELLED, actorUserId);
        verify(flightWebSocketPublisher).publish(sampleResponse);
    }

    @Test
    @DisplayName("cancelFlight - Bulunamayan uçuş ID'sinde 404 NOT_FOUND üretilmeli ve WebSocket publish çağrılmamalıdır")
    void cancelFlight_shouldThrowNotFoundExceptionAndLogFailure_whenFlightDoesNotExist() {
        when(flightRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flightService.cancelFlight(99L, actorUserId, ipAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(activityLogService).logFlightCancelFailure(actorUserId, 99L, "Bu id ile uçuş mevcut değil", ipAddress);
        verify(flightWebSocketPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("cancelFlight - Zaten CANCELLED olan uçuş için 409 CONFLICT fırlatmalı ve WebSocket publish çağrılmamalıdır")
    void cancelFlight_shouldThrowBadRequestExceptionAndLogFailure_whenFlightIsAlreadyCancelled() {
        sampleFlight.setFlightStatus(FlightStatus.CANCELLED);
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));

        assertThatThrownBy(() -> flightService.cancelFlight(1L, actorUserId, ipAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(activityLogService).logFlightCancelFailure(actorUserId, 1L, "Bu uçuş zaten iptal edilmiş", ipAddress);
        verify(flightRepository, never()).save(any());
        verify(flightWebSocketPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("cancelFlight - ARRIVED olan uçuş için 409 CONFLICT fırlatmalı ve WebSocket publish çağrılmamalıdır")
    void cancelFlight_shouldThrowBadRequestExceptionAndLogFailure_whenFlightIsArrived() {
        sampleFlight.setFlightStatus(FlightStatus.ARRIVED);
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));

        assertThatThrownBy(() -> flightService.cancelFlight(1L, actorUserId, ipAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(activityLogService).logFlightCancelFailure(actorUserId, 1L, "Bu uçuş zaten gerçeklmiş neyi iptal ediyon", ipAddress);
        verify(flightRepository, never()).save(any());
        verify(flightWebSocketPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("cancelFlight - DEPARTED olan uçuş için 409 CONFLICT fırlatmalı ve WebSocket publish çağrılmamalıdır")
    void cancelFlight_shouldThrowConflict_whenFlightIsDeparted() {
        sampleFlight.setFlightStatus(FlightStatus.DEPARTED);
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));

        assertThatThrownBy(() -> flightService.cancelFlight(1L, actorUserId, ipAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(flightRepository, never()).save(any());
        verify(flightWebSocketPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("cancelFlight - İptal işleminde veritabanı hatası fırlatıldığında WebSocket publish çağrılmamalıdır")
    void cancelFlight_shouldLogFailureAndRethrow_whenRepositorySaveFails() {
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));
        doThrow(new RuntimeException("Database error")).when(flightRepository).save(sampleFlight);

        assertThatThrownBy(() -> flightService.cancelFlight(1L, actorUserId, ipAddress))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");

        verify(activityLogService).logFlightCancelFailure(actorUserId, 1L, "Flight cancellation failed", ipAddress);
        verify(flightWebSocketPublisher, never()).publish(any());
    }

    // ==================== updateFlightStatus Tests ====================

    @Test
    @DisplayName("updateFlightStatus - Uçuş bulunamadığında 404 NOT_FOUND fırlatmalı ve failure log yazmalıdır")
    void updateFlightStatus_shouldThrowNotFoundException_whenFlightDoesNotExist() {
        FlightStatusUpdateRequest request = new FlightStatusUpdateRequest();
        request.setFlightStatus(FlightStatus.DELAYED);
        when(flightRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flightService.updateFlightStatus(99L, request, actorUserId, ipAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(activityLogService).logFlightUpdateFailure(actorUserId, 99L, "Flight not found", ipAddress);
        verify(flightRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateFlightStatus - Hedef durum ile mevcut durum aynı ise veritabanına kaydetmeden mevcut yanıtı dönmelidir")
    void updateFlightStatus_shouldReturnResponseWithoutSaving_whenTargetStatusEqualsOldStatus() {
        sampleFlight.setFlightStatus(FlightStatus.SCHEDULED);
        FlightStatusUpdateRequest request = new FlightStatusUpdateRequest();
        request.setFlightStatus(FlightStatus.SCHEDULED);
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));
        when(flightMapper.toFlightResponse(sampleFlight)).thenReturn(sampleResponse);

        FlightResponse response = flightService.updateFlightStatus(1L, request, actorUserId, ipAddress);

        assertThat(response).isEqualTo(sampleResponse);
        verify(flightRepository, never()).save(any());
        verify(flightWebSocketPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("updateFlightStatus - Hedef durum CANCELLED ise 409 CONFLICT fırlatmalıdır (iptal endpoint'i kullanılmalıdır)")
    void updateFlightStatus_shouldThrowConflict_whenTargetStatusIsCancelled() {
        sampleFlight.setFlightStatus(FlightStatus.SCHEDULED);
        FlightStatusUpdateRequest request = new FlightStatusUpdateRequest();
        request.setFlightStatus(FlightStatus.CANCELLED);
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));

        assertThatThrownBy(() -> flightService.updateFlightStatus(1L, request, actorUserId, ipAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(flightRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateFlightStatus - Geçersiz durum geçişinde (örn. SCHEDULED -> ARRIVED) 409 CONFLICT fırlatmalıdır")
    void updateFlightStatus_shouldThrowConflictException_whenStatusTransitionNotAllowed() {
        sampleFlight.setFlightStatus(FlightStatus.SCHEDULED);
        FlightStatusUpdateRequest request = new FlightStatusUpdateRequest();
        request.setFlightStatus(FlightStatus.ARRIVED);
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));

        assertThatThrownBy(() -> flightService.updateFlightStatus(1L, request, actorUserId, ipAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(activityLogService).logFlightUpdateFailure(actorUserId, 1L, "Flight status cannot transition from SCHEDULED to ARRIVED", ipAddress);
        verify(flightRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateFlightStatus - Geçerli durum geçişinde (örn. SCHEDULED -> DELAYED) kaydetmeli ve eventleri yayınlamalıdır")
    void updateFlightStatus_shouldUpdateStatusAndPublishEvents_whenTransitionIsValid() {
        sampleFlight.setFlightStatus(FlightStatus.SCHEDULED);
        FlightStatusUpdateRequest request = new FlightStatusUpdateRequest();
        request.setFlightStatus(FlightStatus.DELAYED);
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));
        when(flightMapper.toFlightVersion(sampleFlight)).thenReturn(new FlightVersion());
        when(flightMapper.toFlightResponse(sampleFlight)).thenReturn(sampleResponse);

        FlightResponse response = flightService.updateFlightStatus(1L, request, actorUserId, ipAddress);

        assertThat(response).isEqualTo(sampleResponse);
        assertThat(sampleFlight.getFlightStatus()).isEqualTo(FlightStatus.DELAYED);
        verify(flightRepository).save(sampleFlight);
        verify(flightVersionRepository).save(any(FlightVersion.class));
        verify(activityLogService).logFlightUpdated(actorUserId, 1L, ipAddress);
        verify(flightEventPublisher).publish(sampleFlight, FlightEventType.UPDATED, actorUserId);
        verify(flightWebSocketPublisher).publish(sampleResponse);
    }

    @Test
    @DisplayName("updateFlightStatus - SCHEDULED -> DEPARTED geçerli geçiş")
    void updateFlightStatus_shouldUpdateStatus_whenScheduledToDeparted() {
        sampleFlight.setFlightStatus(FlightStatus.SCHEDULED);
        FlightStatusUpdateRequest request = new FlightStatusUpdateRequest();
        request.setFlightStatus(FlightStatus.DEPARTED);
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));
        when(flightMapper.toFlightVersion(sampleFlight)).thenReturn(new FlightVersion());
        when(flightMapper.toFlightResponse(sampleFlight)).thenReturn(sampleResponse);

        FlightResponse response = flightService.updateFlightStatus(1L, request, actorUserId, ipAddress);

        assertThat(response).isEqualTo(sampleResponse);
        assertThat(sampleFlight.getFlightStatus()).isEqualTo(FlightStatus.DEPARTED);
        verify(flightRepository).save(sampleFlight);
    }

    @Test
    @DisplayName("updateFlightStatus - DELAYED -> DEPARTED geçerli geçiş")
    void updateFlightStatus_shouldUpdateStatus_whenDelayedToDeparted() {
        sampleFlight.setFlightStatus(FlightStatus.DELAYED);
        FlightStatusUpdateRequest request = new FlightStatusUpdateRequest();
        request.setFlightStatus(FlightStatus.DEPARTED);
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));
        when(flightMapper.toFlightVersion(sampleFlight)).thenReturn(new FlightVersion());
        when(flightMapper.toFlightResponse(sampleFlight)).thenReturn(sampleResponse);

        FlightResponse response = flightService.updateFlightStatus(1L, request, actorUserId, ipAddress);

        assertThat(response).isEqualTo(sampleResponse);
        assertThat(sampleFlight.getFlightStatus()).isEqualTo(FlightStatus.DEPARTED);
        verify(flightRepository).save(sampleFlight);
    }

    @Test
    @DisplayName("updateFlightStatus - DEPARTED -> ARRIVED geçerli geçiş")
    void updateFlightStatus_shouldUpdateStatus_whenDepartedToArrived() {
        sampleFlight.setFlightStatus(FlightStatus.DEPARTED);
        FlightStatusUpdateRequest request = new FlightStatusUpdateRequest();
        request.setFlightStatus(FlightStatus.ARRIVED);
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));
        when(flightMapper.toFlightVersion(sampleFlight)).thenReturn(new FlightVersion());
        when(flightMapper.toFlightResponse(sampleFlight)).thenReturn(sampleResponse);

        FlightResponse response = flightService.updateFlightStatus(1L, request, actorUserId, ipAddress);

        assertThat(response).isEqualTo(sampleResponse);
        assertThat(sampleFlight.getFlightStatus()).isEqualTo(FlightStatus.ARRIVED);
        verify(flightRepository).save(sampleFlight);
    }

    @Test
    @DisplayName("updateFlightStatus - ARRIVED -> SCHEDULED geçersiz geçiş")
    void updateFlightStatus_shouldThrowConflict_whenArrivedToScheduled() {
        sampleFlight.setFlightStatus(FlightStatus.ARRIVED);
        FlightStatusUpdateRequest request = new FlightStatusUpdateRequest();
        request.setFlightStatus(FlightStatus.SCHEDULED);
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));

        assertThatThrownBy(() -> flightService.updateFlightStatus(1L, request, actorUserId, ipAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("updateFlightStatus - CANCELLED -> SCHEDULED geçersiz geçiş")
    void updateFlightStatus_shouldThrowConflict_whenCancelledToScheduled() {
        sampleFlight.setFlightStatus(FlightStatus.CANCELLED);
        FlightStatusUpdateRequest request = new FlightStatusUpdateRequest();
        request.setFlightStatus(FlightStatus.SCHEDULED);
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));

        assertThatThrownBy(() -> flightService.updateFlightStatus(1L, request, actorUserId, ipAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("updateFlightStatus - ARRIVED -> CANCELLED geçersiz geçiş")
    void updateFlightStatus_shouldThrowConflict_whenArrivedToCancelled() {
        sampleFlight.setFlightStatus(FlightStatus.ARRIVED);
        FlightStatusUpdateRequest request = new FlightStatusUpdateRequest();
        request.setFlightStatus(FlightStatus.CANCELLED);
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));

        assertThatThrownBy(() -> flightService.updateFlightStatus(1L, request, actorUserId, ipAddress))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }
}
