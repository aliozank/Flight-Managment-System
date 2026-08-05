package com.alikaracor.learning.flightservice.publisher;


import com.alikaracor.learning.flightservice.dto.FlightResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class FlightWebSocketPublisher {

    private final SimpMessagingTemplate simpMessagingTemplate;

    public FlightWebSocketPublisher(SimpMessagingTemplate simpMessagingTemplate) {

        this.simpMessagingTemplate = simpMessagingTemplate;

    }

    public void publish(FlightResponse flightResponse) {

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {

                        simpMessagingTemplate.convertAndSend("/topic/flights", flightResponse);

                    }

                }
        );
    }

}
