package com.alikaracor.learning.referencemanager.service;

import com.alikaracor.learning.referencemanager.dto.AircraftRequest;
import com.alikaracor.learning.referencemanager.dto.AircraftResponse;
import com.alikaracor.learning.referencemanager.event.ReferenceEvent;
import com.alikaracor.learning.referencemanager.event.ReferenceEventType;
import com.alikaracor.learning.referencemanager.event.ReferenceResourceType;
import com.alikaracor.learning.referencemanager.mapper.AircraftMapper;
import com.alikaracor.learning.referencemanager.model.Aircraft;
import com.alikaracor.learning.referencemanager.model.AircraftStatus;
import com.alikaracor.learning.referencemanager.model.AircraftType;
import com.alikaracor.learning.referencemanager.model.Airline;
import com.alikaracor.learning.referencemanager.publisher.ReferenceEventPublisher;
import com.alikaracor.learning.referencemanager.repository.AircraftRepository;
import com.alikaracor.learning.referencemanager.repository.AircraftTypeRepository;
import com.alikaracor.learning.referencemanager.repository.AirlineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Year;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AircraftServiceTest {

    @Mock
    private AircraftRepository aircraftRepository;

    @Mock
    private AircraftMapper aircraftMapper;

    @Mock
    private AircraftTypeRepository aircraftTypeRepository;

    @Mock
    private AirlineRepository airlineRepository;

    @Mock
    private ReferenceEventPublisher publisher;

    @InjectMocks
    private AircraftService aircraftService;

    @Test
    void shouldAddAircraftWithOperatorAirlineAndPublishCreatedEvent() {
        AircraftRequest request = validRequest();
        AircraftType aircraftType = aircraftType(10L);
        Airline airline = airline(20L);
        Aircraft mapped = new Aircraft();
        Aircraft saved = aircraft(1L, AircraftStatus.ACTIVE);
        AircraftResponse expected = response(1L);
        when(aircraftTypeRepository.findById(10L)).thenReturn(Optional.of(aircraftType));
        when(airlineRepository.findById(20L)).thenReturn(Optional.of(airline));
        when(aircraftMapper.toAircraft(request)).thenReturn(mapped);
        when(aircraftRepository.save(mapped)).thenReturn(saved);
        when(aircraftMapper.toAircraftResponse(saved)).thenReturn(expected);

        AircraftResponse actual = aircraftService.addAircraft(request);

        assertSame(expected, actual);
        assertSame(aircraftType, mapped.getAircraftType());
        assertSame(airline, mapped.getOperatorAirline());
        assertEvent(ReferenceEventType.CREATED, 1L);
    }

    @Test
    void shouldAllowAircraftWithoutOperatorAirline() {
        AircraftRequest request = validRequest();
        request.setOperatorAirlineId(null);
        AircraftType aircraftType = aircraftType(10L);
        Aircraft mapped = new Aircraft();
        Aircraft saved = aircraft(2L, AircraftStatus.ACTIVE);
        AircraftResponse expected = response(2L);
        when(aircraftTypeRepository.findById(10L)).thenReturn(Optional.of(aircraftType));
        when(aircraftMapper.toAircraft(request)).thenReturn(mapped);
        when(aircraftRepository.save(mapped)).thenReturn(saved);
        when(aircraftMapper.toAircraftResponse(saved)).thenReturn(expected);

        AircraftResponse actual = aircraftService.addAircraft(request);

        assertSame(expected, actual);
        assertNull(mapped.getOperatorAirline());
        verify(airlineRepository, never()).findById(any());
    }

    @Test
    void shouldRejectDuplicateRegistrationNumber() {
        AircraftRequest request = validRequest();
        when(aircraftRepository.existsByAircraftRegistrationNumberIgnoreCase(
                request.getAircraftRegistrationNumber()
        )).thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () -> aircraftService.addAircraft(request));

        verify(aircraftRepository, never()).save(any(Aircraft.class));
        verify(publisher, never()).publish(any(ReferenceEvent.class));
    }

    @Test
    void shouldRejectMissingAircraftType() {
        AircraftRequest request = validRequest();
        when(aircraftTypeRepository.findById(10L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () -> aircraftService.addAircraft(request));
    }

    @Test
    void shouldRejectMissingOperatorAirline() {
        AircraftRequest request = validRequest();
        when(aircraftTypeRepository.findById(10L)).thenReturn(Optional.of(aircraftType(10L)));
        when(airlineRepository.findById(20L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () -> aircraftService.addAircraft(request));
    }

    @Test
    void shouldRejectFutureManufactureYear() {
        AircraftRequest request = validRequest();
        request.setAircraftManufactureYear(Year.now().getValue() + 1);
        when(aircraftTypeRepository.findById(10L)).thenReturn(Optional.of(aircraftType(10L)));
        when(airlineRepository.findById(20L)).thenReturn(Optional.of(airline(20L)));

        assertStatus(HttpStatus.BAD_REQUEST, () -> aircraftService.addAircraft(request));
    }

    @Test
    void shouldReturnAircraftById() {
        Aircraft aircraft = aircraft(3L, AircraftStatus.ACTIVE);
        AircraftResponse expected = response(3L);
        when(aircraftRepository.findById(3L)).thenReturn(Optional.of(aircraft));
        when(aircraftMapper.toAircraftResponse(aircraft)).thenReturn(expected);

        assertSame(expected, aircraftService.getAircraftById(3L));
    }

    @Test
    void shouldReturnAllAircraft() {
        Aircraft first = aircraft(1L, AircraftStatus.ACTIVE);
        Aircraft second = aircraft(2L, AircraftStatus.MAINTENANCE);
        AircraftResponse firstResponse = response(1L);
        AircraftResponse secondResponse = response(2L);
        when(aircraftRepository.findAll()).thenReturn(List.of(first, second));
        when(aircraftMapper.toAircraftResponse(first)).thenReturn(firstResponse);
        when(aircraftMapper.toAircraftResponse(second)).thenReturn(secondResponse);

        assertEquals(List.of(firstResponse, secondResponse), aircraftService.getAllAircrafts());
    }

    @Test
    void shouldUpdateAircraftAndPublishUpdatedEvent() {
        AircraftRequest request = validRequest();
        request.setAircraftCapacity(210);
        Aircraft existing = aircraft(4L, AircraftStatus.ACTIVE);
        AircraftType aircraftType = aircraftType(10L);
        Airline airline = airline(20L);
        AircraftResponse expected = response(4L);
        when(aircraftRepository.findById(4L)).thenReturn(Optional.of(existing));
        when(aircraftTypeRepository.findById(10L)).thenReturn(Optional.of(aircraftType));
        when(airlineRepository.findById(20L)).thenReturn(Optional.of(airline));
        when(aircraftRepository.saveAndFlush(existing)).thenReturn(existing);
        when(aircraftMapper.toAircraftResponse(existing)).thenReturn(expected);

        AircraftResponse actual = aircraftService.updateAircraftById(4L, request);

        assertSame(expected, actual);
        assertEquals(210, existing.getAircraftCapacity());
        assertSame(aircraftType, existing.getAircraftType());
        assertSame(airline, existing.getOperatorAirline());
        verify(aircraftRepository).saveAndFlush(existing);
        assertEvent(ReferenceEventType.UPDATED, 4L);
    }

    @Test
    void shouldRejectDuplicateRegistrationDuringUpdate() {
        AircraftRequest request = validRequest();
        Aircraft existing = aircraft(5L, AircraftStatus.ACTIVE);
        when(aircraftRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(aircraftRepository.existsByAircraftRegistrationNumberIgnoreCaseAndAircraftIdNot(
                request.getAircraftRegistrationNumber(),
                5L
        )).thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () -> aircraftService.updateAircraftById(5L, request));
    }

    @Test
    void shouldDeactivateAircraftAndPublishEvent() {
        Aircraft aircraft = aircraft(6L, AircraftStatus.ACTIVE);
        when(aircraftRepository.findById(6L)).thenReturn(Optional.of(aircraft));

        aircraftService.deactiveAircraftById(6L);

        assertEquals(AircraftStatus.RETIRED, aircraft.getAircraftStatus());
        verify(aircraftRepository).save(aircraft);
        assertEvent(ReferenceEventType.DEACTIVATED, 6L);
    }

    @Test
    void shouldDoNothingWhenAircraftIsAlreadyRetired() {
        Aircraft aircraft = aircraft(7L, AircraftStatus.RETIRED);
        when(aircraftRepository.findById(7L)).thenReturn(Optional.of(aircraft));

        aircraftService.deactiveAircraftById(7L);

        verify(aircraftRepository, never()).save(any(Aircraft.class));
        verify(publisher, never()).publish(any(ReferenceEvent.class));
    }

    private AircraftRequest validRequest() {
        AircraftRequest request = new AircraftRequest();
        request.setAircraftRegistrationNumber("TC-JAA");
        request.setOperatorAirlineId(20L);
        request.setAircraftTypeId(10L);
        request.setAircraftCapacity(180);
        request.setAircraftManufactureYear(2020);
        request.setAircraftStatus(AircraftStatus.ACTIVE);
        return request;
    }

    private Aircraft aircraft(Long id, AircraftStatus status) {
        Aircraft aircraft = new Aircraft();
        aircraft.setAircraftId(id);
        aircraft.setAircraftRegistrationNumber("TC-JAA");
        aircraft.setAircraftCapacity(180);
        aircraft.setAircraftManufactureYear(2020);
        aircraft.setAircraftStatus(status);
        return aircraft;
    }

    private AircraftType aircraftType(Long id) {
        AircraftType aircraftType = new AircraftType();
        aircraftType.setAircraftTypeId(id);
        return aircraftType;
    }

    private Airline airline(Long id) {
        Airline airline = new Airline();
        airline.setAirlineId(id);
        return airline;
    }

    private AircraftResponse response(Long id) {
        AircraftResponse response = new AircraftResponse();
        response.setAircraftId(id);
        return response;
    }

    private void assertEvent(ReferenceEventType type, Long id) {
        ArgumentCaptor<ReferenceEvent> captor = ArgumentCaptor.forClass(ReferenceEvent.class);
        verify(publisher).publish(captor.capture());
        ReferenceEvent event = captor.getValue();
        assertNotNull(event.getEventId());
        assertNotNull(event.getOccurredAt());
        assertEquals(type, event.getEventType());
        assertEquals(ReferenceResourceType.AIRCRAFT, event.getResourceType());
        assertEquals(id, event.getResourceId());
    }

    private void assertStatus(HttpStatus status, Runnable action) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(status, exception.getStatusCode());
    }
}
