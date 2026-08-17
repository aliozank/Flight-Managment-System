package com.alikaracor.learning.flightservice.controller;

import com.alikaracor.learning.flightservice.dto.AuthResponse;
import com.alikaracor.learning.flightservice.dto.LoginRequest;
import com.alikaracor.learning.flightservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid LoginRequest loginRequest, HttpServletRequest httpServletRequest) {
        return authService.authenticate(loginRequest, httpServletRequest.getRemoteAddr());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal Jwt jwt) {

        authService.logout(jwt.getId(), jwt.getExpiresAt());

    }


}
