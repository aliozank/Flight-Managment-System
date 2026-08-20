package com.alikaracor.learning.flightservice.mapper;

import com.alikaracor.learning.flightservice.dto.AuthResponse;
import com.alikaracor.learning.flightservice.dto.RegisterRequest;
import com.alikaracor.learning.flightservice.dto.UserResponse;
import com.alikaracor.learning.flightservice.model.Role;
import com.alikaracor.learning.flightservice.model.RoleName;
import com.alikaracor.learning.flightservice.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "userPasswordHash", ignore = true)
    @Mapping(target = "userStatus", ignore = true)
    @Mapping(target = "userLastLoginAt", ignore = true)
    @Mapping(target = "userCreatedAt", ignore = true)
    @Mapping(target = "userUpdatedAt", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    User toUser(RegisterRequest registerRequest);


    @Mapping(source = "userRoles", target = "userRoleNames")
    UserResponse toUserResponse(User user);

    default RoleName toRoleName(Role role) {

        if (role == null) {
            return null;
        }

        return role.getRoleName();
    }


    @Mapping(source = "user.userName", target = "userName")
    @Mapping(source = "user.userRoles", target = "userRoleNames")
    @Mapping(source = "accessToken", target = "accessToken")
    @Mapping(source = "expiresIn", target = "expiresIn")
    @Mapping(target = "tokenType", constant = "Bearer")
    AuthResponse toAuthResponse(
            User user,
            String accessToken,
            Long expiresIn
    );
}
