package com.alikaracor.learning.referencemanager.repository;

import com.alikaracor.learning.referencemanager.model.OutboxEvent;
import com.alikaracor.learning.referencemanager.model.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    List<OutboxEvent> findAllByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);

    List<OutboxEvent> findAllByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(OutboxStatus status, Instant now, Pageable pageable);

    long deleteByStatusAndPublishedAtBefore(OutboxStatus status, Instant cutoffTime);
}
