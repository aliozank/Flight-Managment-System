package com.alikaracor.learning.flightservice.scheduler;

import com.alikaracor.learning.flightservice.model.OutboxStatus;
import com.alikaracor.learning.flightservice.repository.OutboxEventRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class OutboxCleanupJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxCleanupJob.class);

    private final OutboxEventRepository outboxEventRepository;

    public OutboxCleanupJob(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @SchedulerLock(name = "OutboxCleanupJob_cleanupPublishedEvents", lockAtLeastFor = "10s", lockAtMostFor = "5m")
    @Transactional
    public void cleanupPublishedEvents() {

        Instant cutoffTime = Instant.now().minus(7, ChronoUnit.DAYS);

        long deletedCount = outboxEventRepository.deleteByStatusAndPublishedAtBefore(OutboxStatus.PUBLISHED, cutoffTime);

        if (deletedCount > 0) {

            LOGGER.info("7 günden eski outbox kayıtları temizlendi. Silinen kayıt sayısı: {}", deletedCount);

        }
    }
}
