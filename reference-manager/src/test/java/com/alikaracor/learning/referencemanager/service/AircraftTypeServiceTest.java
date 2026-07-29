package com.alikaracor.learning.referencemanager.service;

import com.alikaracor.learning.referencemanager.dto.AircraftTypeRequest;
import com.alikaracor.learning.referencemanager.dto.AircraftTypeResponse;
import com.alikaracor.learning.referencemanager.event.ReferenceEvent;
import com.alikaracor.learning.referencemanager.event.ReferenceEventType;
import com.alikaracor.learning.referencemanager.event.ReferenceResourceType;
import com.alikaracor.learning.referencemanager.mapper.AircraftTypeMapper;
import com.alikaracor.learning.referencemanager.model.AircraftCategory;
import com.alikaracor.learning.referencemanager.model.AircraftType;
import com.alikaracor.learning.referencemanager.model.AircraftTypeStatus;
import com.alikaracor.learning.referencemanager.publisher.ReferenceEventPublisher;
import com.alikaracor.learning.referencemanager.repository.AircraftTypeRepository;
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
class AircraftTypeServiceTest {

    @Mock
    private AircraftTypeRepository aircraftTypeRepository;

    @Mock
    private AircraftTypeMapper aircraftTypeMapper;

    @Mock
    private ReferenceEventPublisher publisher;

    @InjectMocks
    private AircraftTypeService aircraftTypeService;

    @Test
    void shouldAddAircraftTypeAndPublishCreatedEvent() {
        AircraftTypeRequest request = validRequest();
        AircraftType mapped = new AircraftType();
        AircraftType saved = aircraftType(1L, AircraftTypeStatus.ACTIVE);
        AircraftTypeResponse expectedResponse = response(1L);
        when(aircraftTypeMapper.toAircraftType(request)).thenReturn(mapped);
        when(aircraftTypeRepository.save(mapped)).thenReturn(saved);
        when(aircraftTypeMapper.toAircraftTypeResponse(saved)).thenReturn(expectedResponse);

        AircraftTypeResponse actualResponse = aircraftTypeService.addAircraftType(request);

        assertSame(expectedResponse, actualResponse);
        verify(aircraftTypeRepository).save(mapped);
        assertEvent(ReferenceEventType.CREATED, 1L);
    }

    @Test
    void shouldThrowConflictWhenAircraftTypeIcaoCodeExists() {
        AircraftTypeRequest request = validRequest();
        when(aircraftTypeRepository.existsByAircraftTypeIcaoCodeIgnoreCase(request.getAircraftTypeIcaoCode()))
                .thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () -> aircraftTypeService.addAircraftType(request));

        verify(aircraftTypeRepository, never()).save(any(AircraftType.class));
        verify(publisher, never()).publish(any(ReferenceEvent.class));
    }

    @Test
    void shouldReturnAircraftTypeById() {
        AircraftType aircraftType = aircraftType(2L, AircraftTypeStatus.ACTIVE);
        AircraftTypeResponse expectedResponse = response(2L);
        when(aircraftTypeRepository.findById(2L)).thenReturn(Optional.of(aircraftType));
        when(aircraftTypeMapper.toAircraftTypeResponse(aircraftType)).thenReturn(expectedResponse);

        AircraftTypeResponse actualResponse = aircraftTypeService.getAircraftTypeById(2L);

        assertSame(expectedResponse, actualResponse);
    }

    @Test
    void shouldThrowNotFoundWhenAircraftTypeDoesNotExist() {
        when(aircraftTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () -> aircraftTypeService.getAircraftTypeById(99L));
    }

    @Test
    void shouldReturnAllAircraftTypes() {
        AircraftType first = aircraftType(1L, AircraftTypeStatus.ACTIVE);
        AircraftType second = aircraftType(2L, AircraftTypeStatus.INACTIVE);
        AircraftTypeResponse firstResponse = response(1L);
        AircraftTypeResponse secondResponse = response(2L);
        when(aircraftTypeRepository.findAll()).thenReturn(List.of(first, second));
        when(aircraftTypeMapper.toAircraftTypeResponse(first)).thenReturn(firstResponse);
        when(aircraftTypeMapper.toAircraftTypeResponse(second)).thenReturn(secondResponse);

        List<AircraftTypeResponse> responses = aircraftTypeService.getAllAircraftTypes();

        assertEquals(List.of(firstResponse, secondResponse), responses);
    }

    @Test
    void shouldUpdateAircraftTypeAndPublishUpdatedEvent() {
        AircraftTypeRequest request = validRequest();
        request.setAircraftTypeManufacturer("Airbus");
        request.setAircraftTypeModel("A321neo");
        AircraftType existing = aircraftType(3L, AircraftTypeStatus.ACTIVE);
        AircraftTypeResponse expectedResponse = response(3L);
        when(aircraftTypeRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(aircraftTypeRepository.saveAndFlush(existing)).thenReturn(existing);
        when(aircraftTypeMapper.toAircraftTypeResponse(existing)).thenReturn(expectedResponse);

        AircraftTypeResponse actualResponse = aircraftTypeService.updateAircraftType(3L, request);

        assertSame(expectedResponse, actualResponse);
        assertEquals(request.getAircraftTypeManufacturer(), existing.getAircraftTypeManufacturer());
        assertEquals(request.getAircraftTypeModel(), existing.getAircraftTypeModel());
        assertEquals(request.getAircraftTypeIcaoCode(), existing.getAircraftTypeIcaoCode());
        assertEquals(request.getAircraftTypeCategory(), existing.getAircraftTypeCategory());
        assertEquals(request.getAircraftTypeStatus(), existing.getAircraftTypeStatus());
        verify(aircraftTypeRepository).saveAndFlush(existing);
        assertEvent(ReferenceEventType.UPDATED, 3L);
    }

    @Test
    void shouldRejectDuplicateIcaoCodeDuringUpdate() {
        AircraftTypeRequest request = validRequest();
        AircraftType existing = aircraftType(4L, AircraftTypeStatus.ACTIVE);
        when(aircraftTypeRepository.findById(4L)).thenReturn(Optional.of(existing));
        when(aircraftTypeRepository.existsByAircraftTypeIcaoCodeIgnoreCaseAndAircraftTypeIdNot(
                request.getAircraftTypeIcaoCode(),
                4L
        )).thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () -> aircraftTypeService.updateAircraftType(4L, request));

        verify(aircraftTypeRepository, never()).saveAndFlush(any(AircraftType.class));
        verify(publisher, never()).publish(any(ReferenceEvent.class));
    }

    @Test
    void shouldDeactivateAircraftTypeAndPublishEvent() {
        AircraftType aircraftType = aircraftType(5L, AircraftTypeStatus.ACTIVE);
        when(aircraftTypeRepository.findById(5L)).thenReturn(Optional.of(aircraftType));

        aircraftTypeService.deactivateAircraftType(5L);

        assertEquals(AircraftTypeStatus.INACTIVE, aircraftType.getAircraftTypeStatus());
        verify(aircraftTypeRepository).save(aircraftType);
        assertEvent(ReferenceEventType.DEACTIVATED, 5L);
    }

    @Test
    void shouldDoNothingWhenAircraftTypeIsAlreadyInactive() {
        AircraftType aircraftType = aircraftType(6L, AircraftTypeStatus.INACTIVE);
        when(aircraftTypeRepository.findById(6L)).thenReturn(Optional.of(aircraftType));

        aircraftTypeService.deactivateAircraftType(6L);

        verify(aircraftTypeRepository, never()).save(any(AircraftType.class));
        verify(publisher, never()).publish(any(ReferenceEvent.class));
    }

    private AircraftTypeRequest validRequest() {
        AircraftTypeRequest request = new AircraftTypeRequest();
        request.setAircraftTypeManufacturer("Boeing");
        request.setAircraftTypeModel("737-800");
        request.setAircraftTypeIcaoCode("B738");
        request.setAircraftTypeCategory(AircraftCategory.NARROW_BODY);
        request.setAircraftTypeStatus(AircraftTypeStatus.ACTIVE);
        return request;
    }

    private AircraftType aircraftType(Long id, AircraftTypeStatus status) {
        AircraftType aircraftType = new AircraftType();
        aircraftType.setAircraftTypeId(id);
        aircraftType.setAircraftTypeManufacturer("Boeing");
        aircraftType.setAircraftTypeModel("737-800");
        aircraftType.setAircraftTypeIcaoCode("B738");
        aircraftType.setAircraftTypeCategory(AircraftCategory.NARROW_BODY);
        aircraftType.setAircraftTypeStatus(status);
        return aircraftType;
    }

    private AircraftTypeResponse response(Long id) {
        AircraftTypeResponse response = new AircraftTypeResponse();
        response.setAircraftTypeId(id);
        return response;
    }

    private void assertEvent(ReferenceEventType eventType, Long resourceId) {
        ArgumentCaptor<ReferenceEvent> captor = ArgumentCaptor.forClass(ReferenceEvent.class);
        verify(publisher).publish(captor.capture());
        ReferenceEvent event = captor.getValue();
        assertNotNull(event.getEventId());
        assertNotNull(event.getOccurredAt());
        assertEquals(eventType, event.getEventType());
        assertEquals(ReferenceResourceType.AIRCRAFT_TYPE, event.getResourceType());
        assertEquals(resourceId, event.getResourceId());
    }

    private void assertStatus(HttpStatus status, Runnable action) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(status, exception.getStatusCode());
    }
}
