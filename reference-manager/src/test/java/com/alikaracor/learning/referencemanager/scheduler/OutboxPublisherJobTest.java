package com.alikaracor.learning.referencemanager.scheduler;

import com.alikaracor.learning.referencemanager.model.OutboxEvent;
import com.alikaracor.learning.referencemanager.model.OutboxStatus;
import com.alikaracor.learning.referencemanager.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherJobTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxPublisherJob outboxPublisherJob;
    private OutboxEvent pendingEvent;

    @BeforeEach
    void setUp() {
        outboxPublisherJob = new OutboxPublisherJob(
                outboxEventRepository,
                kafkaTemplate,
                Duration.ofSeconds(1),
                Duration.ofSeconds(5)
        );

        pendingEvent = new OutboxEvent();
        pendingEvent.setOutboxId(UUID.randomUUID().toString());
        pendingEvent.setEventId(UUID.randomUUID().toString());
        pendingEvent.setAggregateType("AIRLINE");
        pendingEvent.setAggregateId("10");
        pendingEvent.setEventType("CREATED");
        pendingEvent.setTopicName("reference.events");
        pendingEvent.setPayload("{\"resourceId\":10}");
        pendingEvent.setStatus(OutboxStatus.PENDING);
        pendingEvent.setAttemptCount(0);
        pendingEvent.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("Kafka ACK geldiğinde claim edilen reference event PUBLISHED yapılmalıdır")
    void shouldMarkClaimedEventAsPublishedWhenKafkaAcknowledges() {
        stubClaimableEvent();
        when(kafkaTemplate.send("reference.events", "10", "{\"resourceId\":10}"))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(outboxEventRepository.markPublished(
                eq(pendingEvent.getOutboxId()), anyString(), any(Instant.class),
                eq(OutboxStatus.PROCESSING), eq(OutboxStatus.PUBLISHED)
        )).thenReturn(1);

        outboxPublisherJob.publishOutboxEvents();

        verify(outboxEventRepository).markPublished(
                eq(pendingEvent.getOutboxId()), anyString(), any(Instant.class),
                eq(OutboxStatus.PROCESSING), eq(OutboxStatus.PUBLISHED)
        );
    }

    @Test
    @DisplayName("Kafka hatasında reference event retry edilebilir FAILED yapılmalıdır")
    void shouldMarkClaimedEventAsFailedWhenKafkaSendFails() {
        stubClaimableEvent();
        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka Broker Down"));
        when(kafkaTemplate.send("reference.events", "10", "{\"resourceId\":10}"))
                .thenReturn(failedFuture);
        when(outboxEventRepository.markFailed(
                eq(pendingEvent.getOutboxId()), anyString(), eq(1), any(Instant.class),
                eq("Kafka Broker Down"), eq(OutboxStatus.PROCESSING), eq(OutboxStatus.FAILED)
        )).thenReturn(1);

        outboxPublisherJob.publishOutboxEvents();

        verify(outboxEventRepository).markFailed(
                eq(pendingEvent.getOutboxId()), anyString(), eq(1), any(Instant.class),
                eq("Kafka Broker Down"), eq(OutboxStatus.PROCESSING), eq(OutboxStatus.FAILED)
        );
    }

    @Test
    @DisplayName("Atomik claim kaybedilirse reference event Kafka'ya gönderilmemelidir")
    void shouldNotPublishWhenAtomicClaimFails() {
        when(outboxEventRepository.findClaimableEvents(
                eq(OutboxStatus.PENDING), eq(OutboxStatus.FAILED), eq(OutboxStatus.PROCESSING),
                any(Instant.class), any(Pageable.class)
        )).thenReturn(List.of(pendingEvent));
        when(outboxEventRepository.claimEvent(
                eq(pendingEvent.getOutboxId()), anyString(), any(Instant.class), any(Instant.class),
                eq(OutboxStatus.PENDING), eq(OutboxStatus.FAILED), eq(OutboxStatus.PROCESSING)
        )).thenReturn(0);

        outboxPublisherJob.publishOutboxEvents();

        verifyNoInteractions(kafkaTemplate);
    }

    private void stubClaimableEvent() {
        when(outboxEventRepository.findClaimableEvents(
                eq(OutboxStatus.PENDING), eq(OutboxStatus.FAILED), eq(OutboxStatus.PROCESSING),
                any(Instant.class), any(Pageable.class)
        )).thenReturn(List.of(pendingEvent));
        when(outboxEventRepository.claimEvent(
                eq(pendingEvent.getOutboxId()), anyString(), any(Instant.class), any(Instant.class),
                eq(OutboxStatus.PENDING), eq(OutboxStatus.FAILED), eq(OutboxStatus.PROCESSING)
        )).thenReturn(1);
    }
}
