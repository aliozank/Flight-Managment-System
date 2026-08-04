package com.alikaracor.learning.flightservice.controller;

import com.alikaracor.learning.flightservice.dto.AuthResponse;
import com.alikaracor.learning.flightservice.dto.LoginRequest;
import com.alikaracor.learning.flightservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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





}
