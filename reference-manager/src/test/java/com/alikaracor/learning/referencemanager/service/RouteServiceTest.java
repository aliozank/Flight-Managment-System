package com.alikaracor.learning.referencemanager.service;

import com.alikaracor.learning.referencemanager.dto.RouteRequest;
import com.alikaracor.learning.referencemanager.dto.RouteResponse;
import com.alikaracor.learning.referencemanager.event.ReferenceEvent;
import com.alikaracor.learning.referencemanager.event.ReferenceEventType;
import com.alikaracor.learning.referencemanager.event.ReferenceResourceType;
import com.alikaracor.learning.referencemanager.mapper.RouteMapper;
import com.alikaracor.learning.referencemanager.model.Airport;
import com.alikaracor.learning.referencemanager.model.Route;
import com.alikaracor.learning.referencemanager.model.RouteStatus;
import com.alikaracor.learning.referencemanager.publisher.ReferenceEventPublisher;
import com.alikaracor.learning.referencemanager.repository.AirportRepository;
import com.alikaracor.learning.referencemanager.repository.RouteRepository;
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
class RouteServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private RouteMapper routeMapper;

    @Mock
    private AirportRepository airportRepository;

    @Mock
    private ReferenceEventPublisher publisher;

    @InjectMocks
    private RouteService routeService;

    @Test
    void shouldAddRouteAndPublishCreatedEvent() {
        RouteRequest request = validRequest();
        Airport origin = airport(1L);
        Airport destination = airport(2L);
        Route mapped = new Route();
        Route saved = route(10L, RouteStatus.ACTIVE);
        RouteResponse expected = response(10L);
        when(airportRepository.findById(1L)).thenReturn(Optional.of(origin));
        when(airportRepository.findById(2L)).thenReturn(Optional.of(destination));
        when(routeMapper.toRoute(request)).thenReturn(mapped);
        when(routeRepository.save(mapped)).thenReturn(saved);
        when(routeMapper.toRouteResponse(saved)).thenReturn(expected);

        RouteResponse actual = routeService.addRoute(request);

        assertSame(expected, actual);
        assertSame(origin, mapped.getOriginAirport());
        assertSame(destination, mapped.getDestinationAirport());
        assertEvent(ReferenceEventType.CREATED, 10L);
    }

    @Test
    void shouldRejectRouteWithSameOriginAndDestination() {
        RouteRequest request = validRequest();
        request.setDestinationAirportId(1L);

        assertStatus(HttpStatus.BAD_REQUEST, () -> routeService.addRoute(request));

        verify(routeRepository, never()).save(any(Route.class));
        verify(publisher, never()).publish(any(ReferenceEvent.class));
    }

    @Test
    void shouldRejectDuplicateRoute() {
        RouteRequest request = validRequest();
        when(routeRepository.existsByOriginAirport_AirportIdAndDestinationAirport_AirportId(1L, 2L))
                .thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () -> routeService.addRoute(request));
    }

    @Test
    void shouldRejectRouteWhenOriginAirportDoesNotExist() {
        RouteRequest request = validRequest();
        when(airportRepository.findById(1L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () -> routeService.addRoute(request));
    }

    @Test
    void shouldRejectRouteWhenDestinationAirportDoesNotExist() {
        RouteRequest request = validRequest();
        when(airportRepository.findById(1L)).thenReturn(Optional.of(airport(1L)));
        when(airportRepository.findById(2L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () -> routeService.addRoute(request));
    }

    @Test
    void shouldReturnRouteById() {
        Route route = route(11L, RouteStatus.ACTIVE);
        RouteResponse expected = response(11L);
        when(routeRepository.findById(11L)).thenReturn(Optional.of(route));
        when(routeMapper.toRouteResponse(route)).thenReturn(expected);

        assertSame(expected, routeService.getRouteById(11L));
    }

    @Test
    void shouldReturnAllRoutes() {
        Route first = route(1L, RouteStatus.ACTIVE);
        Route second = route(2L, RouteStatus.INACTIVE);
        RouteResponse firstResponse = response(1L);
        RouteResponse secondResponse = response(2L);
        when(routeRepository.findAll()).thenReturn(List.of(first, second));
        when(routeMapper.toRouteResponse(first)).thenReturn(firstResponse);
        when(routeMapper.toRouteResponse(second)).thenReturn(secondResponse);

        assertEquals(List.of(firstResponse, secondResponse), routeService.getAllRoutes());
    }

    @Test
    void shouldUpdateRouteAndPublishUpdatedEvent() {
        RouteRequest request = validRequest();
        Airport origin = airport(1L);
        Airport destination = airport(2L);
        Route existing = route(12L, RouteStatus.INACTIVE);
        RouteResponse expected = response(12L);
        when(routeRepository.findById(12L)).thenReturn(Optional.of(existing));
        when(airportRepository.findById(1L)).thenReturn(Optional.of(origin));
        when(airportRepository.findById(2L)).thenReturn(Optional.of(destination));
        when(routeRepository.saveAndFlush(existing)).thenReturn(existing);
        when(routeMapper.toRouteResponse(existing)).thenReturn(expected);

        RouteResponse actual = routeService.updateRouteById(request, 12L);

        assertSame(expected, actual);
        assertEquals(RouteStatus.ACTIVE, existing.getRouteStatus());
        assertSame(origin, existing.getOriginAirport());
        assertSame(destination, existing.getDestinationAirport());
        assertEvent(ReferenceEventType.UPDATED, 12L);
    }

    @Test
    void shouldRejectDuplicateRouteDuringUpdate() {
        RouteRequest request = validRequest();
        Route existing = route(13L, RouteStatus.ACTIVE);
        when(routeRepository.findById(13L)).thenReturn(Optional.of(existing));
        when(airportRepository.findById(1L)).thenReturn(Optional.of(airport(1L)));
        when(airportRepository.findById(2L)).thenReturn(Optional.of(airport(2L)));
        when(routeRepository.existsByOriginAirport_AirportIdAndDestinationAirport_AirportIdAndRouteIdNot(
                1L,
                2L,
                13L
        )).thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () -> routeService.updateRouteById(request, 13L));
    }

    @Test
    void shouldDeactivateRouteAndPublishEvent() {
        Route route = route(14L, RouteStatus.ACTIVE);
        when(routeRepository.findById(14L)).thenReturn(Optional.of(route));

        routeService.deactiveRouteById(14L);

        assertEquals(RouteStatus.INACTIVE, route.getRouteStatus());
        verify(routeRepository).save(route);
        assertEvent(ReferenceEventType.DEACTIVATED, 14L);
    }

    @Test
    void shouldDoNothingWhenRouteIsAlreadyInactive() {
        Route route = route(15L, RouteStatus.INACTIVE);
        when(routeRepository.findById(15L)).thenReturn(Optional.of(route));

        routeService.deactiveRouteById(15L);

        verify(routeRepository, never()).save(any(Route.class));
        verify(publisher, never()).publish(any(ReferenceEvent.class));
    }

    private RouteRequest validRequest() {
        RouteRequest request = new RouteRequest();
        request.setOriginAirportId(1L);
        request.setDestinationAirportId(2L);
        request.setRouteStatus(RouteStatus.ACTIVE);
        return request;
    }

    private Airport airport(Long id) {
        Airport airport = new Airport();
        airport.setAirportId(id);
        return airport;
    }

    private Route route(Long id, RouteStatus status) {
        Route route = new Route();
        route.setRouteId(id);
        route.setRouteStatus(status);
        return route;
    }

    private RouteResponse response(Long id) {
        RouteResponse response = new RouteResponse();
        response.setRouteId(id);
        return response;
    }

    private void assertEvent(ReferenceEventType type, Long id) {
        ArgumentCaptor<ReferenceEvent> captor = ArgumentCaptor.forClass(ReferenceEvent.class);
        verify(publisher).publish(captor.capture());
        ReferenceEvent event = captor.getValue();
        assertNotNull(event.getEventId());
        assertNotNull(event.getOccurredAt());
        assertEquals(type, event.getEventType());
        assertEquals(ReferenceResourceType.ROUTE, event.getResourceType());
        assertEquals(id, event.getResourceId());
    }

    private void assertStatus(HttpStatus status, Runnable action) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(status, exception.getStatusCode());
    }
}
