package com.alikaracor.learning.flightservice.consumer;

import com.alikaracor.learning.flightservice.event.ReferenceEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class ReferenceEventConsumer {

    private final CacheManager cacheManager;

    public ReferenceEventConsumer(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    private void evictCacheEntry(String cacheName, Long resourceId) {

        Cache cache = cacheManager.getCache(cacheName);

        if (cache != null) {
            cache.evict(resourceId);
        }
    }

    private void clearCache(String cacheName) {

        Cache cache = cacheManager.getCache(cacheName);

        if (cache != null) {
            cache.clear();
        }
    }

    @KafkaListener(
            topics = "reference.events",
            groupId = "flight-service-reference-cache"
    )
    public void consumeReferenceEvent(@Payload ReferenceEvent referenceEvent) {

        Long resourceId = referenceEvent.getResourceId();

        switch (referenceEvent.getResourceType()) {

            case AIRLINE ->
                    evictCacheEntry("airlines", resourceId);

            case AIRPORT ->
                    evictCacheEntry("airports", resourceId);

            case AIRCRAFT ->
                    evictCacheEntry("aircrafts", resourceId);

            case AIRCRAFT_TYPE ->
                    evictCacheEntry("aircraftTypes", resourceId);

            case FLIGHT_TYPE ->
                    evictCacheEntry("flightTypes", resourceId);

            case ROUTE ->
                    clearCache("routes");

        }


    }

}
