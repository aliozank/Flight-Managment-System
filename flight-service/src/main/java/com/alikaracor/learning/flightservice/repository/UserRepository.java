package com.alikaracor.learning.flightservice.repository;

import com.alikaracor.learning.flightservice.dto.UserResponse;
import com.alikaracor.learning.flightservice.model.RoleName;
import com.alikaracor.learning.flightservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserNameIgnoreCase(String userName);

    boolean existsByUserNameIgnoreCase(String userName);

    boolean existsByUserEmailIgnoreCase(String userEmail);

    boolean existsByUserRoles_RoleName(RoleName roleName);
}
