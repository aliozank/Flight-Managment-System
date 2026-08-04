package com.alikaracor.learning.flightservice.dto;

import com.alikaracor.learning.flightservice.model.RoleName;
import com.alikaracor.learning.flightservice.model.UserStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class UserResponse {
    private Long userId;
    private String userName;
    private String userEmail;
    private UserStatus userStatus;
    private Set<RoleName> userRoleNames;
    private Instant userCreatedAt;
    private Instant userLastLoginAt;
}
