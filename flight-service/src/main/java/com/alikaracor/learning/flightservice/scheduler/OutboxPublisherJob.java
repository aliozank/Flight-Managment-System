package com.alikaracor.learning.flightservice.scheduler;

import com.alikaracor.learning.flightservice.model.OutboxEvent;
import com.alikaracor.learning.flightservice.model.OutboxStatus;
import com.alikaracor.learning.flightservice.repository.OutboxEventRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class OutboxPublisherJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxPublisherJob.class);
    private static final int BATCH_SIZE = 50;
    private static final int MAX_ATTEMPTS = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Duration kafkaSendTimeout;
    private final Duration processingLease;

    public OutboxPublisherJob(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.outbox.kafka-send-timeout:10s}") Duration kafkaSendTimeout,
            @Value("${app.outbox.processing-lease:30s}") Duration processingLease
    ) {
        if (kafkaSendTimeout.isZero() || kafkaSendTimeout.isNegative()) {
            throw new IllegalArgumentException("Kafka send timeout must be positive");
        }
        if (processingLease.compareTo(kafkaSendTimeout) <= 0) {
            throw new IllegalArgumentException("Outbox processing lease must be longer than Kafka send timeout");
        }

        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaSendTimeout = kafkaSendTimeout;
        this.processingLease = processingLease;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publisher-fixed-delay:2s}")
    @SchedulerLock(
            name = "OutboxPublisherJob_publishOutboxEvents",
            lockAtLeastFor = "1s",
            lockAtMostFor = "10m"
    )
    public void publishOutboxEvents() {
        Instant now = Instant.now();
        List<OutboxEvent> claimableEvents = outboxEventRepository.findClaimableEvents(
                OutboxStatus.PENDING,
                OutboxStatus.FAILED,
                OutboxStatus.PROCESSING,
                now,
                PageRequest.of(0, BATCH_SIZE)
        );

        for (OutboxEvent event : claimableEvents) {

            if (!processEvent(event)) {
                break;
            }

        }
    }

    private boolean processEvent(OutboxEvent event) {

        String lockToken = UUID.randomUUID().toString();
        Instant now = Instant.now();
        int claimed = outboxEventRepository.claimEvent(
                event.getOutboxId(),
                lockToken,
                now.plus(processingLease),
                now,
                OutboxStatus.PENDING,
                OutboxStatus.FAILED,
                OutboxStatus.PROCESSING
        );

        if (claimed == 0) {

            return true;

        }

        try {

            kafkaTemplate
                    .send(event.getTopicName(), event.getAggregateId(), event.getPayload())
                    .get(kafkaSendTimeout.toMillis(), TimeUnit.MILLISECONDS);

            int updated = outboxEventRepository.markPublished(
                    event.getOutboxId(),
                    lockToken,
                    Instant.now(),
                    OutboxStatus.PROCESSING,
                    OutboxStatus.PUBLISHED
            );

            if (updated == 0) {

                LOGGER.warn("Outbox publish ACK arrived after claim ownership changed. EventID: {}", event.getEventId());

            }

            return true;
        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();
            handleFailure(event, lockToken, exception);

            return false;
        } catch (ExecutionException exception) {

            Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
            handleFailure(event, lockToken, cause);

            return true;
        } catch (TimeoutException | RuntimeException exception) {

            handleFailure(event, lockToken, exception);
            return true;

        }
    }

    private void handleFailure(OutboxEvent event, String lockToken, Throwable exception) {

        int newAttemptCount = event.getAttemptCount() + 1;
        Instant nextAttemptAt = newAttemptCount >= MAX_ATTEMPTS
                ? null
                : Instant.now().plusSeconds(5L * newAttemptCount);
        String errorMessage = truncateErrorMessage(exception);

        int updated = outboxEventRepository.markFailed(
                event.getOutboxId(),
                lockToken,
                newAttemptCount,
                nextAttemptAt,
                errorMessage,
                OutboxStatus.PROCESSING,
                OutboxStatus.FAILED
        );

        if (updated == 0) {

            LOGGER.warn("Outbox failure arrived after claim ownership changed. EventID: {}", event.getEventId());

        } else if (newAttemptCount >= MAX_ATTEMPTS) {

            LOGGER.error("Outbox event reached maximum retry count. EventID: {}", event.getEventId());

        }
    }

    private String truncateErrorMessage(Throwable exception) {

        String message = exception.getMessage() != null ? exception.getMessage() : "Unknown Error";
        return message.substring(0, Math.min(message.length(), 500));

    }
}
