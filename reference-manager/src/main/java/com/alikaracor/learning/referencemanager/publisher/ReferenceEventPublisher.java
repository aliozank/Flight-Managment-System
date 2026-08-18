
package com.alikaracor.learning.referencemanager.publisher;

import com.alikaracor.learning.referencemanager.event.ReferenceEvent;
import com.alikaracor.learning.referencemanager.service.OutboxService;
import org.springframework.stereotype.Component;

@Component
public class ReferenceEventPublisher {

    private final OutboxService outboxService;

    public ReferenceEventPublisher(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    public void publish(ReferenceEvent referenceEvent) {

        outboxService.saveReferenceEvent(referenceEvent);

    }
}
