package com.alikaracor.learning.flightservice.service;

import com.alikaracor.learning.flightservice.dto.AuthResponse;
import com.alikaracor.learning.flightservice.dto.LoginRequest;
import com.alikaracor.learning.flightservice.mapper.UserMapper;
import com.alikaracor.learning.flightservice.model.User;
import com.alikaracor.learning.flightservice.model.UserStatus;
import com.alikaracor.learning.flightservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private AuthService authService;

    private LoginRequest loginRequest;
    private User sampleUser;
    private String ipAddress;

    @BeforeEach
    void setUp() {
        ipAddress = "192.168.1.50";

        loginRequest = new LoginRequest();
        loginRequest.setUserName("admin");
        loginRequest.setUserPassword("secret123");

        sampleUser = new User();
        sampleUser.setUserId(1L);
        sampleUser.setUserName("admin");
        sampleUser.setUserPasswordHash("$2a$10$hash");
        sampleUser.setUserStatus(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("authenticate - Kimlik bilgileri doğru ve kullanıcı aktif olduğunda AuthResponse dönmelidir")
    void authenticate_shouldReturnAuthResponse_whenCredentialsAreValidAndUserIsActive() {
        when(userRepository.findByUserNameIgnoreCase("admin")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("secret123", "$2a$10$hash")).thenReturn(true);
        when(jwtService.generateToken(sampleUser)).thenReturn("generated-jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse expectedResponse = new AuthResponse();
        expectedResponse.setAccessToken("generated-jwt-token");
        when(userMapper.toAuthResponse(sampleUser, "generated-jwt-token", 3600L)).thenReturn(expectedResponse);

        AuthResponse response = authService.authenticate(loginRequest, ipAddress);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("generated-jwt-token");
        assertThat(sampleUser.getUserLastLoginAt()).isNotNull();

        verify(activityLogService).logLoginSuccess(1L, ipAddress);
    }

    @Test
    @DisplayName("authenticate - Kullanıcı bulunamadığında BadCredentialsException fırlatmalı ve null userId ile loglamalıdır")
    void authenticate_shouldThrowBadCredentialsException_whenUserNotFound() {
        when(userRepository.findByUserNameIgnoreCase("admin")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate(loginRequest, ipAddress))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");

        verify(activityLogService).logLoginFailure(null, "User not found", ipAddress);
    }

    @Test
    @DisplayName("authenticate - Kullanıcı pasif ise DisabledException fırlatmalı ve failure loglamalıdır")
    void authenticate_shouldThrowDisabledException_whenUserIsNotActive() {
        sampleUser.setUserStatus(UserStatus.INACTIVE);
        when(userRepository.findByUserNameIgnoreCase("admin")).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> authService.authenticate(loginRequest, ipAddress))
                .isInstanceOf(DisabledException.class)
                .hasMessage("User is not active");

        verify(activityLogService).logLoginFailure(1L, "User is not active", ipAddress);
    }

    @Test
    @DisplayName("authenticate - Şifre eşleşmediğinde BadCredentialsException fırlatmalı ve failure loglamalıdır")
    void authenticate_shouldThrowBadCredentialsException_whenPasswordIsWrong() {
        when(userRepository.findByUserNameIgnoreCase("admin")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("secret123", "$2a$10$hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate(loginRequest, ipAddress))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");

        verify(activityLogService).logLoginFailure(1L, "invalid user password", ipAddress);
    }
}
