package com.alikaracor.learning.referencemanager.controller;

import com.alikaracor.learning.referencemanager.dto.RouteRequest;
import com.alikaracor.learning.referencemanager.dto.RouteResponse;
import com.alikaracor.learning.referencemanager.service.RouteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;

    }

    @GetMapping
    public List<RouteResponse> getAllRoutes() {
        return routeService.getAllRoutes();
    }

    @GetMapping("/{routeId}")
    public RouteResponse getRouteById(@PathVariable Long routeId) {
        return routeService.getRouteById(routeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RouteResponse addRoute(@Valid @RequestBody RouteRequest routeRequest) {
        return routeService.addRoute(routeRequest);
    }

    @PutMapping("/{routeId}")
    public RouteResponse updateRouteById(@PathVariable Long routeId, @Valid @RequestBody RouteRequest routeRequest) {
        return routeService.updateRouteById(routeRequest, routeId);
    }

    @DeleteMapping("/{routeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateRouteById(@PathVariable Long routeId) {
        routeService.deactiveRouteById(routeId);
    }
}
