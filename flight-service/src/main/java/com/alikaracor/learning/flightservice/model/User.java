package com.alikaracor.learning.flightservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String userName;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String userEmail;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String userPasswordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false, length = 30)
    private UserStatus userStatus;

    @Column(name = "last_login_at")
    private Instant userLastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant userCreatedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant userUpdatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> userRoles = new HashSet<>();
}
