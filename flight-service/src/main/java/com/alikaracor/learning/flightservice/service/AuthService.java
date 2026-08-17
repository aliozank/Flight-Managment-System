package com.alikaracor.learning.flightservice.service;

import com.alikaracor.learning.flightservice.dto.AuthResponse;
import com.alikaracor.learning.flightservice.dto.LoginRequest;
import com.alikaracor.learning.flightservice.mapper.UserMapper;
import com.alikaracor.learning.flightservice.model.User;
import com.alikaracor.learning.flightservice.model.UserStatus;
import com.alikaracor.learning.flightservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final ActivityLogService activityLogService;
    private final TokenRevocationService tokenRevocationService;


    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, UserMapper userMapper, ActivityLogService activityLogService, TokenRevocationService tokenRevocationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.activityLogService = activityLogService;
        this.tokenRevocationService = tokenRevocationService;
    }

    @Transactional
    public AuthResponse authenticate(LoginRequest loginRequest, String ipAddress) {

        User loginUser = userRepository.findByUserNameIgnoreCase(loginRequest.getUserName())
                .orElseThrow(() -> {

                    activityLogService.logLoginFailure(
                            null,
                            "User not found",
                            ipAddress
                    );

                    return new BadCredentialsException("Invalid username or password");

                });


        if (loginUser.getUserStatus() != UserStatus.ACTIVE) {

            activityLogService.logLoginFailure(
                    loginUser.getUserId(),
                    "User is not active",
                    ipAddress
            );

            throw new DisabledException("User is not active");

        }


        if (!passwordEncoder.matches(loginRequest.getUserPassword(), loginUser.getUserPasswordHash())) {

            activityLogService.logLoginFailure(
                    loginUser.getUserId(),
                    "invalid user password",
                    ipAddress
            );

            throw new BadCredentialsException("Invalid username or password");

        }

        String accessToken = jwtService.generateToken(loginUser);

        loginUser.setUserLastLoginAt(Instant.now());

        activityLogService.logLoginSuccess(loginUser.getUserId(), ipAddress);

        return userMapper.toAuthResponse(
                loginUser,
                accessToken,
                jwtService.getExpirationSeconds()
        );


    }

    public void logout(String tokenId, Instant expiresAt) {
        tokenRevocationService.revokeToken(tokenId, expiresAt);
    }



}
