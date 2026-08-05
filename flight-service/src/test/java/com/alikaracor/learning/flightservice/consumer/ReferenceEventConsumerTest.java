package com.alikaracor.learning.flightservice.consumer;

import com.alikaracor.learning.flightservice.event.ReferenceEvent;
import com.alikaracor.learning.flightservice.event.ReferenceResourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReferenceEventConsumerTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private ReferenceEventConsumer referenceEventConsumer;

    @Test
    @DisplayName("consumeReferenceEvent - AIRLINE event geldiğinde 'airlines' cache'inden entry silinmelidir")
    void consumeReferenceEvent_shouldEvictAirlinesCache_whenResourceTypeIsAirline() {
        ReferenceEvent event = new ReferenceEvent();
        event.setResourceType(ReferenceResourceType.AIRLINE);
        event.setResourceId(10L);

        when(cacheManager.getCache("airlines")).thenReturn(cache);

        referenceEventConsumer.consumeReferenceEvent(event);

        verify(cacheManager).getCache("airlines");
        verify(cache).evict(10L);
    }

    @Test
    @DisplayName("consumeReferenceEvent - AIRPORT event geldiğinde 'airports' cache'inden entry silinmelidir")
    void consumeReferenceEvent_shouldEvictAirportsCache_whenResourceTypeIsAirport() {
        ReferenceEvent event = new ReferenceEvent();
        event.setResourceType(ReferenceResourceType.AIRPORT);
        event.setResourceId(1L);

        when(cacheManager.getCache("airports")).thenReturn(cache);

        referenceEventConsumer.consumeReferenceEvent(event);

        verify(cacheManager).getCache("airports");
        verify(cache).evict(1L);
    }

    @Test
    @DisplayName("consumeReferenceEvent - AIRCRAFT event geldiğinde 'aircrafts' cache'inden entry silinmelidir")
    void consumeReferenceEvent_shouldEvictAircraftsCache_whenResourceTypeIsAircraft() {
        ReferenceEvent event = new ReferenceEvent();
        event.setResourceType(ReferenceResourceType.AIRCRAFT);
        event.setResourceId(100L);

        when(cacheManager.getCache("aircrafts")).thenReturn(cache);

        referenceEventConsumer.consumeReferenceEvent(event);

        verify(cacheManager).getCache("aircrafts");
        verify(cache).evict(100L);
    }

    @Test
    @DisplayName("consumeReferenceEvent - AIRCRAFT_TYPE event geldiğinde 'aircraftTypes' cache'inden entry silinmelidir")
    void consumeReferenceEvent_shouldEvictAircraftTypesCache_whenResourceTypeIsAircraftType() {
        ReferenceEvent event = new ReferenceEvent();
        event.setResourceType(ReferenceResourceType.AIRCRAFT_TYPE);
        event.setResourceId(20L);

        when(cacheManager.getCache("aircraftTypes")).thenReturn(cache);

        referenceEventConsumer.consumeReferenceEvent(event);

        verify(cacheManager).getCache("aircraftTypes");
        verify(cache).evict(20L);
    }

    @Test
    @DisplayName("consumeReferenceEvent - FLIGHT_TYPE event geldiğinde 'flightTypes' cache'inden entry silinmelidir")
    void consumeReferenceEvent_shouldEvictFlightTypesCache_whenResourceTypeIsFlightType() {
        ReferenceEvent event = new ReferenceEvent();
        event.setResourceType(ReferenceResourceType.FLIGHT_TYPE);
        event.setResourceId(5L);

        when(cacheManager.getCache("flightTypes")).thenReturn(cache);

        referenceEventConsumer.consumeReferenceEvent(event);

        verify(cacheManager).getCache("flightTypes");
        verify(cache).evict(5L);
    }

    @Test
    @DisplayName("consumeReferenceEvent - ROUTE event geldiğinde 'routes' cache'i tamamen temizlenmelidir")
    void consumeReferenceEvent_shouldClearRoutesCache_whenResourceTypeIsRoute() {
        ReferenceEvent event = new ReferenceEvent();
        event.setResourceType(ReferenceResourceType.ROUTE);
        event.setResourceId(50L);

        when(cacheManager.getCache("routes")).thenReturn(cache);

        referenceEventConsumer.consumeReferenceEvent(event);

        verify(cacheManager).getCache("routes");
        verify(cache).clear();
    }

    @Test
    @DisplayName("consumeReferenceEvent - Cache nesnesi null döndüğünde NPE fırlatmamalıdır")
    void consumeReferenceEvent_shouldNotThrowException_whenCacheIsNull() {
        ReferenceEvent event = new ReferenceEvent();
        event.setResourceType(ReferenceResourceType.AIRLINE);
        event.setResourceId(10L);

        when(cacheManager.getCache("airlines")).thenReturn(null);

        assertThatCode(() -> referenceEventConsumer.consumeReferenceEvent(event))
                .doesNotThrowAnyException();
    }
}
