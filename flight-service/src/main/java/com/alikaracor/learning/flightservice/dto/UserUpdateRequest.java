package com.alikaracor.learning.flightservice.dto;

import com.alikaracor.learning.flightservice.model.RoleName;
import com.alikaracor.learning.flightservice.model.UserStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class UserUpdateRequest {

    @NotNull
    private UserStatus userStatus;

    @NotEmpty
    private Set<@NotNull RoleName> userRoleNames;
}