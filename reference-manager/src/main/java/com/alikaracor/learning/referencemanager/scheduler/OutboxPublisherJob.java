package com.alikaracor.learning.referencemanager.scheduler;

import com.alikaracor.learning.referencemanager.model.OutboxEvent;
import com.alikaracor.learning.referencemanager.model.OutboxStatus;
import com.alikaracor.learning.referencemanager.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxPublisherJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxPublisherJob.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisherJob(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 2000)
    public void publishOutboxEvents() {

        List<OutboxEvent> pendingEvents = outboxEventRepository.findAllByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING,
                PageRequest.of(0, 50)

        );

        for (OutboxEvent event : pendingEvents) {

            processEvent(event);

        }

        List<OutboxEvent> failedEvents = outboxEventRepository.findAllByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                OutboxStatus.FAILED,
                Instant.now(),
                PageRequest.of(0, 50)
        );

        for (OutboxEvent event : failedEvents) {

            processEvent(event);

        }

    }

    private void processEvent(OutboxEvent event) {

        try {
            kafkaTemplate.send(event.getTopicName(), event.getAggregateId(), event.getPayload())
                    .whenComplete((result, ex) -> {
                        if (ex == null) {

                            event.setStatus(OutboxStatus.PUBLISHED);
                            event.setPublishedAt(Instant.now());
                            event.setLastError(null);

                            outboxEventRepository.save(event);
                        }

                        else {

                            handleFailure(event, ex);

                        }
                    });
        }

        catch (Exception ex) {

            handleFailure(event, ex);

        }

    }

    private void handleFailure(OutboxEvent event, Throwable ex) {

        int newAttemptCount = event.getAttemptCount() + 1;
        event.setAttemptCount(newAttemptCount);
        event.setLastError(ex.getMessage() != null ? ex.getMessage().substring(0, Math.min(ex.getMessage().length(), 500)) : "Unknown Error");

        if (newAttemptCount >= 5) {

            event.setStatus(OutboxStatus.FAILED);
            event.setNextAttemptAt(null);

            LOGGER.error("Reference outbox event 5 kez denendi fakat yayınlanamadı. EventID: {}", event.getEventId());
        }

        else {

            event.setStatus(OutboxStatus.FAILED);
            event.setNextAttemptAt(Instant.now().plusSeconds(5L * newAttemptCount));

        }

        outboxEventRepository.save(event);

    }
}
