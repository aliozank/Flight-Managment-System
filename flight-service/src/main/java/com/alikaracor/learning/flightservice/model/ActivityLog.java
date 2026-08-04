package com.alikaracor.learning.flightservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "activity_logs")
@Getter
@Setter
@NoArgsConstructor
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_log_id", nullable = false)
    private Long activityLogId;

    @Column(name = "actor_user_id",nullable = true)
    private Long actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_action" ,nullable = false, length = 50)
    private ActivityAction activityAction;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_resource_type", nullable = false , length = 50)
    private ActivityResourceType activityResourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "success",  nullable = false)
    private boolean success;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;






}
