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
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RouteService {

    private final RouteRepository routeRepository;
    private final RouteMapper routeMapper;
    private final AirportRepository airportRepository;
    private final ReferenceEventPublisher publisher;

    public RouteService(RouteRepository routeRepository, RouteMapper routeMapper, AirportRepository airportRepository, ReferenceEventPublisher publisher) {
        this.routeRepository = routeRepository;
        this.routeMapper = routeMapper;
        this.airportRepository = airportRepository;
        this.publisher = publisher;
    }

    public RouteResponse getRouteById(Long routeId) {

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Bu id ile eşleşen route yok"));

        return routeMapper.toRouteResponse(route);

    }

    public List<RouteResponse> getAllRoutes() {

        List<RouteResponse> routeResponses = routeRepository.findAll()
                .stream()
                .map(routeMapper::toRouteResponse)
                .toList();
        return routeResponses;

    }

    @Transactional
    public RouteResponse addRoute(RouteRequest routeRequest) {

        Long originAirportId = routeRequest.getOriginAirportId();
        Long destinationAirportId = routeRequest.getDestinationAirportId();

        if (originAirportId.equals(destinationAirportId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Varış ve kalkış aynı havalimanı olamaz");
        }

        if (routeRepository.existsByOriginAirport_AirportIdAndDestinationAirport_AirportId(originAirportId, destinationAirportId)) {

            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Aynı rota mevcut");

        }


        Airport originAirport = airportRepository.findById(originAirportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Kalkış havalimanı id si ile eşleşen bir id bulunamadı"));

        Airport destinationAirport = airportRepository.findById(destinationAirportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Varış havalimanı id si ile eşleşen bir id bulunamadı"));

        Route newRoute = routeMapper.toRoute(routeRequest);

        newRoute.setOriginAirport(originAirport);
        newRoute.setDestinationAirport(destinationAirport);

        Route savedRoute = routeRepository.save(newRoute);

        ReferenceEvent referenceEvent = new ReferenceEvent();
        referenceEvent.setOccurredAt(Instant.now());
        referenceEvent.setResourceType(ReferenceResourceType.ROUTE);
        referenceEvent.setEventType(ReferenceEventType.CREATED);
        referenceEvent.setEventId(UUID.randomUUID());
        referenceEvent.setResourceId(savedRoute.getRouteId());

        publisher.publish(referenceEvent);

        return routeMapper.toRouteResponse(savedRoute);

    }

    @Transactional
    public RouteResponse updateRouteById(RouteRequest routeRequest, Long routeId) {

        Route updatedRoute = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bu id ile rota mevcut değil"));

        Long originAirportId = routeRequest.getOriginAirportId();
        Long destinationAirportId = routeRequest.getDestinationAirportId();

        if (originAirportId.equals(destinationAirportId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Varış ve kalkış aynı havalimanı olamaz");
        }

        Airport originAirport = airportRepository.findById(originAirportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Kalkış havalimanı id si ile eşleşen bir id bulunamadı"));

        Airport destinationAirport = airportRepository.findById(destinationAirportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Varış havalimanı id si ile eşleşen bir id bulunamadı"));

        if (routeRepository.existsByOriginAirport_AirportIdAndDestinationAirport_AirportIdAndRouteIdNot(originAirportId, destinationAirportId, routeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Güncellenmek istenen rota mevcut");
        }

        updatedRoute.setRouteStatus(routeRequest.getRouteStatus());
        updatedRoute.setOriginAirport(originAirport);
        updatedRoute.setDestinationAirport(destinationAirport);

        Route savedRoute = routeRepository.saveAndFlush(updatedRoute);

        ReferenceEvent referenceEvent = new ReferenceEvent();
        referenceEvent.setOccurredAt(Instant.now());
        referenceEvent.setResourceType(ReferenceResourceType.ROUTE);
        referenceEvent.setEventType(ReferenceEventType.UPDATED);
        referenceEvent.setEventId(UUID.randomUUID());
        referenceEvent.setResourceId(savedRoute.getRouteId());

        publisher.publish(referenceEvent);

        return routeMapper.toRouteResponse(savedRoute);

    }

    @Transactional
    public void deactiveRouteById(Long routeId) {

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bu id ile rota mevcut değil"));

        if(route.getRouteStatus() == RouteStatus.INACTIVE){

            return;

        }

        route.setRouteStatus(RouteStatus.INACTIVE);

        routeRepository.save(route);

        ReferenceEvent referenceEvent = new ReferenceEvent();
        referenceEvent.setOccurredAt(Instant.now());
        referenceEvent.setResourceType(ReferenceResourceType.ROUTE);
        referenceEvent.setEventType(ReferenceEventType.DEACTIVATED);
        referenceEvent.setEventId(UUID.randomUUID());
        referenceEvent.setResourceId(route.getRouteId());

        publisher.publish(referenceEvent);

    }
}
