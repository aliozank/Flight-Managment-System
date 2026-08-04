package com.alikaracor.learning.flightservice.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ReferenceEvent {

    private UUID eventId;
    private ReferenceEventType eventType;
    private ReferenceResourceType resourceType;
    private Long resourceId;
    private Instant occurredAt;
}