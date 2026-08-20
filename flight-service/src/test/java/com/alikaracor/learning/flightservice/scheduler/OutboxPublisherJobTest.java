package com.alikaracor.learning.flightservice.scheduler;

import com.alikaracor.learning.flightservice.model.OutboxEvent;
import com.alikaracor.learning.flightservice.model.OutboxStatus;
import com.alikaracor.learning.flightservice.repository.OutboxEventRepository;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
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
        pendingEvent.setAggregateType("FLIGHT");
        pendingEvent.setAggregateId("42");
        pendingEvent.setEventType("CREATED");
        pendingEvent.setTopicName("flight.events");
        pendingEvent.setPayload("{\"flightId\":42}");
        pendingEvent.setStatus(OutboxStatus.PENDING);
        pendingEvent.setAttemptCount(0);
        pendingEvent.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("Kafka ACK geldiğinde claim edilen kayıt PUBLISHED yapılmalıdır")
    void shouldMarkClaimedEventAsPublishedWhenKafkaAcknowledges() {
        stubClaimableEvent();
        when(kafkaTemplate.send("flight.events", "42", "{\"flightId\":42}"))
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
        verify(outboxEventRepository, never()).markFailed(
                anyString(), anyString(), anyInt(), any(), anyString(), any(), any()
        );
    }

    @Test
    @DisplayName("Kafka gönderimi başarısızsa claim sahibi kaydı retry edilebilir FAILED yapmalıdır")
    void shouldMarkClaimedEventAsFailedWhenKafkaSendFails() {
        stubClaimableEvent();
        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka Broker Down"));
        when(kafkaTemplate.send("flight.events", "42", "{\"flightId\":42}"))
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
    @DisplayName("Başka worker tarafından claim edilen kayıt Kafka'ya gönderilmemelidir")
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

    @Test
    @DisplayName("Job Kafka future tamamlanmadan dönmemeli ve ikinci poll için kaydı PROCESSING tutmalıdır")
    void shouldWaitForKafkaAcknowledgementBeforeReturning() throws Exception {
        stubClaimableEvent();
        CompletableFuture<SendResult<String, String>> kafkaFuture = new CompletableFuture<>();
        CountDownLatch sendStarted = new CountDownLatch(1);
        when(kafkaTemplate.send("flight.events", "42", "{\"flightId\":42}"))
                .thenAnswer(invocation -> {
                    sendStarted.countDown();
                    return kafkaFuture;
                });
        when(outboxEventRepository.markPublished(
                eq(pendingEvent.getOutboxId()), anyString(), any(Instant.class),
                eq(OutboxStatus.PROCESSING), eq(OutboxStatus.PUBLISHED)
        )).thenReturn(1);

        CompletableFuture<Void> jobFuture = CompletableFuture.runAsync(outboxPublisherJob::publishOutboxEvents);

        assertThat(sendStarted.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(jobFuture.isDone()).isFalse();

        kafkaFuture.complete(null);
        jobFuture.get(1, TimeUnit.SECONDS);

        verify(outboxEventRepository).markPublished(
                eq(pendingEvent.getOutboxId()), anyString(), any(Instant.class),
                eq(OutboxStatus.PROCESSING), eq(OutboxStatus.PUBLISHED)
        );
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
