package com.alikaracor.learning.referencemanager.publisher;

import com.alikaracor.learning.referencemanager.event.ReferenceEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReferenceEventPublisherTest {

    @Mock
    private KafkaTemplate<String, ReferenceEvent> kafkaTemplate;

    @Test
    void shouldSendReferenceEventToReferenceEventsTopic() {
        ReferenceEventPublisher publisher = new ReferenceEventPublisher(kafkaTemplate);
        ReferenceEvent event = new ReferenceEvent();

        publisher.publish(event);

        verify(kafkaTemplate).send("reference.events", event);
    }
}
