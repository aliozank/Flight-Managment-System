package com.alikaracor.learning.flightservice.scheduler;

import com.alikaracor.learning.flightservice.model.OutboxStatus;
import com.alikaracor.learning.flightservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxCleanupJobTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private OutboxCleanupJob outboxCleanupJob;

    @Test
    @DisplayName("cleanupPublishedEvents - 7 günden eski PUBLISHED kayıtları silmelidir")
    void cleanupPublishedEvents_shouldDeletePublishedEventsOlderThan7Days() {
        when(outboxEventRepository.deleteByStatusAndPublishedAtBefore(eq(OutboxStatus.PUBLISHED), any(Instant.class)))
                .thenReturn(5L);

        Instant beforeCall = Instant.now().minus(7, ChronoUnit.DAYS);

        outboxCleanupJob.cleanupPublishedEvents();

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(outboxEventRepository).deleteByStatusAndPublishedAtBefore(eq(OutboxStatus.PUBLISHED), cutoffCaptor.capture());

        Instant capturedCutoff = cutoffCaptor.getValue();
        assertThat(capturedCutoff).isNotNull();
        // Cutoff zamanı yaklaşık 7 gün öncesi olmalıdır (10 saniye tolerans ile)
        assertThat(capturedCutoff).isAfterOrEqualTo(beforeCall.minusSeconds(10));
        assertThat(capturedCutoff).isBeforeOrEqualTo(Instant.now().minus(7, ChronoUnit.DAYS));
    }
}
