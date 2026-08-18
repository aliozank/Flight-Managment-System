package com.alikaracor.learning.referencemanager.scheduler;

import com.alikaracor.learning.referencemanager.model.OutboxEvent;
import com.alikaracor.learning.referencemanager.model.OutboxStatus;
import com.alikaracor.learning.referencemanager.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherJobTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OutboxPublisherJob outboxPublisherJob;

    private OutboxEvent pendingEvent;

    @BeforeEach
    void setUp() {
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
    @DisplayName("publishOutboxEvents - Kafka gönderimi başarılı olduğunda outbox kaydını PUBLISHED yapmalıdır")
    void publishOutboxEvents_shouldMarkAsPublished_whenKafkaSendSucceeds() {
        when(outboxEventRepository.findAllByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(pendingEvent));
        when(outboxEventRepository.findAllByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(eq(OutboxStatus.FAILED), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send("reference.events", "10", "{\"resourceId\":10}")).thenReturn(future);

        outboxPublisherJob.publishOutboxEvents();

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(saved.getPublishedAt()).isNotNull();
        assertThat(saved.getLastError()).isNull();
    }

    @Test
    @DisplayName("publishOutboxEvents - Kafka gönderimi başarısız olduğunda attemptCount artırmalı ve FAILED yapmalıdır")
    void publishOutboxEvents_shouldHandleFailure_whenKafkaSendFails() {
        when(outboxEventRepository.findAllByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(pendingEvent));
        when(outboxEventRepository.findAllByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(eq(OutboxStatus.FAILED), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka Broker Down"));
        when(kafkaTemplate.send("reference.events", "10", "{\"resourceId\":10}")).thenReturn(future);

        outboxPublisherJob.publishOutboxEvents();

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(saved.getAttemptCount()).isEqualTo(1);
        assertThat(saved.getNextAttemptAt()).isNotNull();
        assertThat(saved.getLastError()).contains("Kafka Broker Down");
    }
}
