package com.alikaracor.learning.flightservice.service;

import com.alikaracor.learning.flightservice.dto.RegisterRequest;
import com.alikaracor.learning.flightservice.dto.UserResponse;
import com.alikaracor.learning.flightservice.dto.UserUpdateRequest;
import com.alikaracor.learning.flightservice.mapper.UserMapper;
import com.alikaracor.learning.flightservice.model.Role;
import com.alikaracor.learning.flightservice.model.User;
import com.alikaracor.learning.flightservice.model.UserStatus;
import com.alikaracor.learning.flightservice.repository.RoleRepository;
import com.alikaracor.learning.flightservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;
    private final ActivityLogService activityLogService;


    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserMapper userMapper, RoleRepository roleRepository, ActivityLogService activityLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.roleRepository = roleRepository;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public UserResponse registerUser(RegisterRequest registerRequest, Long performedByUserId, String clientIpAddress)
    {

        if (userRepository.existsByUserNameIgnoreCase(registerRequest.getUserName())) {

            activityLogService.logUserCreateFailure(performedByUserId, "Username is already in use", clientIpAddress);

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username is already in use");
        }

        if (userRepository.existsByUserEmailIgnoreCase(registerRequest.getUserEmail())) {

            activityLogService.logUserCreateFailure(performedByUserId, "Email is already in use", clientIpAddress);

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already in use");
        }

        User newUser = userMapper.toUser(registerRequest);

        String encodedPassword = passwordEncoder.encode(registerRequest.getUserPassword());

        newUser.setUserPasswordHash(encodedPassword);
        newUser.setUserStatus(UserStatus.ACTIVE);   //DEFAULT BÖYLE GELECEK ADMİN İSTERSE DEĞİŞTİRİR

        newUser.setUserRoles(registerRequest.getUserRoleNames()
                .stream()
                .map(roleName -> roleRepository.findByRoleName(roleName)
                        .orElseThrow(() -> {

                            activityLogService.logUserCreateFailure(performedByUserId, "Rol bulunamadı: " + roleName, clientIpAddress);

                            return new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Rol bulunamadı: " + roleName
                            );
                        }))
                .collect(Collectors.toSet()));

        User savedUser = userRepository.save(newUser);

        activityLogService.logUserCreated(performedByUserId, savedUser.getUserId(), clientIpAddress);

        return userMapper.toUserResponse(savedUser);

    }

    public UserResponse getUserById(Long userId) {

        User getUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"));

        return userMapper.toUserResponse(getUser);

    }

    public List<UserResponse> getAllUsers() {

        List<UserResponse> userList = userRepository.findAll()
                .stream()
                .map(userMapper::toUserResponse)
                .toList();

        return userList;
    }

    @Transactional
    public UserResponse updateUserById(Long requestedUserId, UserUpdateRequest userUpdateRequest, Long performedByUserId, String clientIpAddress) {

        User updatedUser = userRepository.findById(requestedUserId)
                .orElseThrow(() -> {

                    activityLogService.logUserUpdateFailure(performedByUserId, requestedUserId, "User not found", clientIpAddress);

                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "User not found"
                    );
                });

        Set<Role> updatedRoles = userUpdateRequest.getUserRoleNames()
                .stream()
                .map(roleName -> roleRepository.findByRoleName(roleName)
                        .orElseThrow(() -> {

                            activityLogService.logUserUpdateFailure(performedByUserId, requestedUserId, "Rol bulunamadı: " + roleName, clientIpAddress);

                            return new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Rol bulunamadı: " + roleName
                            );
                        }))
                .collect(Collectors.toSet());

        updatedUser.setUserRoles(updatedRoles);
        updatedUser.setUserStatus(userUpdateRequest.getUserStatus());

        User savedUser = userRepository.save(updatedUser);  // dirty checking var ama okunabilirlik olsun

        activityLogService.logUserUpdated(performedByUserId, savedUser.getUserId(), clientIpAddress);

        return userMapper.toUserResponse(savedUser);


    }

}
