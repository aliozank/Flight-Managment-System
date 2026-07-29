package com.alikaracor.learning.referencemanager.service;

import com.alikaracor.learning.referencemanager.dto.AirlineRequest;
import com.alikaracor.learning.referencemanager.dto.AirlineResponse;
import com.alikaracor.learning.referencemanager.event.ReferenceEvent;
import com.alikaracor.learning.referencemanager.event.ReferenceEventType;
import com.alikaracor.learning.referencemanager.event.ReferenceResourceType;
import com.alikaracor.learning.referencemanager.mapper.AirlineMapper;
import com.alikaracor.learning.referencemanager.model.Airline;
import com.alikaracor.learning.referencemanager.model.AirlineStatus;
import com.alikaracor.learning.referencemanager.publisher.ReferenceEventPublisher;
import com.alikaracor.learning.referencemanager.repository.AirlineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AirlineServiceTest {

    @Mock
    private AirlineRepository airlineRepository;

    @Mock
    private AirlineMapper airlineMapper;

    @Mock
    private ReferenceEventPublisher publisher;

    @InjectMocks
    private AirlineService airlineService;

    @Test
    void shouldAddAirlineAndPublishCreatedEvent() {
        AirlineRequest request = validRequest();
        request.setAirlineName("  Turkish Airlines  ");
        request.setAirlineCountry("  Türkiye  ");

        Airline mappedAirline = new Airline();
        Airline savedAirline = airline(1L, AirlineStatus.ACTIVE);
        AirlineResponse expectedResponse = response(1L, AirlineStatus.ACTIVE);

        when(airlineMapper.toAirline(request)).thenReturn(mappedAirline);
        when(airlineRepository.save(mappedAirline)).thenReturn(savedAirline);
        when(airlineMapper.toAirlineResponse(savedAirline)).thenReturn(expectedResponse);

        AirlineResponse actualResponse = airlineService.addAirline(request);

        assertSame(expectedResponse, actualResponse);
        assertEquals("Turkish Airlines", mappedAirline.getAirlineName());
        assertEquals("Türkiye", mappedAirline.getAirlineCountry());
        verify(airlineRepository).save(mappedAirline);
        assertEvent(ReferenceEventType.CREATED, 1L);
    }

    @Test
    void shouldThrowConflictWhenIcaoCodeAlreadyExists() {
        AirlineRequest request = validRequest();
        when(airlineRepository.existsByAirlineIcaoCodeIgnoreCase(request.getAirlineIcaoCode()))
                .thenReturn(true);

        assertStatus(
                HttpStatus.CONFLICT,
                () -> airlineService.addAirline(request)
        );

        verify(airlineRepository, never()).save(any(Airline.class));
        verify(publisher, never()).publish(any(ReferenceEvent.class));
    }

    @Test
    void shouldThrowConflictWhenIataCodeAlreadyExists() {
        AirlineRequest request = validRequest();
        when(airlineRepository.existsByAirlineIataCodeIgnoreCase(request.getAirlineIataCode()))
                .thenReturn(true);

        assertStatus(
                HttpStatus.CONFLICT,
                () -> airlineService.addAirline(request)
        );

        verify(airlineRepository, never()).save(any(Airline.class));
        verify(publisher, never()).publish(any(ReferenceEvent.class));
    }

    @Test
    void shouldThrowConflictWhenAirlineNameAlreadyExists() {
        AirlineRequest request = validRequest();
        when(airlineRepository.existsByAirlineNameIgnoreCase(request.getAirlineName()))
                .thenReturn(true);

        assertStatus(
                HttpStatus.CONFLICT,
                () -> airlineService.addAirline(request)
        );

        verify(airlineRepository, never()).save(any(Airline.class));
        verify(publisher, never()).publish(any(ReferenceEvent.class));
    }

    @Test
    void shouldReturnAirlineById() {
        Airline airline = airline(5L, AirlineStatus.ACTIVE);
        AirlineResponse expectedResponse = response(5L, AirlineStatus.ACTIVE);
        when(airlineRepository.findById(5L)).thenReturn(Optional.of(airline));
        when(airlineMapper.toAirlineResponse(airline)).thenReturn(expectedResponse);

        AirlineResponse actualResponse = airlineService.getAirlineById(5L);

        assertSame(expectedResponse, actualResponse);
    }

    @Test
    void shouldThrowNotFoundWhenAirlineIdDoesNotExist() {
        when(airlineRepository.findById(99L)).thenReturn(Optional.empty());

        assertStatus(
                HttpStatus.NOT_FOUND,
                () -> airlineService.getAirlineById(99L)
        );
    }

    @Test
    void shouldReturnAllAirlines() {
        Airline first = airline(1L, AirlineStatus.ACTIVE);
        Airline second = airline(2L, AirlineStatus.SUSPENDED);
        AirlineResponse firstResponse = response(1L, AirlineStatus.ACTIVE);
        AirlineResponse secondResponse = response(2L, AirlineStatus.SUSPENDED);
        when(airlineRepository.findAll()).thenReturn(List.of(first, second));
        when(airlineMapper.toAirlineResponse(first)).thenReturn(firstResponse);
        when(airlineMapper.toAirlineResponse(second)).thenReturn(secondResponse);

        List<AirlineResponse> responses = airlineService.getAllAirlines();

        assertEquals(List.of(firstResponse, secondResponse), responses);
    }

    @Test
    void shouldUpdateAirlineAndPublishUpdatedEvent() {
        AirlineRequest request = validRequest();
        request.setAirlineName("  Updated Airline  ");
        request.setAirlineCountry("  Germany  ");
        Airline existingAirline = airline(7L, AirlineStatus.ACTIVE);
        AirlineResponse expectedResponse = response(7L, AirlineStatus.SUSPENDED);
        when(airlineRepository.findById(7L)).thenReturn(Optional.of(existingAirline));
        when(airlineRepository.saveAndFlush(existingAirline)).thenReturn(existingAirline);
        when(airlineMapper.toAirlineResponse(existingAirline)).thenReturn(expectedResponse);

        AirlineResponse actualResponse = airlineService.updateAirline(7L, request);

        assertSame(expectedResponse, actualResponse);
        assertEquals("Updated Airline", existingAirline.getAirlineName());
        assertEquals("Germany", existingAirline.getAirlineCountry());
        assertEquals(request.getAirlineIataCode(), existingAirline.getAirlineIataCode());
        assertEquals(request.getAirlineIcaoCode(), existingAirline.getAirlineIcaoCode());
        assertEquals(request.getAirlineStatus(), existingAirline.getAirlineStatus());
        verify(airlineRepository).saveAndFlush(existingAirline);
        assertEvent(ReferenceEventType.UPDATED, 7L);
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingMissingAirline() {
        when(airlineRepository.findById(77L)).thenReturn(Optional.empty());

        assertStatus(
                HttpStatus.NOT_FOUND,
                () -> airlineService.updateAirline(77L, validRequest())
        );

        verify(airlineRepository, never()).saveAndFlush(any(Airline.class));
        verify(publisher, never()).publish(any(ReferenceEvent.class));
    }

    @Test
    void shouldThrowConflictWhenUpdatingWithAnotherAirlinesIataCode() {
        Airline existingAirline = airline(8L, AirlineStatus.ACTIVE);
        AirlineRequest request = validRequest();
        when(airlineRepository.findById(8L)).thenReturn(Optional.of(existingAirline));
        when(airlineRepository.existsByAirlineIataCodeIgnoreCaseAndAirlineIdNot(
                request.getAirlineIataCode(),
                8L
        )).thenReturn(true);

        assertStatus(
                HttpStatus.CONFLICT,
                () -> airlineService.updateAirline(8L, request)
        );

        verify(airlineRepository, never()).saveAndFlush(any(Airline.class));
        verify(publisher, never()).publish(any(ReferenceEvent.class));
    }

    @Test
    void shouldDeactivateAirlineAndPublishDeactivatedEvent() {
        Airline airline = airline(9L, AirlineStatus.ACTIVE);
        when(airlineRepository.findById(9L)).thenReturn(Optional.of(airline));

        airlineService.deactivateAirline(9L);

        assertEquals(AirlineStatus.INACTIVE, airline.getAirlineStatus());
        verify(airlineRepository).save(airline);
        assertEvent(ReferenceEventType.DEACTIVATED, 9L);
    }

    @Test
    void shouldNotSaveOrPublishWhenAirlineIsAlreadyInactive() {
        Airline airline = airline(10L, AirlineStatus.INACTIVE);
        when(airlineRepository.findById(10L)).thenReturn(Optional.of(airline));

        airlineService.deactivateAirline(10L);

        verify(airlineRepository, never()).save(any(Airline.class));
        verify(publisher, never()).publish(any(ReferenceEvent.class));
    }

    @Test
    void shouldThrowNotFoundWhenDeactivatingMissingAirline() {
        when(airlineRepository.findById(55L)).thenReturn(Optional.empty());

        assertStatus(
                HttpStatus.NOT_FOUND,
                () -> airlineService.deactivateAirline(55L)
        );

        verify(publisher, never()).publish(any(ReferenceEvent.class));
    }

    private AirlineRequest validRequest() {
        AirlineRequest request = new AirlineRequest();
        request.setAirlineName("Turkish Airlines");
        request.setAirlineIataCode("TK");
        request.setAirlineIcaoCode("THY");
        request.setAirlineCountry("Türkiye");
        request.setAirlineStatus(AirlineStatus.ACTIVE);
        return request;
    }

    private Airline airline(Long id, AirlineStatus status) {
        Airline airline = new Airline();
        airline.setAirlineId(id);
        airline.setAirlineName("Turkish Airlines");
        airline.setAirlineIataCode("TK");
        airline.setAirlineIcaoCode("THY");
        airline.setAirlineCountry("Türkiye");
        airline.setAirlineStatus(status);
        return airline;
    }

    private AirlineResponse response(Long id, AirlineStatus status) {
        AirlineResponse response = new AirlineResponse();
        response.setAirlineId(id);
        response.setAirlineStatus(status);
        return response;
    }

    private void assertEvent(ReferenceEventType eventType, Long resourceId) {
        ArgumentCaptor<ReferenceEvent> captor = ArgumentCaptor.forClass(ReferenceEvent.class);
        verify(publisher).publish(captor.capture());
        ReferenceEvent event = captor.getValue();
        assertNotNull(event.getEventId());
        assertNotNull(event.getOccurredAt());
        assertEquals(eventType, event.getEventType());
        assertEquals(ReferenceResourceType.AIRLINE, event.getResourceType());
        assertEquals(resourceId, event.getResourceId());
    }

    private void assertStatus(HttpStatus expectedStatus, Runnable action) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(expectedStatus, exception.getStatusCode());
    }
}
