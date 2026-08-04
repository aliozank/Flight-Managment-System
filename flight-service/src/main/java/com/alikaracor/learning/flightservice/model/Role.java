package com.alikaracor.learning.flightservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false, unique = true, length = 40)
    private RoleName roleName;

    @Column(name = "role_description", length = 150)
    private String roleDescription;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant roleCreatedAt;
}
