package com.alikaracor.learning.referencemanager.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ReferenceEvent {

    UUID eventId;

    ReferenceEventType eventType;

    ReferenceResourceType resourceType;

    Long resourceId;

    Instant occurredAt;


}
