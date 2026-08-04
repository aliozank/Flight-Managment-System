package com.alikaracor.learning.flightservice.publisher;

import com.alikaracor.learning.flightservice.event.FlightEvent;
import com.alikaracor.learning.flightservice.event.FlightEventType;
import com.alikaracor.learning.flightservice.mapper.FlightMapper;
import com.alikaracor.learning.flightservice.model.Flight;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Component
public class FlightEventPublisher {

    private final KafkaTemplate<String, FlightEvent> kafkaTemplate;
    private final FlightMapper flightMapper;

    public FlightEventPublisher(KafkaTemplate<String, FlightEvent> kafkaTemplate, FlightMapper flightMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.flightMapper = flightMapper;
    }


    public void publish(Flight flight, FlightEventType flightEventType, Long changedByUserId) {

        FlightEvent newFlightEvent = flightMapper.toFlightEvent(flight);

        newFlightEvent.setEventId(UUID.randomUUID());
        newFlightEvent.setEventType(flightEventType);
        newFlightEvent.setChangedByUserId(changedByUserId);
        newFlightEvent.setOccurredAt(Instant.now());

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                                                                        //Kayıt başarı ile databaseye gitemzse diye commit olmadan kafkaya mesaj attırmıyor
                        kafkaTemplate.send(
                                "flight.events",
                                flight.getFlightId().toString(),
                                newFlightEvent
                        );
                    }
                }
        );


    }


}
