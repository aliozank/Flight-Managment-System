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
    private com.alikaracor.learning.referencemanager.service.OutboxService outboxService;

    @Test
    void shouldSaveReferenceEventToOutboxService() {
        ReferenceEventPublisher publisher = new ReferenceEventPublisher(outboxService);
        ReferenceEvent event = new ReferenceEvent();

        publisher.publish(event);

        verify(outboxService).saveReferenceEvent(event);
    }
}
