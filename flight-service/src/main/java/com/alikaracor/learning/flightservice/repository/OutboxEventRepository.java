package com.alikaracor.learning.flightservice.repository;

import com.alikaracor.learning.flightservice.model.OutboxEvent;
import com.alikaracor.learning.flightservice.model.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    List<OutboxEvent> findAllByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);

    List<OutboxEvent> findAllByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(OutboxStatus status, Instant now, Pageable pageable);

    long deleteByStatusAndPublishedAtBefore(OutboxStatus status, Instant cutoffTime);



}
