package com.alikaracor.learning.flightservice.repository;

import com.alikaracor.learning.flightservice.model.OutboxEvent;
import com.alikaracor.learning.flightservice.model.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    @Query("""
            SELECT event FROM OutboxEvent event
            WHERE event.status = :pendingStatus
               OR (event.status = :failedStatus AND event.nextAttemptAt <= :now)
               OR (event.status = :processingStatus
                   AND (event.lockedUntil IS NULL OR event.lockedUntil <= :now))
            ORDER BY event.createdAt ASC
            """)
    List<OutboxEvent> findClaimableEvents(
            @Param("pendingStatus") OutboxStatus pendingStatus,
            @Param("failedStatus") OutboxStatus failedStatus,
            @Param("processingStatus") OutboxStatus processingStatus,
            @Param("now") Instant now,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            UPDATE OutboxEvent event
               SET event.status = :processingStatus,
                   event.lockToken = :lockToken,
                   event.lockedUntil = :lockedUntil
             WHERE event.outboxId = :outboxId
               AND (event.status = :pendingStatus
                    OR (event.status = :failedStatus AND event.nextAttemptAt <= :now)
                    OR (event.status = :processingStatus
                        AND (event.lockedUntil IS NULL OR event.lockedUntil <= :now)))
            """)
    int claimEvent(
            @Param("outboxId") String outboxId,
            @Param("lockToken") String lockToken,
            @Param("lockedUntil") Instant lockedUntil,
            @Param("now") Instant now,
            @Param("pendingStatus") OutboxStatus pendingStatus,
            @Param("failedStatus") OutboxStatus failedStatus,
            @Param("processingStatus") OutboxStatus processingStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            UPDATE OutboxEvent event
               SET event.status = :publishedStatus,
                   event.publishedAt = :publishedAt,
                   event.nextAttemptAt = NULL,
                   event.lastError = NULL,
                   event.lockToken = NULL,
                   event.lockedUntil = NULL
             WHERE event.outboxId = :outboxId
               AND event.status = :processingStatus
               AND event.lockToken = :lockToken
            """)
    int markPublished(
            @Param("outboxId") String outboxId,
            @Param("lockToken") String lockToken,
            @Param("publishedAt") Instant publishedAt,
            @Param("processingStatus") OutboxStatus processingStatus,
            @Param("publishedStatus") OutboxStatus publishedStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            UPDATE OutboxEvent event
               SET event.status = :failedStatus,
                   event.attemptCount = :attemptCount,
                   event.nextAttemptAt = :nextAttemptAt,
                   event.lastError = :lastError,
                   event.lockToken = NULL,
                   event.lockedUntil = NULL
             WHERE event.outboxId = :outboxId
               AND event.status = :processingStatus
               AND event.lockToken = :lockToken
            """)
    int markFailed(
            @Param("outboxId") String outboxId,
            @Param("lockToken") String lockToken,
            @Param("attemptCount") int attemptCount,
            @Param("nextAttemptAt") Instant nextAttemptAt,
            @Param("lastError") String lastError,
            @Param("processingStatus") OutboxStatus processingStatus,
            @Param("failedStatus") OutboxStatus failedStatus
    );

    long deleteByStatusAndPublishedAtBefore(OutboxStatus status, Instant cutoffTime);
}
