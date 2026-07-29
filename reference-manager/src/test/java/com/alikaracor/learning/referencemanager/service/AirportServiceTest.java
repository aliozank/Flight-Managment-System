package com.alikaracor.learning.referencemanager.service;

import com.alikaracor.learning.referencemanager.dto.AirportRequest;
import com.alikaracor.learning.referencemanager.dto.AirportResponse;
import com.alikaracor.learning.referencemanager.event.ReferenceEvent;
import com.alikaracor.learning.referencemanager.event.ReferenceEventType;
import com.alikaracor.learning.referencemanager.event.ReferenceResourceType;
import com.alikaracor.learning.referencemanager.mapper.AirportMapper;
import com.alikaracor.learning.referencemanager.model.Airport;
import com.alikaracor.learning.referencemanager.model.AirportStatus;
import com.alikaracor.learning.referencemanager.publisher.ReferenceEventPublisher;
import com.alikaracor.learning.referencemanager.repository.AirportRepository;
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
class AirportServiceTest {

    @Mock
    private AirportRepository airportRepository;

    @Mock
    private AirportMapper airportMapper;

    @Mock
    private ReferenceEventPublisher publisher;

    @InjectMocks
    private AirportService airportService;

    @Test
    void shouldAddAirportAndPublishCreatedEvent() {
        AirportRequest request = validRequest();
        request.setAirportTimezone("  Europe/Istanbul  ");
        Airport mapped = new Airport();
        Airport saved = airport(1L, AirportStatus.OPERATIONAL);
        AirportResponse expected = response(1L);
        when(airportMapper.toAirport(request)).thenReturn(mapped);
        when(airportRepository.save(mapped)).thenReturn(saved);
        when(airportMapper.toAirportResponse(saved)).thenReturn(expected);

        AirportResponse actual = airportService.addAirport(request);

        assertSame(expected, actual);
        assertEquals("Europe/Istanbul", mapped.getAirportTimezone());
        verify(airportRepository).save(mapped);
        assertEvent(ReferenceEventType.CREATED, 1L);
    }

    @Test
    void shouldRejectDuplicateIataCode() {
        AirportRequest request = validRequest();
        when(airportRepository.existsByAirportIataCodeIgnoreCase(request.getAirportIataCode()))
                .thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () -> airportService.addAirport(request));

        verify(airportRepository, never()).save(any(Airport.class));
        verify(publisher, never()).publish(any(ReferenceEvent.class));
    }

    @Test
    void shouldRejectDuplicateIcaoCode() {
        AirportRequest request = validRequest();
        when(airportRepository.existsByAirportIcaoCodeIgnoreCase(request.getAirportIcaoCode()))
                .thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () -> airportService.addAirport(request));
    }

    @Test
    void shouldRejectInvalidTimezone() {
        AirportRequest request = validRequest();
        request.setAirportTimezone("Not/A-Timezone");

        assertStatus(HttpStatus.BAD_REQUEST, () -> airportService.addAirport(request));

        verify(airportRepository, never()).save(any(Airport.class));
        verify(publisher, never()).publish(any(ReferenceEvent.class));
    }

    @Test
    void shouldReturnAirportById() {
        Airport airport = airport(2L, AirportStatus.OPERATIONAL);
        AirportResponse expected = response(2L);
        when(airportRepository.findById(2L)).thenReturn(Optional.of(airport));
        when(airportMapper.toAirportResponse(airport)).thenReturn(expected);

        assertSame(expected, airportService.getAirportById(2L));
    }

    @Test
    void shouldReturnAirportByIataCodeIgnoringCase() {
        Airport airport = airport(3L, AirportStatus.OPERATIONAL);
        AirportResponse expected = response(3L);
        when(airportRepository.findByAirportIataCodeIgnoreCase("ist")).thenReturn(Optional.of(airport));
        when(airportMapper.toAirportResponse(airport)).thenReturn(expected);

        assertSame(expected, airportService.getAirportByIataCode("ist"));
    }

    @Test
    void shouldReturnAirportByIcaoCodeIgnoringCase() {
        Airport airport = airport(4L, AirportStatus.OPERATIONAL);
        AirportResponse expected = response(4L);
        when(airportRepository.findByAirportIcaoCodeIgnoreCase("ltfm")).thenReturn(Optional.of(airport));
        when(airportMapper.toAirportResponse(airport)).thenReturn(expected);

        assertSame(expected, airportService.getAirportByIcaoCode("ltfm"));
    }

    @Test
    void shouldReturnAllAirports() {
        Airport first = airport(1L, AirportStatus.OPERATIONAL);
        Airport second = airport(2L, AirportStatus.TEMPORARILY_CLOSED);
        AirportResponse firstResponse = response(1L);
        AirportResponse secondResponse = response(2L);
        when(airportRepository.findAll()).thenReturn(List.of(first, second));
        when(airportMapper.toAirportResponse(first)).thenReturn(firstResponse);
        when(airportMapper.toAirportResponse(second)).thenReturn(secondResponse);

        assertEquals(List.of(firstResponse, secondResponse), airportService.getAllAirports());
    }

    @Test
    void shouldUpdateAirportAndPublishUpdatedEvent() {
        AirportRequest request = validRequest();
        request.setAirportName("Updated Airport");
        request.setAirportTimezone("Europe/Berlin");
        Airport existing = airport(5L, AirportStatus.OPERATIONAL);
        AirportResponse expected = response(5L);
        when(airportRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(airportRepository.saveAndFlush(existing)).thenReturn(existing);
        when(airportMapper.toAirportResponse(existing)).thenReturn(expected);

        AirportResponse actual = airportService.updateAirport(5L, request);

        assertSame(expected, actual);
        assertEquals("Updated Airport", existing.getAirportName());
        assertEquals("Europe/Berlin", existing.getAirportTimezone());
        verify(airportRepository).saveAndFlush(existing);
        assertEvent(ReferenceEventType.UPDATED, 5L);
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingMissingAirport() {
        when(airportRepository.findById(66L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () -> airportService.updateAirport(66L, validRequest()));
    }

    @Test
    void shouldDeactivateAirportAndPublishEvent() {
        Airport airport = airport(6L, AirportStatus.OPERATIONAL);
        when(airportRepository.findById(6L)).thenReturn(Optional.of(airport));

        airportService.deactivateAirport(6L);

        assertEquals(AirportStatus.PERMANENTLY_CLOSED, airport.getAirportStatus());
        verify(airportRepository).save(airport);
        assertEvent(ReferenceEventType.DEACTIVATED, 6L);
    }

    @Test
    void shouldDoNothingWhenAirportIsAlreadyPermanentlyClosed() {
        Airport airport = airport(7L, AirportStatus.PERMANENTLY_CLOSED);
        when(airportRepository.findById(7L)).thenReturn(Optional.of(airport));

        airportService.deactivateAirport(7L);

        verify(airportRepository, never()).save(any(Airport.class));
        verify(publisher, never()).publish(any(ReferenceEvent.class));
    }

    private AirportRequest validRequest() {
        AirportRequest request = new AirportRequest();
        request.setAirportName("Istanbul Airport");
        request.setAirportCity("Istanbul");
        request.setAirportCountry("Türkiye");
        request.setAirportIataCode("IST");
        request.setAirportIcaoCode("LTFM");
        request.setAirportTimezone("Europe/Istanbul");
        request.setAirportStatus(AirportStatus.OPERATIONAL);
        return request;
    }

    private Airport airport(Long id, AirportStatus status) {
        Airport airport = new Airport();
        airport.setAirportId(id);
        airport.setAirportName("Istanbul Airport");
        airport.setAirportCity("Istanbul");
        airport.setAirportCountry("Türkiye");
        airport.setAirportIataCode("IST");
        airport.setAirportIcaoCode("LTFM");
        airport.setAirportTimezone("Europe/Istanbul");
        airport.setAirportStatus(status);
        return airport;
    }

    private AirportResponse response(Long id) {
        AirportResponse response = new AirportResponse();
        response.setAirportId(id);
        return response;
    }

    private void assertEvent(ReferenceEventType type, Long id) {
        ArgumentCaptor<ReferenceEvent> captor = ArgumentCaptor.forClass(ReferenceEvent.class);
        verify(publisher).publish(captor.capture());
        ReferenceEvent event = captor.getValue();
        assertNotNull(event.getEventId());
        assertNotNull(event.getOccurredAt());
        assertEquals(type, event.getEventType());
        assertEquals(ReferenceResourceType.AIRPORT, event.getResourceType());
        assertEquals(id, event.getResourceId());
    }

    private void assertStatus(HttpStatus status, Runnable action) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(status, exception.getStatusCode());
    }
}
