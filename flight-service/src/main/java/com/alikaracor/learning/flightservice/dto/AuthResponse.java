package com.alikaracor.learning.flightservice.dto;

import com.alikaracor.learning.flightservice.model.RoleName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class AuthResponse {

    private String accessToken;

    private String tokenType;

    private Long expiresIn;

    private Long userId;

    private String userName;

    private Set<RoleName> userRoleNames;
}