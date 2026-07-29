package com.alikaracor.learning.referencemanager.service;

import com.alikaracor.learning.referencemanager.dto.FlightTypeRequest;
import com.alikaracor.learning.referencemanager.dto.FlightTypeResponse;
import com.alikaracor.learning.referencemanager.event.ReferenceEvent;
import com.alikaracor.learning.referencemanager.event.ReferenceEventType;
import com.alikaracor.learning.referencemanager.event.ReferenceResourceType;
import com.alikaracor.learning.referencemanager.mapper.FlightTypeMapper;
import com.alikaracor.learning.referencemanager.model.FlightType;
import com.alikaracor.learning.referencemanager.model.FlightTypeStatus;
import com.alikaracor.learning.referencemanager.publisher.ReferenceEventPublisher;
import com.alikaracor.learning.referencemanager.repository.FlightTypeRepository;
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
class FlightTypeServiceTest {

    @Mock
    private FlightTypeRepository flightTypeRepository;

    @Mock
    private FlightTypeMapper flightTypeMapper;

    @Mock
    private ReferenceEventPublisher publisher;

    @InjectMocks
    private FlightTypeService flightTypeService;

    @Test
    void shouldAddFlightTypeAndPublishCreatedEvent() {
        FlightTypeRequest request = validRequest();
        request.setFlightTypeName("  Passenger  ");
        FlightType mapped = new FlightType();
        FlightType saved = flightType(1L, FlightTypeStatus.ACTIVE);
        FlightTypeResponse expected = response(1L);
        when(flightTypeMapper.toFlightType(request)).thenReturn(mapped);
        when(flightTypeRepository.save(mapped)).thenReturn(saved);
        when(flightTypeMapper.toFlightTypeResponse(saved)).thenReturn(expected);

        FlightTypeResponse actual = flightTypeService.addFlightType(request);

        assertSame(expected, actual);
        assertEquals("Passenger", mapped.getFlightTypeName());
        assertEvent(ReferenceEventType.CREATED, 1L);
    }

    @Test
    void shouldRejectDuplicateFlightTypeName() {
        FlightTypeRequest request = validRequest();
        when(flightTypeRepository.existsByFlightTypeNameIgnoreCase(request.getFlightTypeName()))
                .thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () -> flightTypeService.addFlightType(request));

        verify(flightTypeRepository, never()).save(any(FlightType.class));
        verify(publisher, never()).publish(any(ReferenceEvent.class));
    }

    @Test
    void shouldRejectDuplicateFlightTypeCode() {
        FlightTypeRequest request = validRequest();
        when(flightTypeRepository.existsByFlightTypeCodeIgnoreCase(request.getFlightTypeCode()))
                .thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () -> flightTypeService.addFlightType(request));
    }

    @Test
    void shouldReturnFlightTypeById() {
        FlightType flightType = flightType(2L, FlightTypeStatus.ACTIVE);
        FlightTypeResponse expected = response(2L);
        when(flightTypeRepository.findById(2L)).thenReturn(Optional.of(flightType));
        when(flightTypeMapper.toFlightTypeResponse(flightType)).thenReturn(expected);

        assertSame(expected, flightTypeService.getFlightTypeById(2L));
    }

    @Test
    void shouldReturnOnlyActiveFlightTypesInRepositoryOrder() {
        FlightType first = flightType(1L, FlightTypeStatus.ACTIVE);
        FlightType second = flightType(2L, FlightTypeStatus.ACTIVE);
        FlightTypeResponse firstResponse = response(1L);
        FlightTypeResponse secondResponse = response(2L);
        when(flightTypeRepository.findAllByFlightTypeStatusOrderByFlightTypeNameAsc(FlightTypeStatus.ACTIVE))
                .thenReturn(List.of(first, second));
        when(flightTypeMapper.toFlightTypeResponse(first)).thenReturn(firstResponse);
        when(flightTypeMapper.toFlightTypeResponse(second)).thenReturn(secondResponse);

        List<FlightTypeResponse> responses = flightTypeService.getActiveFlightTypes();

        assertEquals(List.of(firstResponse, secondResponse), responses);
    }

    @Test
    void shouldUpdateFlightTypeAndPublishUpdatedEvent() {
        FlightTypeRequest request = validRequest();
        request.setFlightTypeName("  Cargo  ");
        request.setFlightTypeCode("CARGO");
        FlightType existing = flightType(3L, FlightTypeStatus.ACTIVE);
        FlightTypeResponse expected = response(3L);
        when(flightTypeRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(flightTypeRepository.saveAndFlush(existing)).thenReturn(existing);
        when(flightTypeMapper.toFlightTypeResponse(existing)).thenReturn(expected);

        FlightTypeResponse actual = flightTypeService.updateFlightTypeById(3L, request);

        assertSame(expected, actual);
        assertEquals("Cargo", existing.getFlightTypeName());
        assertEquals("CARGO", existing.getFlightTypeCode());
        verify(flightTypeRepository).saveAndFlush(existing);
        assertEvent(ReferenceEventType.UPDATED, 3L);
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingMissingFlightType() {
        when(flightTypeRepository.findById(88L)).thenReturn(Optional.empty());

        assertStatus(
                HttpStatus.NOT_FOUND,
                () -> flightTypeService.updateFlightTypeById(88L, validRequest())
        );
    }

    @Test
    void shouldDeactivateFlightTypeAndPublishEvent() {
        FlightType flightType = flightType(4L, FlightTypeStatus.ACTIVE);
        when(flightTypeRepository.findById(4L)).thenReturn(Optional.of(flightType));

        flightTypeService.deactivateFlightTypeById(4L);

        assertEquals(FlightTypeStatus.INACTIVE, flightType.getFlightTypeStatus());
        verify(flightTypeRepository).save(flightType);
        assertEvent(ReferenceEventType.DEACTIVATED, 4L);
    }

    @Test
    void shouldDoNothingWhenFlightTypeIsAlreadyInactive() {
        FlightType flightType = flightType(5L, FlightTypeStatus.INACTIVE);
        when(flightTypeRepository.findById(5L)).thenReturn(Optional.of(flightType));

        flightTypeService.deactivateFlightTypeById(5L);

        verify(flightTypeRepository, never()).save(any(FlightType.class));
        verify(publisher, never()).publish(any(ReferenceEvent.class));
    }

    private FlightTypeRequest validRequest() {
        FlightTypeRequest request = new FlightTypeRequest();
        request.setFlightTypeName("Passenger");
        request.setFlightTypeCode("PASSENGER");
        request.setFlightTypeStatus(FlightTypeStatus.ACTIVE);
        return request;
    }

    private FlightType flightType(Long id, FlightTypeStatus status) {
        FlightType flightType = new FlightType();
        flightType.setFlightTypeId(id);
        flightType.setFlightTypeName("Passenger");
        flightType.setFlightTypeCode("PASSENGER");
        flightType.setFlightTypeStatus(status);
        return flightType;
    }

    private FlightTypeResponse response(Long id) {
        FlightTypeResponse response = new FlightTypeResponse();
        response.setFlightTypeId(id);
        return response;
    }

    private void assertEvent(ReferenceEventType type, Long id) {
        ArgumentCaptor<ReferenceEvent> captor = ArgumentCaptor.forClass(ReferenceEvent.class);
        verify(publisher).publish(captor.capture());
        ReferenceEvent event = captor.getValue();
        assertNotNull(event.getEventId());
        assertNotNull(event.getOccurredAt());
        assertEquals(type, event.getEventType());
        assertEquals(ReferenceResourceType.FLIGHT_TYPE, event.getResourceType());
        assertEquals(id, event.getResourceId());
    }

    private void assertStatus(HttpStatus status, Runnable action) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(status, exception.getStatusCode());
    }
}
