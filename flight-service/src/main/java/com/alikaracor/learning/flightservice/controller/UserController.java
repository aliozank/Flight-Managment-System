package com.alikaracor.learning.flightservice.controller;

import com.alikaracor.learning.flightservice.dto.RegisterRequest;
import com.alikaracor.learning.flightservice.dto.UserResponse;
import com.alikaracor.learning.flightservice.dto.UserUpdateRequest;
import com.alikaracor.learning.flightservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@RequestBody @Valid RegisterRequest registerRequest, @AuthenticationPrincipal Jwt jwt, HttpServletRequest httpServletRequest) {

        Long performedByUserId = Long.valueOf(jwt.getSubject());

        return userService.registerUser(
                registerRequest,
                performedByUserId,
                httpServletRequest.getRemoteAddr()
        );

    }

    @GetMapping("/{userId}")
    public UserResponse getUserById(@PathVariable Long userId) {

        return userService.getUserById(userId);

    }

    @GetMapping
    public List<UserResponse> getAllUsers() {

        return userService.getAllUsers();

    }

    @PutMapping("/{userId}")
    public UserResponse updateUser(@PathVariable Long userId, @Valid @RequestBody UserUpdateRequest userUpdateRequest, @AuthenticationPrincipal Jwt jwt, HttpServletRequest httpServletRequest) {

        Long performedByUserId = Long.valueOf(jwt.getSubject());

        return userService.updateUserById(
                userId,
                userUpdateRequest,
                performedByUserId,
                httpServletRequest.getRemoteAddr()
        );
    }

}
